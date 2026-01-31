import { MaterialCommunityIcons } from '@expo/vector-icons';
import React from 'react';
import { StyleSheet, ViewStyle } from 'react-native';
import { Button as PaperButton, useTheme } from 'react-native-paper';

interface ButtonProps {
    children: string;
    onPress?: () => void;
    mode?: 'text' | 'outlined' | 'contained' | 'elevated' | 'contained-tonal';
    icon?: keyof typeof MaterialCommunityIcons.glyphMap;
    loading?: boolean;
    disabled?: boolean;
    style?: ViewStyle;
    compact?: boolean;
}

export const Button: React.FC<ButtonProps> = ({
    children,
    onPress,
    mode = 'contained',
    icon,
    loading = false,
    disabled = false,
    style,
    compact = false,
}) => {
    const theme = useTheme();

    return (
        <PaperButton
            mode={mode}
            onPress={onPress}
            icon={icon}
            loading={loading}
            disabled={disabled}
            style={[styles.button, style]}
            contentStyle={compact ? styles.compactContent : styles.content}
            labelStyle={styles.label}
        >
            {children}
        </PaperButton>
    );
};

const styles = StyleSheet.create({
    button: {
        marginVertical: 4,
    },
    content: {
        paddingVertical: 8,
    },
    compactContent: {
        paddingVertical: 4,
    },
    label: {
        fontSize: 16,
        fontWeight: '600',
    },
});
