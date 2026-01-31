import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Localization from 'expo-localization';
import React, { createContext, ReactNode, useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { SUPPORTED_LANGUAGES, SupportedLanguage } from './config';

const LANGUAGE_STORAGE_KEY = '@app:language';

interface I18nContextType {
    language: SupportedLanguage;
    setLanguage: (lang: SupportedLanguage) => Promise<void>;
    t: (key: string, params?: any) => string;
    availableLanguages: typeof SUPPORTED_LANGUAGES;
}

const I18nContext = createContext<I18nContextType | undefined>(undefined);

interface I18nProviderProps {
    children: ReactNode;
}

export const I18nProvider: React.FC<I18nProviderProps> = ({ children }) => {
    const { t, i18n: i18nInstance } = useTranslation();
    const [language, setLanguageState] = useState<SupportedLanguage>(
        (i18nInstance.language as SupportedLanguage) || 'en'
    );

    // Load saved language preference on mount
    useEffect(() => {
        const loadLanguage = async () => {
            try {
                const savedLanguage = await AsyncStorage.getItem(LANGUAGE_STORAGE_KEY);

                if (savedLanguage && savedLanguage in SUPPORTED_LANGUAGES) {
                    await i18nInstance.changeLanguage(savedLanguage);
                    setLanguageState(savedLanguage as SupportedLanguage);
                } else {
                    // Use device language if no preference is saved
                    const deviceLanguage = Localization.getLocales()[0]?.languageCode || 'en';
                    const supportedDeviceLanguage = deviceLanguage in SUPPORTED_LANGUAGES
                        ? deviceLanguage as SupportedLanguage
                        : 'en';

                    await i18nInstance.changeLanguage(supportedDeviceLanguage);
                    setLanguageState(supportedDeviceLanguage);
                }
            } catch (error) {
                console.error('Error loading language preference:', error);
            }
        };

        loadLanguage();
    }, [i18nInstance]);

    const setLanguage = async (lang: SupportedLanguage) => {
        try {
            await AsyncStorage.setItem(LANGUAGE_STORAGE_KEY, lang);
            await i18nInstance.changeLanguage(lang);
            setLanguageState(lang);
        } catch (error) {
            console.error('Error saving language preference:', error);
        }
    };

    const value: I18nContextType = {
        language,
        setLanguage,
        t,
        availableLanguages: SUPPORTED_LANGUAGES,
    };

    return (
        <I18nContext.Provider value={value}>
            {children}
        </I18nContext.Provider>
    );
};

export const useI18n = (): I18nContextType => {
    const context = useContext(I18nContext);
    if (!context) {
        throw new Error('useI18n must be used within an I18nProvider');
    }
    return context;
};
