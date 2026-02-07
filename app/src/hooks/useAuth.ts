import { useCallback } from 'react';
import { getApiBaseUrl } from '../api/config';
import { useAuthStore } from '../store/authStore';

export const useAuth = () => {
  const { setTokens, logout, isAuthenticated, isLoading, user } = useAuthStore();

  // Handle login with direct REST API call
  const login = useCallback(async (username?: string, password?: string) => {
    try {
      if (!username || !password) {
        throw new Error('Username and password are required');
      }

      const baseUrl = await getApiBaseUrl();
      const loginUrl = `${baseUrl}/api/v1/auth/login`;

      console.log('[AUTH] Attempting login at:', loginUrl);

      const response = await fetch(loginUrl, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        console.error('[AUTH] Login failed:', response.status, errorText);
        throw new Error('Invalid credentials');
      }

      const data = await response.json();
      console.log('[AUTH] Login successful, tokens received');

      await setTokens(data.accessToken, data.refreshToken, data.expiresIn);
      
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  }, [setTokens]);

  // Handle logout
  const handleLogout = useCallback(async () => {
    await logout();
  }, [logout]);

  return {
    login,
    logout: handleLogout,
    isAuthenticated,
    isLoading,
    user,
  };
};
