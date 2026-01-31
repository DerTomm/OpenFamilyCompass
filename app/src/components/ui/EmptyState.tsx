import { MaterialCommunityIcons } from '@expo/vector-icons';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { Text, useTheme } from 'react-native-paper';

interface EmptyStateProps {
    icon: keyof typeof MaterialCommunityIcons.glyphMap;
    title: string;
    message?: string;
    action?: React.ReactNode;
    style?: ViewStyle;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
    icon,
    title,
    message,
    action,
    style,
}) => {
    const theme = useTheme();

    return (
        <View style={[styles.container, style]}>
            <MaterialCommunityIcons
                name={icon}
                size={64}
                color={theme.colors.onSurfaceVariant}
                style={styles.icon}
            />
            <Text variant="titleLarge" style={[styles.title, { color: theme.colors.onSurface }]}>
                {title}
            </Text>
            {message && (
                <Text
                    variant="bodyMedium"
                    style={[styles.message, { color: theme.colors.onSurfaceVariant }]}
                >
                    {message}
                </Text>
            )}
            {action && <View style={styles.action}>{action}</View>}
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        paddingHorizontal: 32,
        paddingVertical: 48,
    },
    icon: {
        marginBottom: 16,
        opacity: 0.6,
    },
    title: {
        textAlign: 'center',
        marginBottom: 8,
    },
    message: {
        textAlign: 'center',
        marginBottom: 24,
    },
    action: {
        marginTop: 8,
    },
});
