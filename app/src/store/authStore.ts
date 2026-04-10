import AsyncStorage from '@react-native-async-storage/async-storage';
import { create } from 'zustand';
import { api } from '../api/client';
import { getApiBaseUrl, secureStorage, STORAGE_KEYS } from '../api/config';
import { notificationsApi } from '../api/services';
import { UserProfileResponse, UserRole } from '../types/api';

interface AuthState {
  isAuthenticated: boolean;
  isLoading: boolean;
  isSetupComplete: boolean;
  user: UserProfileResponse | null;
  avatarVersion: number;

  // Actions
  initialize: () => Promise<void>;
  setTokens: (accessToken: string, refreshToken: string, expiresIn: number) => Promise<void>;
  fetchUser: () => Promise<void>;
  logout: () => Promise<void>;
  completeSetup: () => void;
  bumpAvatarVersion: () => void;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  isAuthenticated: false,
  isLoading: true,
  isSetupComplete: false,
  user: null,
  avatarVersion: 0,

  initialize: async () => {
    try {
      // Check if server URL is configured
      const serverUrl = await secureStorage.getItem(STORAGE_KEYS.SERVER_URL);
      if (!serverUrl) {
        set({ isLoading: false, isSetupComplete: false });
        return;
      }

      set({ isSetupComplete: true });

      const accessToken = await secureStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
      const tokenExpiry = await secureStorage.getItem(STORAGE_KEYS.TOKEN_EXPIRY);

      if (accessToken && tokenExpiry) {
        const expiryTime = parseInt(tokenExpiry, 10);

        if (Date.now() < expiryTime) {
          // Token still valid - fetch user profile
          try {
            await get().fetchUser();
            set({ isAuthenticated: true });
          } catch (error) {
            console.error('Failed to fetch user profile during initialization:', error);
            // If profile fetch fails, clear only auth data (keep SERVER_URL)
            await secureStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
            await secureStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
            await secureStorage.removeItem(STORAGE_KEYS.TOKEN_EXPIRY);
            await secureStorage.removeItem(STORAGE_KEYS.USER_PROFILE);
            set({ isAuthenticated: false, user: null });
          }
        } else {
          // Token expired - try to refresh it before giving up
          try {
            const refreshToken = await secureStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN);
            if (refreshToken) {
              const baseUrl = await getApiBaseUrl();
              const response = await fetch(`${baseUrl}/api/v1/auth/refresh`, {
                method: 'POST',
                headers: {
                  'Content-Type': 'application/json'
                },
                body: JSON.stringify({ refreshToken })
              });

              if (response.ok) {
                const data = await response.json();
                await get().setTokens(data.accessToken, data.refreshToken, data.expiresIn);
                return; // Successfully refreshed and set tokens (which fetches user)
              }
            }
          } catch (refreshErr) {
            console.error('Initial refresh failed', refreshErr);
          }

          // Token expired and refresh failed/not possible - clear only auth data (keep SERVER_URL)
          console.warn('Token expired, clearing auth state');
          await secureStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
          await secureStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
          await secureStorage.removeItem(STORAGE_KEYS.TOKEN_EXPIRY);
          await secureStorage.removeItem(STORAGE_KEYS.USER_PROFILE);
          set({ isAuthenticated: false, user: null });
        }
      } else {
        set({ isAuthenticated: false, user: null });
      }
    } catch (error) {

      console.error('Auth initialization error:', error);
      // Clear only auth data on error (keep SERVER_URL)
      await secureStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
      await secureStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
      await secureStorage.removeItem(STORAGE_KEYS.TOKEN_EXPIRY);
      await secureStorage.removeItem(STORAGE_KEYS.USER_PROFILE);
      set({ isAuthenticated: false, user: null });
    } finally {
      set({ isLoading: false });
    }
  },

  setTokens: async (accessToken, refreshToken, expiresIn) => {
    console.log('[AUTH] setTokens called');
    console.log('[AUTH] Access token length:', accessToken?.length || 0);
    console.log('[AUTH] Refresh token length:', refreshToken?.length || 0);
    console.log('[AUTH] Expires in:', expiresIn, 'seconds');

    if (!accessToken) {
      console.error('[AUTH] No access token provided!');
      throw new Error('No access token received from authorization server');
    }

    if (!refreshToken) {
      console.warn('[AUTH] WARNING: No refresh token received! You will need to login again after token expiry.');
    }

    try {
      await secureStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, accessToken);
      console.log('[AUTH] Access token stored');

      if (refreshToken) {
        await secureStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, refreshToken);
        console.log('[AUTH] Refresh token stored');
      }

      const expiry = (Date.now() + expiresIn * 1000).toString();
      await secureStorage.setItem(STORAGE_KEYS.TOKEN_EXPIRY, expiry);
      console.log('[AUTH] Token expiry stored:', new Date(parseInt(expiry)));

      // Verify tokens were stored
      const storedAccessToken = await secureStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
      const storedRefreshToken = await secureStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN);
      console.log('[AUTH] Verification - Access token stored:', !!storedAccessToken);
      console.log('[AUTH] Verification - Refresh token stored:', !!storedRefreshToken);

      console.log('[AUTH] Fetching user profile...');
      try {
        await get().fetchUser();
        console.log('[AUTH] User profile fetched, setting authenticated=true');
        set({ isAuthenticated: true });
      } catch (err) {
        console.error('[AUTH] Failed to fetch user profile after setting tokens:', err);
        // Don't throw here, otherwise the login screen might show a generic error.
        // The user might be logged in but the profile fetch failed (e.g. temporary network issue).
        // However, for consistency, if profile fetch fails, we might want to consider it a failed login
        // or just proceed with limited info. For now, let's re-throw to be safe so UI handles it.
        throw err;
      }
      console.log('[AUTH] setTokens complete');
    } catch (error) {
      console.error('[AUTH] setTokens error:', error);
      throw error;
    }
  },

  fetchUser: async () => {
    try {
      const user = await api.get<UserProfileResponse>('/profile');
      console.log('User profile fetched:', user);
      console.log('User role:', user.role);
      console.log('Is Admin:', user.role === 'ADMIN');
      set({ user });
    } catch (error) {
      console.error('Failed to fetch user profile:', error);
      throw error;
    }
  },

  logout: async () => {
    try {
      console.log('[LOGOUT] Starting client-side logout...');

      // 1. Unregister FCM device token while access token is still valid
      try {
        const fcmToken = await AsyncStorage.getItem('fcm_current_token');
        if (fcmToken) {
          await notificationsApi.unregisterDevice(fcmToken);
          console.log('[LOGOUT] FCM device token unregistered');
        }
      } catch (fcmErr) {
        console.warn('[LOGOUT] Failed to unregister FCM token:', fcmErr);
      } finally {
        // Always clear FCM state so the next user gets a fresh device record
        await AsyncStorage.removeItem('fcm_current_token');
        await AsyncStorage.removeItem('fcm_device_id');
      }

      // 2. Clear all tokens from secure storage
      console.log('[LOGOUT] Clearing tokens and auth data...');
      await secureStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
      await secureStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
      await secureStorage.removeItem(STORAGE_KEYS.TOKEN_EXPIRY);
      await secureStorage.removeItem(STORAGE_KEYS.USER_PROFILE);
      console.log('[LOGOUT] All tokens cleared');

      // 2. Verify tokens are deleted
      const checkToken = await secureStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
      console.log('[LOGOUT] Token verification:', checkToken === null ? 'NULL (OK)' : `STILL EXISTS: ${checkToken}`);

      // 3. Set state to logged out - this will trigger navigation to login screen
      set({ isAuthenticated: false, user: null, isLoading: false });
      console.log('[LOGOUT] State updated to logged out - navigation will follow');
    } catch (error) {
      console.error('[LOGOUT] Logout error:', error);
      set({ isAuthenticated: false, user: null, isLoading: false });
    }
  },

  completeSetup: () => {
    set({ isSetupComplete: true });
  },

  bumpAvatarVersion: () => {
    set((state) => ({ avatarVersion: state.avatarVersion + 1 }));
  },
}));

// Selectors
export const selectIsAdmin = (state: AuthState) => state.user?.role === 'ADMIN';
export const selectIsParent = (state: AuthState) => state.user?.role === 'PARENT' || state.user?.role === 'ADMIN';
export const selectIsChild = (state: AuthState) => state.user?.role === 'CHILD';
export const selectUserRole = (state: AuthState): UserRole | null => state.user?.role ?? null;
