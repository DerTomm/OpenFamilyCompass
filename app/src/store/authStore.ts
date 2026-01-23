import { create } from 'zustand';
import { secureStorage, STORAGE_KEYS } from '../api/config';
import { UserProfileResponse, UserRole } from '../types/api';
import { api } from '../api/client';

interface AuthState {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: UserProfileResponse | null;
  
  // Actions
  initialize: () => Promise<void>;
  setTokens: (accessToken: string, refreshToken: string, expiresIn: number) => Promise<void>;
  fetchUser: () => Promise<void>;
  logout: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  isAuthenticated: false,
  isLoading: true,
  user: null,

  initialize: async () => {
    try {
      const accessToken = await secureStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
      const tokenExpiry = await secureStorage.getItem(STORAGE_KEYS.TOKEN_EXPIRY);

      if (accessToken && tokenExpiry) {
        const expiryTime = parseInt(tokenExpiry, 10);
        
        if (Date.now() < expiryTime) {
          // Token still valid - fetch user profile
          await get().fetchUser();
          set({ isAuthenticated: true });
        } else {
          // Token expired - try refresh
          const refreshToken = await secureStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN);
          if (refreshToken) {
            // The API client interceptor will handle the refresh
            try {
              await get().fetchUser();
              set({ isAuthenticated: true });
            } catch {
              await secureStorage.clear();
              set({ isAuthenticated: false, user: null });
            }
          } else {
            await secureStorage.clear();
            set({ isAuthenticated: false, user: null });
          }
        }
      } else {
        set({ isAuthenticated: false, user: null });
      }
    } catch (error) {
      console.error('Auth initialization error:', error);
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
}));

// Selectors
export const selectIsAdmin = (state: AuthState) => state.user?.role === 'ADMIN';
export const selectIsParent = (state: AuthState) => state.user?.role === 'PARENT' || state.user?.role === 'ADMIN';
export const selectIsChild = (state: AuthState) => state.user?.role === 'CHILD';
export const selectUserRole = (state: AuthState): UserRole | null => state.user?.role ?? null;
