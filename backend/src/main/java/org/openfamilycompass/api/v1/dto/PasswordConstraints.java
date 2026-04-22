package org.openfamilycompass.api.v1.dto;

/**
 * Centralised password policy constants used by all DTOs that accept a
 * plaintext password (user creation, admin updates, profile self-service
 * password change, legacy auth change-password endpoint).
 * <p>
 * Having one source of truth prevents the "admin creates a 4-character
 * password that the user can never change themselves" inconsistency, because
 * all entry points enforce the same bounds.
 * <p>
 * <b>Minimum length:</b> 6 characters. Matches the long-standing expectation
 * of the mobile client (i18n messages and client-side validators). Short
 * enough to remain usable for younger users in a family context.
 * <p>
 * <b>Maximum length:</b> 128 characters. Guards against denial-of-service
 * attacks via extremely long passwords; BCrypt truncates at 72 bytes anyway,
 * so longer inputs carry no additional security value.
 * <p>
 * The message constant is kept as a plain string (not a property reference)
 * because Bean Validation annotation attributes must be compile-time
 * constants.
 */
public final class PasswordConstraints {

    public static final int MIN_LENGTH = 6;
    public static final int MAX_LENGTH = 128;

    /** Validation message used for all password {@code @Size} annotations. */
    public static final String SIZE_MESSAGE = "Password must be between 6 and 128 characters";

    private PasswordConstraints() {
        // utility class
    }
}
