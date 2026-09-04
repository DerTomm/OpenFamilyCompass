import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet } from 'react-native';
import {
  Button,
  HelperText,
  Surface,
  Text,
  TextInput,
  useTheme,
} from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { API_CONFIG, secureStorage, STORAGE_KEYS } from '../../api/config';
import { useDialogs } from '../../hooks/useDialogs';
import { useI18n } from '../../i18n/I18nContext';
import { useAuthStore } from '../../store/authStore';
import { spacing } from '../../theme/theme';

export const ServerSettingsScreen: React.FC = () => {
  const theme = useTheme();
  const { t } = useI18n();
  const { showError, showSuccess, Dialogs } = useDialogs();
  const changeServer = useAuthStore((state) => state.changeServer);
  const [serverUrl, setServerUrl] = useState('');
  const [savedServerUrl, setSavedServerUrl] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [validationError, setValidationError] = useState('');

  useEffect(() => {
    secureStorage.getItem(STORAGE_KEYS.SERVER_URL).then((url) => {
      const currentUrl = url || API_CONFIG.baseUrl;
      setServerUrl(currentUrl);
      setSavedServerUrl(currentUrl);
    });
  }, []);

  const validateAndSave = async () => {
    setValidationError('');
    let normalizedUrl = serverUrl.trim();

    if (!normalizedUrl) {
      setValidationError(t('setup.server.url.required'));
      return;
    }

    if (!normalizedUrl.startsWith('http://') && !normalizedUrl.startsWith('https://')) {
      normalizedUrl = `https://${normalizedUrl}`;
    }
    normalizedUrl = normalizedUrl.replace(/\/+$/, '');

    try {
      new URL(normalizedUrl);
    } catch {
      setValidationError(t('setup.server.url.invalid'));
      return;
    }

    setIsSaving(true);
    try {
      const response = await fetch(`${normalizedUrl}/actuator/health`, {
        headers: { Accept: 'application/json' },
      });
      if (!response.ok) {
        setValidationError(t('setup.server.connection.failed'));
        return;
      }

      if (normalizedUrl === savedServerUrl) {
        setServerUrl(normalizedUrl);
        showSuccess(t('login.server.save.success'), t('common.success'));
        return;
      }

      await changeServer(normalizedUrl);
    } catch (error) {
      console.error('Server settings error:', error);
      showError(t('setup.server.connection.error'), t('common.error'));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <SafeAreaView
      style={[styles.container, { backgroundColor: theme.colors.background }]}
      edges={['bottom']}
    >
      <ScrollView contentContainerStyle={styles.content}>
        <Surface style={styles.card} elevation={1}>
          <Text variant="titleLarge" style={[styles.title, { color: theme.colors.onSurface }]}>
            {t('login.server.settings')}
          </Text>
          <Text
            variant="bodyMedium"
            style={[styles.description, { color: theme.colors.onSurfaceVariant }]}
          >
            {t('settings.server.description')}
          </Text>

          <TextInput
            label={t('setup.server.url.label')}
            value={serverUrl}
            onChangeText={setServerUrl}
            placeholder={t('login.server.url.placeholder')}
            mode="outlined"
            autoCapitalize="none"
            autoCorrect={false}
            keyboardType="url"
            disabled={isSaving}
            error={!!validationError}
          />
          <HelperText type="error" visible={!!validationError}>
            {validationError}
          </HelperText>
          <HelperText type="info" visible={!validationError}>
            {t('settings.server.relogin')}
          </HelperText>

          <Button
            mode="contained"
            icon="content-save"
            onPress={validateAndSave}
            loading={isSaving}
            disabled={isSaving || !serverUrl.trim()}
            style={styles.button}
          >
            {t('button.save')}
          </Button>
        </Surface>
      </ScrollView>
      <Dialogs />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    padding: spacing.md,
  },
  card: {
    padding: spacing.lg,
    borderRadius: 12,
  },
  title: {
    fontWeight: '600',
    marginBottom: spacing.sm,
  },
  description: {
    marginBottom: spacing.md,
  },
  button: {
    marginTop: spacing.md,
  },
});
