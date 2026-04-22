package org.openfamilycompass.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class JwtConfig {

    /** Claim name used to distinguish access tokens from refresh tokens. */
    public static final String TOKEN_TYPE_CLAIM = "type";
    /** Claim value for access tokens. */
    public static final String TOKEN_TYPE_ACCESS = "access";
    /** Claim value for refresh tokens. */
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final KeyPair keyPair;

    public JwtConfig(
            @Value("${app.security.jwt.keystore-path:jwt-keys.pfx}") String keystorePath,
            @Value("${app.security.jwt.keystore-password:changeit}") String keystorePassword,
            @Value("${app.security.jwt.key-alias:jwt-key}") String keyAlias) {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        this.keyPair = loadOrGenerateKeyPair(keystorePath, keystorePassword, keyAlias);
    }

    /**
     * Primary {@link JwtDecoder} used by Spring Security's OAuth2 Resource Server
     * for access-token validation. In addition to the standard validators
     * (signature + expiration), it rejects tokens that were issued as refresh
     * tokens (i.e. carry {@code "type": "refresh"}) so refresh tokens cannot be
     * used to authenticate API requests.
     */
    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) keyPair.getPublic())
                .build();

        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefault();
        OAuth2TokenValidator<Jwt> notRefreshToken = jwt -> {
            String type = jwt.getClaimAsString(TOKEN_TYPE_CLAIM);
            if (TOKEN_TYPE_REFRESH.equals(type)) {
                OAuth2Error err = new OAuth2Error(
                        "invalid_token",
                        "Refresh tokens must not be used for API access",
                        null);
                return OAuth2TokenValidatorResult.failure(err);
            }
            return OAuth2TokenValidatorResult.success();
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaults, notRefreshToken));
        return decoder;
    }

    /**
     * Plain {@link JwtDecoder} used by
     * {@link org.openfamilycompass.security.TokenService} to validate refresh
     * tokens. It applies the default validators (signature + expiration) but
     * does not reject refresh tokens - the caller is expected to verify the
     * {@link #TOKEN_TYPE_CLAIM} claim explicitly.
     */
    @Bean(name = "refreshTokenJwtDecoder")
    public JwtDecoder refreshTokenJwtDecoder() {
        return NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) keyPair.getPublic())
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        JWK jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    private KeyPair loadOrGenerateKeyPair(String keystorePath, String password, String alias) {
        File file = new File(keystorePath);
        if (file.exists()) {
            try {
                log.info("Loading JWT keys from {}", file.getAbsolutePath());
                KeyStore ks = KeyStore.getInstance("PKCS12");
                try (FileInputStream fis = new FileInputStream(file)) {
                    ks.load(fis, password.toCharArray());
                }

                PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password.toCharArray());
                Certificate cert = ks.getCertificate(alias);
                RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();

                if (privateKey != null && publicKey != null) {
                    return new KeyPair(publicKey, privateKey);
                }
            } catch (Exception e) {
                log.error("Failed to load JWT keys from {}: {}", keystorePath, e.getMessage());
            }
        }

        log.info("Generating new persistent JWT keys (valid 1 year) to {}", file.getAbsolutePath());
        return generateAndSaveKeyPair(file, password, alias);
    }

    private KeyPair generateAndSaveKeyPair(File file, String password, String alias) {
        try {
            // Generate RSA KeyPair
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair kp = keyPairGenerator.generateKeyPair();

            // Create Self-Signed Certificate
            X500Name owner = new X500Name("CN=OpenFamilyCompass JWT Signer");
            Instant now = Instant.now();
            Date notBefore = Date.from(now);
            Date notAfter = Date.from(now.plus(365, ChronoUnit.DAYS)); // 1 Year Validity
            BigInteger serialNumber = new BigInteger(64, new SecureRandom());

            JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSA");
            ContentSigner contentSigner = signerBuilder.build(kp.getPrivate());

            JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    owner, serialNumber, notBefore, notAfter, owner, kp.getPublic());
            
            X509CertificateHolder certHolder = certBuilder.build(contentSigner);
            X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);

            // Save to PKCS12 Keystore
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, password.toCharArray()); // Initialize empty keystore
            ks.setKeyEntry(alias, kp.getPrivate(), password.toCharArray(), new Certificate[] { cert });

            try (FileOutputStream fos = new FileOutputStream(file)) {
                ks.store(fos, password.toCharArray());
            }

            return kp;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate and save JWT keys", e);
        }
    }
}
