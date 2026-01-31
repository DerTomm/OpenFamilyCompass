import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StatusBar } from 'expo-status-bar';
import React, { useEffect } from 'react';
import { Platform } from 'react-native';
import { PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { API_CONFIG, getApiBaseUrl } from './src/api/config';
import { I18nProvider } from './src/i18n/I18nContext';
import './src/i18n/config'; // Initialize i18n
import { AppNavigator } from './src/navigation/AppNavigator';
import { useAuthStore } from './src/store/authStore';
import { ThemeProvider, useTheme } from './src/theme/ThemeContext';

// Create React Query client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      retry: 2,
    },
  },
});

// Handle OAuth callback on web
function useOAuthCallback() {
  const { setTokens } = useAuthStore();

  useEffect(() => {
    if (Platform.OS !== 'web') return;

    const handleCallback = async () => {
      const params = new URLSearchParams(window.location.search);
      const code = params.get('code');

      if (!code) return;

      const codeVerifier = sessionStorage.getItem('pkce_code_verifier');
      const redirectUri = sessionStorage.getItem('pkce_redirect_uri');

      if (!codeVerifier || !redirectUri) {
        console.error('Missing PKCE data');
        return;
      }

      try {
        const baseUrl = await getApiBaseUrl();
        const response = await fetch(`${baseUrl}${API_CONFIG.oauth.tokenEndpoint}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          body: new URLSearchParams({
            grant_type: 'authorization_code',
            client_id: API_CONFIG.oauth.clientId,
            code,
            redirect_uri: redirectUri,
            code_verifier: codeVerifier,
          }).toString(),
        });

        if (!response.ok) {
          const error = await response.text();
          console.error('Token exchange failed:', error);
          return;
        }

        const tokens = await response.json();
        await setTokens(tokens.access_token, tokens.refresh_token, tokens.expires_in);

        // Clear PKCE data and URL params
        sessionStorage.removeItem('pkce_code_verifier');
        sessionStorage.removeItem('pkce_redirect_uri');
        window.history.replaceState({}, '', window.location.pathname);
      } catch (error) {
        console.error('OAuth callback error:', error);
      }
    };

    handleCallback();
  }, [setTokens]);
}

function AppContent() {
  useOAuthCallback();
  const { theme, isDark } = useTheme();

  return (
    <PaperProvider theme={theme}>
      <StatusBar style={isDark ? 'light' : 'dark'} />
      <AppNavigator />
    </PaperProvider>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <SafeAreaProvider>
        <I18nProvider>
          <ThemeProvider>
            <AppContent />
          </ThemeProvider>
        </I18nProvider>
      </SafeAreaProvider>
    </QueryClientProvider>
  );
}
