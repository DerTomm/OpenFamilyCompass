package org.openfamilycompass.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates security-sensitive configuration on startup and refuses to boot
 * the application when production-like profiles are running with insecure
 * default credentials.
 * <p>
 * Checks are executed on {@link ApplicationReadyEvent} so the full
 * {@link Environment} is available (including externalized properties from
 * env vars, Docker compose, etc.).
 * <p>
 * Fail-fast is triggered when either:
 * <ul>
 *   <li>an active Spring profile named {@code prod} or {@code production}
 *       is present, or</li>
 *   <li>the property {@code app.security.fail-on-default-credentials}
 *       is explicitly set to {@code true}.</li>
 * </ul>
 * In all other cases insecure defaults produce a loud {@code WARN} log entry
 * so the problem is visible in development without blocking local work.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class SecurityStartupValidator {

    /** Values treated as weak/default for the initial admin password. */
    private static final Set<String> INSECURE_ADMIN_PASSWORDS = Set.of(
            "", "admin", "changeme", "changeit", "password", "12345", "123456");

    /** Values treated as weak/default for the JWT keystore password. */
    private static final Set<String> INSECURE_KEYSTORE_PASSWORDS = Set.of(
            "", "changeit", "changeme", "password", "12345", "123456");

    private final Environment environment;

    @Value("${app.admin.default-password:}")
    private String adminDefaultPassword;

    @Value("${app.security.jwt.keystore-password:}")
    private String jwtKeystorePassword;

    @Value("${app.security.fail-on-default-credentials:false}")
    private boolean failOnDefaultCredentials;

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        List<String> findings = new ArrayList<>();

        if (INSECURE_ADMIN_PASSWORDS.contains(adminDefaultPassword)) {
            findings.add("app.admin.default-password is set to a weak/default value. "
                    + "Override APP_ADMIN_DEFAULT_PASSWORD with a strong secret.");
        }
        if (INSECURE_KEYSTORE_PASSWORDS.contains(jwtKeystorePassword)) {
            findings.add("app.security.jwt.keystore-password is set to a weak/default value. "
                    + "Override APP_SECURITY_JWT_KEYSTORE_PASSWORD with a strong secret "
                    + "and remove the existing jwt-keys.pfx so a new keystore is generated with the new password.");
        }

        if (findings.isEmpty()) {
            log.debug("Security startup validation passed.");
            return;
        }

        String summary = "Insecure default credentials detected:\n  - "
                + String.join("\n  - ", findings);

        if (shouldFailFast()) {
            log.error(summary);
            throw new IllegalStateException(
                    "Refusing to start with insecure defaults. " + summary
                            + "\nSet safe values via environment variables, or disable this check by setting "
                            + "app.security.fail-on-default-credentials=false (NOT recommended for production).");
        }

        log.warn("{}\n"
                + "The application continues to start because no production-like profile is active. "
                + "Enable the 'prod' profile or set app.security.fail-on-default-credentials=true "
                + "to enforce this check in production.", summary);
    }

    private boolean shouldFailFast() {
        if (failOnDefaultCredentials) {
            return true;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
    }
}
