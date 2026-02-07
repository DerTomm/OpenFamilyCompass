import { MaterialCommunityIcons } from '@expo/vector-icons';
import React, { useEffect, useState } from 'react';
import {
  Alert,
  Image,
  Modal,
  TextInput as RNTextInput,
  StyleSheet,
  View
} from 'react-native';
import { Surface, Text, useTheme } from 'react-native-paper';
import { SafeAreaView } from 'react-native-safe-area-context';
import { API_CONFIG, secureStorage, STORAGE_KEYS } from '../../api/config';
import { Button } from '../../components/ui';
import { useAuth } from '../../hooks/useAuth';
import { useI18n } from '../../i18n/I18nContext';

export const LoginScreen: React.FC = () => {
  const { login } = useAuth();
  const theme = useTheme();
  const { t } = useI18n();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showServerDialog, setShowServerDialog] = useState(false);
  const [serverUrl, setServerUrl] = useState(API_CONFIG.baseUrl);

  // Login Form State
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  useEffect(() => {
    // Lade die gespeicherte Server-URL beim Start
    loadServerUrl();
  }, []);

  const loadServerUrl = async () => {
    try {
      const savedUrl = await secureStorage.getItem(STORAGE_KEYS.SERVER_URL);
      if (savedUrl) {
        setServerUrl(savedUrl);
        API_CONFIG.baseUrl = savedUrl;
      }
    } catch (err) {
      console.error('Error loading server URL:', err);
    }
  };

  const saveServerUrl = async () => {
    try {
      // Validiere die URL
      if (!serverUrl || !serverUrl.startsWith('http')) {
        Alert.alert(t('common.error'), t('login.server.url.invalid'));
        return;
      }

      // Entferne trailing slash
      const cleanUrl = serverUrl.replace(/\/$/, '');

      await secureStorage.setItem(STORAGE_KEYS.SERVER_URL, cleanUrl);
      API_CONFIG.baseUrl = cleanUrl;

      setShowServerDialog(false);
      Alert.alert(t('common.success'), t('login.server.save.success'));
    } catch (err) {
      console.error('Error saving server URL:', err);
      Alert.alert(t('common.error'), t('login.server.save.error'));
    }
  };

  const handleLogin = async () => {
    if (!username || !password) {
      setError(t('login.enter_credentials'));
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      await login(username, password);
    } catch (err) {
      setError(t('login.error.credentials'));
      console.error('Login error:', err);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <SafeAreaView style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <View style={styles.content}>
        {/* Logo Section */}
        <View style={styles.logoContainer}>
          <Surface style={[styles.logoCircle, { backgroundColor: theme.colors.primary + '20' }]} elevation={2}>
            <Image 
              source={require('../../../assets/icon.png')} 
              style={{ width: 100, height: 100 }} 
              resizeMode="contain" 
            />
          </Surface>

          <Text variant="displaySmall" style={[styles.title, { color: theme.colors.onBackground }]}>
            {t('app.name')}
          </Text>

        </View>

        {/* Error Message */}
        {error && (
          <Surface
            style={[styles.errorContainer, { backgroundColor: theme.colors.errorContainer }]}
            elevation={0}
          >
            <MaterialCommunityIcons
              name="alert-circle"
              size={20}
              color={theme.colors.error}
              style={styles.errorIcon}
            />
            <Text style={[styles.errorText, { color: theme.colors.error }]}>
              {error}
            </Text>
          </Surface>
        )}

        {/* Username Input */}
        <View style={styles.inputContainer}>
          <Text variant="labelMedium" style={{ marginBottom: 4, color: theme.colors.onSurfaceVariant }}>
            {t('login.username')}
          </Text>
          <RNTextInput
            style={[
              styles.input,
              {
                backgroundColor: theme.colors.surfaceVariant,
                color: theme.colors.onSurface,
                borderColor: theme.colors.outline,
              }
            ]}
            value={username}
            onChangeText={setUsername}
            autoCapitalize="none"
            autoCorrect={false}
          />
        </View>

        {/* Password Input */}
        <View style={styles.inputContainer}>
          <Text variant="labelMedium" style={{ marginBottom: 4, color: theme.colors.onSurfaceVariant }}>
            {t('login.password')}
          </Text>
          <RNTextInput
            style={[
              styles.input,
              {
                backgroundColor: theme.colors.surfaceVariant,
                color: theme.colors.onSurface,
                borderColor: theme.colors.outline,
              }
            ]}
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />
        </View>

        {/* Login Button */}
        <Button
          mode="contained"
          onPress={handleLogin}
          loading={isLoading}
          disabled={isLoading || !username || !password}
          icon="login"
          style={styles.loginButton}
        >
          {isLoading ? t('login.loading') : t('login.button')}
        </Button>

        {/* Server Config Button */}
        <Button
          mode="text"
          onPress={() => setShowServerDialog(true)}
          icon="cog"
          style={styles.configButton}
        >
          {t('login.server.settings')}
        </Button>
      </View>

      {/* Footer */}
      <View style={styles.footer}>
        <MaterialCommunityIcons
          name="open-source-initiative"
          size={16}
          color={theme.colors.onSurfaceVariant}
          style={styles.footerIcon}
        />
        <Text variant="bodySmall" style={{ color: theme.colors.onSurfaceVariant }}>
          {t('login.footer')}
        </Text>
      </View>

      {/* Server Settings Modal */}
      <Modal
        visible={showServerDialog}
        transparent
        animationType="fade"
        onRequestClose={() => setShowServerDialog(false)}
      >
        <View style={styles.modalOverlay}>
          <Surface style={styles.modalContent} elevation={4}>
            <View style={styles.modalHeader}>
              <MaterialCommunityIcons
                name="server"
                size={32}
                color={theme.colors.primary}
              />
              <Text variant="headlineSmall" style={[styles.modalTitle, { color: theme.colors.onSurface }]}>
                {t('login.server.settings')}
              </Text>
            </View>

            <Text variant="bodyMedium" style={[styles.modalDescription, { color: theme.colors.onSurfaceVariant }]}>
              {t('login.server.url.description')}
            </Text>

            <View style={styles.inputContainer}>
              <Text variant="labelMedium" style={[styles.inputLabel, { color: theme.colors.onSurfaceVariant }]}>
                {t('login.server.url.label')}
              </Text>
              <RNTextInput
                style={[
                  styles.input,
                  {
                    backgroundColor: theme.colors.surfaceVariant,
                    color: theme.colors.onSurface,
                    borderColor: theme.colors.outline,
                  }
                ]}
                value={serverUrl}
                onChangeText={setServerUrl}
                placeholder={t('login.server.url.placeholder')}
                placeholderTextColor={theme.colors.onSurfaceVariant}
                autoCapitalize="none"
                autoCorrect={false}
                keyboardType="url"
              />
              <Text variant="bodySmall" style={[styles.inputHint, { color: theme.colors.onSurfaceVariant }]}>
                {t('login.server.url.hint')}
              </Text>
            </View>

            <View style={styles.modalActions}>
              <Button
                mode="outlined"
                onPress={() => setShowServerDialog(false)}
                style={styles.modalButton}
              >
                {t('button.cancel')}
              </Button>
              <Button
                mode="contained"
                onPress={saveServerUrl}
                style={styles.modalButton}
              >
                {t('button.save')}
              </Button>
            </View>
          </Surface>
        </View>
      </Modal>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 32,
  },
  logoContainer: {
    alignItems: 'center',
    marginBottom: 48,
  },
  logoCircle: {
    width: 140,
    height: 140,
    borderRadius: 70,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 24,
  },
  title: {
    fontWeight: '700',
    marginBottom: 4,
  },
  titleAccent: {
    fontWeight: '700',
    marginBottom: 16,
  },
  subtitle: {
    textAlign: 'center',
    lineHeight: 24,
    maxWidth: 300,
  },
  errorContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderRadius: 12,
    marginBottom: 24,
    width: '100%',
  },
  errorIcon: {
    marginRight: 8,
  },
  errorText: {
    flex: 1,
    fontSize: 14,
  },
  loginButton: {
    width: 200,
    alignSelf: 'center',
    marginBottom: 8,
  },
  configButton: {
    marginTop: 8,
  },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 16,
    paddingBottom: 24,
  },
  footerIcon: {
    marginRight: 8,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  modalContent: {
    width: '100%',
    maxWidth: 500,
    borderRadius: 16,
    padding: 24,
  },
  modalHeader: {
    alignItems: 'center',
    marginBottom: 16,
  },
  modalTitle: {
    marginTop: 12,
    textAlign: 'center',
  },
  modalDescription: {
    textAlign: 'center',
    marginBottom: 24,
    lineHeight: 22,
  },
  inputContainer: {
    marginBottom: 24,
  },
  inputLabel: {
    marginBottom: 8,
  },
  input: {
    borderWidth: 1,
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
  },
  inputHint: {
    marginTop: 4,
  },
  modalActions: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 12,
  },
  modalButton: {
    flex: 1,
  },
});

