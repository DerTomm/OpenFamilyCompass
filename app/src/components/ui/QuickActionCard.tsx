import { MaterialCommunityIcons } from '@expo/vector-icons';
import React from 'react';
import { StyleSheet, TouchableOpacity, View, ViewStyle } from 'react-native';
import { Badge as PaperBadge, Text, useTheme } from 'react-native-paper';
import { shadows } from '../../theme/theme';

interface QuickActionCardProps {
    title: string;
    icon: keyof typeof MaterialCommunityIcons.glyphMap;
    count?: number;
    color?: string;
    onPress: () => void;
    style?: ViewStyle;
}

export const QuickActionCard: React.FC<QuickActionCardProps> = ({
    title,
    icon,
    count,
    color,
    onPress,
    style,
}) => {
    const theme = useTheme();
    const accentColor = color || theme.colors.primary;

    return (
        <TouchableOpacity
            style={[
                styles.container,
                {
                    backgroundColor: theme.colors.surface,
                    borderLeftColor: accentColor,
                },
                shadows.medium,
                style,
            ]}
            onPress={onPress}
            activeOpacity={0.7}
        >
            <View style={styles.iconContainer}>
                <View style={[styles.iconCircle, { backgroundColor: accentColor + '20' }]}>
                    <MaterialCommunityIcons name={icon} size={28} color={accentColor} />
                </View>
            </View>

            <View style={styles.content}>
                <Text variant="titleMedium" style={{ color: theme.colors.onSurface }}>
                    {title}
                </Text>
            </View>

            {count !== undefined && count > 0 && (
                <View style={styles.badgeContainer}>
                    <PaperBadge
                        style={[styles.badge, { backgroundColor: accentColor }]}
                        size={24}
                    >
                        {count > 99 ? '99+' : count}
                    </PaperBadge>
                </View>
            )}
        </TouchableOpacity>
    );
};

const styles = StyleSheet.create({
    container: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: 16,
        borderRadius: 12,
        borderLeftWidth: 4,
        marginVertical: 6,
    },
    iconContainer: {
        marginRight: 16,
    },
    iconCircle: {
        width: 56,
        height: 56,
        borderRadius: 28,
        justifyContent: 'center',
        alignItems: 'center',
    },
    content: {
        flex: 1,
    },
    badgeContainer: {
        marginLeft: 8,
    },
    badge: {
        fontSize: 12,
        fontWeight: '700',
    },
});
