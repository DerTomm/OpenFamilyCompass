import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Constants, { ExecutionEnvironment } from 'expo-constants';
import { StatusBar } from 'expo-status-bar';
import React from 'react';
import { PaperProvider } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { usePushNotifications } from './src/hooks/usePushNotifications';
import './src/i18n/config'; // Initialize i18n
import { I18nProvider } from './src/i18n/I18nContext';
import { AppNavigator } from './src/navigation/AppNavigator';
import { useAuthStore } from './src/store/authStore';
import { ThemeProvider, useTheme } from './src/theme/ThemeContext';

const isExpoGo = Constants.executionEnvironment === ExecutionEnvironment.StoreClient;

// Verhalten bei eingehenden Benachrichtigungen (App im Vordergrund).
// Dynamisches require verhindert, dass expo-notifications in Expo Go geladen wird.
if (!isExpoGo) {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const Notifications = require('expo-notifications');
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowAlert: true,
      shouldPlaySound: true,
      shouldSetBadge: true,
    }),
  });
}

// Create React Query client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5, // 5 minutes
      retry: 2,
    },
  },
});

function AppContent() {
  const { theme, isDark } = useTheme();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  usePushNotifications(isAuthenticated);

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

