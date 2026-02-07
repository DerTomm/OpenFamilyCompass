import { create } from 'zustand';
import { api } from '../api/client';
import { secureStorage, STORAGE_KEYS } from '../api/config';
import { UserProfileResponse, UserRole } from '../types/api';

interface AuthState {
  isAuthenticated: boolean;
  isLoading: boolean;
  isSetupComplete: boolean;
  user: UserProfileResponse | null;

  // Actions
  initialize: () => Promise<void>;
  setTokens: (accessToken: string, refreshToken: string, expiresIn: number) => Promise<void>;
  fetchUser: () => Promise<void>;
  logout: () => Promise<void>;
  completeSetup: () => void;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  isAuthenticated: false,
  isLoading: true,
  isSetupComplete: false,
  user: null,

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
          // Token expired - clear only auth data (keep SERVER_URL)
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
    await secureStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, accessToken);
    await secureStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, refreshToken);
    await secureStorage.setItem(
      STORAGE_KEYS.TOKEN_EXPIRY,
      (Date.now() + expiresIn * 1000).toString()
    );

    await get().fetchUser();
    set({ isAuthenticated: true });
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
      console.log('[LOGOUT] Starting logout process...');

      // Clear all auth-related data FIRST (keep SERVER_URL for re-login)
      console.log('[LOGOUT] Clearing local storage...');
      await secureStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
      await secureStorage.removeItem(STORAGE_KEYS.REFRESH_TOKEN);
      await secureStorage.removeItem(STORAGE_KEYS.TOKEN_EXPIRY);
      await secureStorage.removeItem(STORAGE_KEYS.USER_PROFILE);
      await secureStorage.removeItem(STORAGE_KEYS.PKCE_CODE_VERIFIER);
      await secureStorage.removeItem(STORAGE_KEYS.PKCE_REDIRECT_URI);
      console.log('[LOGOUT] Local storage cleared');

      // Delete all cookies to clear backend session (JSESSIONID)
      if (typeof document !== 'undefined') {
        console.log('[LOGOUT] Clearing all cookies...');
        const cookies = document.cookie.split(';');
        for (let i = 0; i < cookies.length; i++) {
          const cookie = cookies[i];
          const eqPos = cookie.indexOf('=');
          const name = eqPos > -1 ? cookie.substring(0, eqPos).trim() : cookie.trim();
          
          // Delete cookie for all possible paths and domains
          document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`;
          document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;domain=${window.location.hostname}`;
          document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/;domain=.${window.location.hostname}`;
          
          console.log('[LOGOUT] Deleted cookie:', name);
        }
      }

      // Verify tokens are really deleted
      const checkToken = await secureStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
      console.log('[LOGOUT] Token after deletion:', checkToken === null ? 'NULL (OK)' : `STILL EXISTS: ${checkToken}`);

      // Set state to logged out
      set({ isAuthenticated: false, user: null, isLoading: false });
      console.log('[LOGOUT] State updated to logged out');

      // On web: Reload page to ensure clean state and prevent auto-login
      if (typeof window !== 'undefined') {
        console.log('[LOGOUT] Reloading page to ensure clean state...');
        window.location.href = window.location.origin;
      }
    } catch (error) {
      console.error('[LOGOUT] Logout error:', error);
      set({ isAuthenticated: false, user: null, isLoading: false });
    }
  },

  completeSetup: () => {
    set({ isSetupComplete: true });
  },
}));

// Selectors
export const selectIsAdmin = (state: AuthState) => state.user?.role === 'ADMIN';
export const selectIsParent = (state: AuthState) => state.user?.role === 'PARENT' || state.user?.role === 'ADMIN';
export const selectIsChild = (state: AuthState) => state.user?.role === 'CHILD';
export const selectUserRole = (state: AuthState): UserRole | null => state.user?.role ?? null;
