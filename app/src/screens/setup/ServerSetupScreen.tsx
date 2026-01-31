import React, { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import {
    Button,
    HelperText,
    Surface,
    Text,
    TextInput,
    useTheme
} from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { secureStorage, STORAGE_KEYS } from '../../api/config';
import { useI18n } from '../../i18n/I18nContext';
import { spacing } from '../../theme/theme';

interface ServerSetupScreenProps {
    onComplete: () => void;
}

export const ServerSetupScreen: React.FC<ServerSetupScreenProps> = ({ onComplete }) => {
    const theme = useTheme();
    const { t } = useI18n();
    const [serverUrl, setServerUrl] = useState('');
    const [isValidating, setIsValidating] = useState(false);
    const [error, setError] = useState('');

    const validateAndSave = async () => {
        setError('');

        // Basic URL validation
        let url = serverUrl.trim();

        if (!url) {
            setError(t('setup.server.url.required'));
            return;
        }

        // Add http:// if no protocol specified
        if (!url.startsWith('http://') && !url.startsWith('https://')) {
            url = `https://${url}`;
        }

        // Remove trailing slash
        url = url.replace(/\/$/, '');

        // Validate URL format
        try {
            new URL(url);
        } catch {
            setError(t('setup.server.url.invalid'));
            return;
        }

        setIsValidating(true);

        try {
            // Test connection to backend
            const response = await fetch(`${url}/actuator/health`, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                },
            });

            if (!response.ok) {
                setError(t('setup.server.connection.failed'));
                setIsValidating(false);
                return;
            }

            // Save URL
            await secureStorage.setItem(STORAGE_KEYS.SERVER_URL, url);
            onComplete();
        } catch (err) {
            console.error('Server validation error:', err);
            setError(t('setup.server.connection.error'));
            setIsValidating(false);
        }
    };

    const useDevelopmentServer = async () => {
        const devUrl = 'http://localhost:8080';
        await secureStorage.setItem(STORAGE_KEYS.SERVER_URL, devUrl);
        onComplete();
    };

    return (
        <SafeAreaView style={styles.container} edges={['top', 'bottom']}>
            <ScrollView contentContainerStyle={styles.scrollContent}>
                <View style={styles.header}>
                    <Text variant="displaySmall" style={styles.title}>
                        {t('setup.server.title')}
                    </Text>
                    <Text variant="bodyLarge" style={[styles.subtitle, { color: theme.colors.onSurfaceVariant }]}>
                        {t('setup.server.subtitle')}
                    </Text>
                </View>

                <Surface style={styles.card} elevation={2}>
                    <TextInput
                        label={t('setup.server.url.label')}
                        placeholder="https://example.com"
                        value={serverUrl}
                        onChangeText={setServerUrl}
                        mode="outlined"
                        autoCapitalize="none"
                        autoCorrect={false}
                        keyboardType="url"
                        disabled={isValidating}
                        error={!!error}
                        style={styles.input}
                    />
                    <HelperText type="error" visible={!!error}>
                        {error}
                    </HelperText>
                    <HelperText type="info" visible={!error}>
                        {t('setup.server.url.hint')}
                    </HelperText>

                    <Button
                        mode="contained"
                        onPress={validateAndSave}
                        loading={isValidating}
                        disabled={isValidating || !serverUrl}
                        style={styles.button}
                    >
                        {t('setup.server.connect')}
                    </Button>

                    <Button
                        mode="outlined"
                        onPress={useDevelopmentServer}
                        disabled={isValidating}
                        style={styles.devButton}
                    >
                        {t('setup.server.use.dev')}
                    </Button>
                </Surface>

                <View style={styles.infoBox}>
                    <Text variant="bodyMedium" style={{ color: theme.colors.onSurfaceVariant }}>
                        💡 {t('setup.server.info')}
                    </Text>
                </View>
            </ScrollView>
        </SafeAreaView>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#f5f5f5',
    },
    scrollContent: {
        flexGrow: 1,
        padding: spacing.lg,
        justifyContent: 'center',
    },
    header: {
        marginBottom: spacing.xl,
        alignItems: 'center',
    },
    title: {
        fontWeight: 'bold',
        textAlign: 'center',
        marginBottom: spacing.sm,
    },
    subtitle: {
        textAlign: 'center',
    },
    card: {
        padding: spacing.lg,
        borderRadius: 12,
        marginBottom: spacing.lg,
    },
    input: {
        marginBottom: spacing.xs,
    },
    button: {
        marginTop: spacing.md,
    },
    devButton: {
        marginTop: spacing.sm,
    },
    infoBox: {
        padding: spacing.md,
        backgroundColor: 'rgba(33, 150, 243, 0.1)',
        borderRadius: 8,
        borderLeftWidth: 4,
        borderLeftColor: '#2196F3',
    },
});
