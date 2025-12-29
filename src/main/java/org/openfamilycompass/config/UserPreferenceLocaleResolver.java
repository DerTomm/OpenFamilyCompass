package org.openfamilycompass.config;

import java.util.List;
import java.util.Locale;

import org.openfamilycompass.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.LocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Custom LocaleResolver that:
 * 1. Uses browser's Accept-Language header as primary source
 * 2. Allows authenticated users to override with their preference
 * 3. Falls back to English if no preference is found
 */
public class UserPreferenceLocaleResolver implements LocaleResolver {

    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, Locale.GERMAN);

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        // Check if user is authenticated and has a language preference
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            String userLanguage = user.getLanguage();
            if (userLanguage != null) {
                Locale userLocale = Locale.forLanguageTag(userLanguage);
                if (SUPPORTED_LOCALES.contains(userLocale)) {
                    return userLocale;
                }
            }
        }

        // Fall back to browser's Accept-Language header
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage != null && !acceptLanguage.trim().isEmpty()) {
            Locale browserLocale = request.getLocale();
            // Check if browser locale is supported
            for (Locale supported : SUPPORTED_LOCALES) {
                if (supported.getLanguage().equals(browserLocale.getLanguage())) {
                    return supported;
                }
            }
        }

        // Default fallback
        return DEFAULT_LOCALE;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        // This resolver doesn't support setting locale via this method
        // Language preferences are set via user profile
    }
}