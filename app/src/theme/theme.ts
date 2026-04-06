import { MD3DarkTheme, MD3LightTheme, MD3Theme } from 'react-native-paper';

// Custom color palette for OpenFamilyCompass
const customColors = {
    primary: '#4A90E2',      // Calm blue - trust and reliability
    secondary: '#9C27B0',    // Purple - creativity and fun
    tertiary: '#FF9800',     // Orange - energy and encouragement
    success: '#4CAF50',      // Green - achievements
    warning: '#FF9800',      // Orange - attention needed
    error: '#F44336',        // Red - errors
    info: '#2196F3',         // Blue - information

    // Family-specific colors
    child: '#FF9800',        // Orange for children
    parent: '#2196F3',       // Blue for parents
    admin: '#9C27B0',        // Purple for admins

    // Task & reward colors
    taskPending: '#FF9800',
    taskComplete: '#4CAF50',
    reward: '#9C27B0',
};

// Dark theme (default)
export const darkTheme: MD3Theme = {
    ...MD3DarkTheme,
    colors: {
        ...MD3DarkTheme.colors,
        primary: customColors.primary,
        secondary: customColors.secondary,
        tertiary: customColors.tertiary,
        error: customColors.error,

        // Surface colors for dark mode
        background: '#111111',
        surface: '#1C1C1C',
        surfaceVariant: '#484848',
        outline: '#888888',

        // Text colors
        onBackground: '#FFFFFF',
        onSurface: '#FFFFFF',
        onSurfaceVariant: '#CCCCCC',
    },
    roundness: 12,
};

// Light theme
export const lightTheme: MD3Theme = {
    ...MD3LightTheme,
    colors: {
        ...MD3LightTheme.colors,
        primary: customColors.primary,
        secondary: customColors.secondary,
        tertiary: customColors.tertiary,
        error: customColors.error,

        // Surface colors for light mode
        background: '#F5F5F5',
        surface: '#FFFFFF',
        surfaceVariant: '#F0F0F0',

        // Text colors
        onBackground: '#000000',
        onSurface: '#000000',
        onSurfaceVariant: '#666666',
    },
    roundness: 12,
};

// Typography scale
export const typography = {
    displayLarge: {
        fontSize: 57,
        lineHeight: 64,
        fontWeight: '400' as const,
    },
    displayMedium: {
        fontSize: 45,
        lineHeight: 52,
        fontWeight: '400' as const,
    },
    displaySmall: {
        fontSize: 36,
        lineHeight: 44,
        fontWeight: '400' as const,
    },
    headlineLarge: {
        fontSize: 32,
        lineHeight: 40,
        fontWeight: '600' as const,
    },
    headlineMedium: {
        fontSize: 28,
        lineHeight: 36,
        fontWeight: '600' as const,
    },
    headlineSmall: {
        fontSize: 24,
        lineHeight: 32,
        fontWeight: '600' as const,
    },
    titleLarge: {
        fontSize: 22,
        lineHeight: 28,
        fontWeight: '500' as const,
    },
    titleMedium: {
        fontSize: 16,
        lineHeight: 24,
        fontWeight: '500' as const,
    },
    titleSmall: {
        fontSize: 14,
        lineHeight: 20,
        fontWeight: '500' as const,
    },
    bodyLarge: {
        fontSize: 16,
        lineHeight: 24,
        fontWeight: '400' as const,
    },
    bodyMedium: {
        fontSize: 14,
        lineHeight: 20,
        fontWeight: '400' as const,
    },
    bodySmall: {
        fontSize: 12,
        lineHeight: 16,
        fontWeight: '400' as const,
    },
    labelLarge: {
        fontSize: 14,
        lineHeight: 20,
        fontWeight: '500' as const,
    },
    labelMedium: {
        fontSize: 12,
        lineHeight: 16,
        fontWeight: '500' as const,
    },
    labelSmall: {
        fontSize: 11,
        lineHeight: 16,
        fontWeight: '500' as const,
    },
};

// Spacing scale (following 8pt grid)
export const spacing = {
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
    xxl: 48,
    xxxl: 64,
};

// Shadow presets
export const shadows = {
    small: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 1 },
        shadowOpacity: 0.18,
        shadowRadius: 1.0,
        elevation: 1,
    },
    medium: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.23,
        shadowRadius: 2.62,
        elevation: 4,
    },
    large: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.30,
        shadowRadius: 4.65,
        elevation: 8,
    },
};

// Animation durations
export const animations = {
    fast: 150,
    normal: 250,
    slow: 350,
};
