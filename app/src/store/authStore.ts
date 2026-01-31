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
            // If profile fetch fails, clear auth state to prevent loops
            await secureStorage.clear();
            set({ isAuthenticated: false, user: null });
          }
        } else {
          // Token expired - clear storage and let user log in again
          console.warn('Token expired, clearing auth state');
          await secureStorage.clear();
          set({ isAuthenticated: false, user: null });
        }
      } else {
        set({ isAuthenticated: false, user: null });
      }
    } catch (error) {
      console.error('Auth initialization error:', error);
      await secureStorage.clear();
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
    await secureStorage.clear();
    set({ isAuthenticated: false, user: null });
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
