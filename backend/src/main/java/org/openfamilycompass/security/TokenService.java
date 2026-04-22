package org.openfamilycompass.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import org.openfamilycompass.config.JwtConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues and validates JWT access and refresh tokens.
 * <p>
 * Access tokens carry a {@code "type": "access"} claim and are validated by
 * Spring Security's OAuth2 Resource Server (see
 * {@link JwtConfig#jwtDecoder()}), which rejects any token whose {@code type}
 * claim is {@code "refresh"}.
 * <p>
 * Refresh tokens carry a {@code "type": "refresh"} claim and are validated
 * explicitly via {@link #validateRefreshToken(String)}, using the dedicated
 * {@link JwtConfig#refreshTokenJwtDecoder()} which does not apply the
 * access-token-only restriction.
 */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder refreshTokenDecoder;

    public TokenService(
            JwtEncoder jwtEncoder,
            @Qualifier("refreshTokenJwtDecoder") JwtDecoder refreshTokenDecoder) {
        this.jwtEncoder = jwtEncoder;
        this.refreshTokenDecoder = refreshTokenDecoder;
    }

    /**
     * Generates a short-lived access token (1 hour) for the given authentication.
     */
    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(authentication.getName())
                .claim("scope", scope)
                .claim(JwtConfig.TOKEN_TYPE_CLAIM, JwtConfig.TOKEN_TYPE_ACCESS)
                .claim("roles", authentication.getAuthorities().stream()
                        .map(a -> a.getAuthority().replace("ROLE_", ""))
                        .collect(Collectors.toList()))
                .build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * Generates a long-lived refresh token (30 days) for the given authentication.
     * The token carries {@code "type": "refresh"} so it cannot be used as an
     * access token (see {@link JwtConfig#jwtDecoder()}).
     */
    public String generateRefreshToken(Authentication authentication) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(30, ChronoUnit.DAYS))
                .subject(authentication.getName())
                .claim(JwtConfig.TOKEN_TYPE_CLAIM, JwtConfig.TOKEN_TYPE_REFRESH)
                .build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * Validates that the given token is a syntactically valid, non-expired
     * refresh token (signature + expiration + {@code "type": "refresh"} claim).
     *
     * @return {@code true} if the token is a valid refresh token, {@code false}
     *         otherwise (invalid signature, expired, wrong type, or malformed).
     */
    public boolean validateRefreshToken(String token) {
        try {
            Jwt decoded = refreshTokenDecoder.decode(token);
            return JwtConfig.TOKEN_TYPE_REFRESH.equals(decoded.getClaimAsString(JwtConfig.TOKEN_TYPE_CLAIM));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts the subject (username) from a refresh token. Callers must first
     * verify the token via {@link #validateRefreshToken(String)}; this method
     * does not perform a type check and will happily decode any syntactically
     * valid JWT signed with the configured key.
     */
    public String getUsernameFromRefreshToken(String token) {
        return refreshTokenDecoder.decode(token).getSubject();
    }
}
