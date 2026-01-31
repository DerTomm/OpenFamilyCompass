import React from 'react';
import { StyleSheet, ViewStyle } from 'react-native';
import { Card as PaperCard, useTheme as usePaperTheme } from 'react-native-paper';

interface CardProps {
    children: React.ReactNode;
    title?: string;
    subtitle?: string;
    style?: ViewStyle;
    onPress?: () => void;
    elevation?: 0 | 1 | 2 | 3 | 4 | 5;
}

export const Card: React.FC<CardProps> = ({
    children,
    title,
    subtitle,
    style,
    onPress,
    elevation = 2,
}) => {
    const theme = usePaperTheme();

    return (
        <PaperCard
            style={[
                styles.card,
                { backgroundColor: theme.colors.surface },
                style
            ]}
            elevation={elevation}
            onPress={onPress}
            mode="elevated"
        >
            {(title || subtitle) && (
                <PaperCard.Title
                    title={title}
                    subtitle={subtitle}
                    titleStyle={[styles.title, { color: theme.colors.onSurface }]}
                    subtitleStyle={[styles.subtitle, { color: theme.colors.onSurfaceVariant }]}
                />
            )}
            <PaperCard.Content>
                {children}
            </PaperCard.Content>
        </PaperCard>
    );
};

const styles = StyleSheet.create({
    card: {
        marginVertical: 8,
        marginHorizontal: 4,
    },
    title: {
        fontSize: 18,
        fontWeight: '600',
    },
    subtitle: {
        fontSize: 14,
    },
});
