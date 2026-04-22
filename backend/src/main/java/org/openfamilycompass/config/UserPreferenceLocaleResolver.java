package org.openfamilycompass.config;

import java.util.List;
import java.util.Locale;

import org.springframework.web.servlet.LocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * LocaleResolver used for backend-generated messages (e.g. validation errors).
 *
 * Resolution order:
 * 1. Browser's Accept-Language header (if among the supported locales)
 * 2. English as default fallback
 *
 * A previous implementation attempted to read the authenticated user's
 * language preference from the Spring Security principal. That path is no
 * longer reachable since the app uses a JWT resource server (the principal is
 * a {@link org.springframework.security.oauth2.jwt.Jwt}, not a domain User).
 * The mobile client sends its own locale via Accept-Language, so the
 * browser-locale path is sufficient for backend messages. User-level language
 * preferences are applied client-side.
 */
public class UserPreferenceLocaleResolver implements LocaleResolver {

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, Locale.GERMAN);

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null && !acceptLanguage.trim().isEmpty()) {
            Locale browserLocale = request.getLocale();
            for (Locale supported : SUPPORTED_LOCALES) {
                if (supported.getLanguage().equals(browserLocale.getLanguage())) {
                    return supported;
                }
            }
        }

        return DEFAULT_LOCALE;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        // This resolver doesn't support setting locale via this method
        // Language preferences are set via user profile
    }
}