import * as Localization from 'expo-localization';
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { de } from './locales/de';
import { en } from './locales/en';

// Detect device language
const deviceLanguage = Localization.getLocales()[0]?.languageCode || 'en';

// Supported languages
export const SUPPORTED_LANGUAGES = {
    en: { name: 'English', nativeName: 'English' },
    de: { name: 'German', nativeName: 'Deutsch' },
} as const;

export type SupportedLanguage = keyof typeof SUPPORTED_LANGUAGES;

// Initialize i18next
i18n
    .use(initReactI18next)
    .init({
        resources: {
            en: { translation: en },
            de: { translation: de },
        },
        lng: deviceLanguage, // Will be overridden by user preference if set
        fallbackLng: 'en',
        interpolation: {
            escapeValue: false, // React already escapes
            format: (value, format, lng) => {
                if (format === 'uppercase') return value.toUpperCase();
                return value;
            },
        },
        react: {
            useSuspense: false, // Important for React Native
        },
    });

export default i18n;
