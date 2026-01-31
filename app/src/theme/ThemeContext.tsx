import React, { createContext, ReactNode, useContext, useState } from 'react';
import { useColorScheme } from 'react-native';
import { MD3Theme } from 'react-native-paper';
import { darkTheme, lightTheme } from './theme';

type ThemeMode = 'light' | 'dark' | 'auto';

interface ThemeContextType {
    theme: MD3Theme;
    themeMode: ThemeMode;
    isDark: boolean;
    setThemeMode: (mode: ThemeMode) => void;
    toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

interface ThemeProviderProps {
    children: ReactNode;
}

export const ThemeProvider: React.FC<ThemeProviderProps> = ({ children }) => {
    const systemColorScheme = useColorScheme();
    const [themeMode, setThemeMode] = useState<ThemeMode>('dark'); // Default to dark mode

    // Determine the actual theme based on mode
    const isDark = themeMode === 'auto'
        ? systemColorScheme === 'dark'
        : themeMode === 'dark';

    const theme = isDark ? darkTheme : lightTheme;

    const toggleTheme = () => {
        setThemeMode(prev => {
            if (prev === 'dark') return 'light';
            if (prev === 'light') return 'auto';
            return 'dark';
        });
    };

    const value: ThemeContextType = {
        theme,
        themeMode,
        isDark,
        setThemeMode,
        toggleTheme,
    };

    return (
        <ThemeContext.Provider value={value}>
            {children}
        </ThemeContext.Provider>
    );
};

export const useTheme = (): ThemeContextType => {
    const context = useContext(ThemeContext);
    if (!context) {
        throw new Error('useTheme must be used within a ThemeProvider');
    }
    return context;
};
