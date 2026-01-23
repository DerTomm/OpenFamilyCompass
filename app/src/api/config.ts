import * as SecureStore from 'expo-secure-store';

// API Configuration
export const API_CONFIG = {
  // Default to localhost for development - can be changed in settings
  baseUrl: 'http://localhost:8080',
  apiVersion: 'v1',
  
  // OAuth2 PKCE Configuration
  oauth: {
    clientId: 'openfamilycompass-mobile',
    authorizationEndpoint: '/oauth2/authorize',
    tokenEndpoint: '/oauth2/token',
    scopes: ['openid', 'profile', 'read', 'write'],
  },
};

// Storage keys
export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'access_token',
  REFRESH_TOKEN: 'refresh_token',
  TOKEN_EXPIRY: 'token_expiry',
  SERVER_URL: 'server_url',
  USER_PROFILE: 'user_profile',
};

// Helper functions for secure storage
export const secureStorage = {
  async getItem(key: string): Promise<string | null> {
    try {
      return await SecureStore.getItemAsync(key);
    } catch {
      return null;
    }
  },

  async setItem(key: string, value: string): Promise<void> {
    await SecureStore.setItemAsync(key, value);
  },

  async removeItem(key: string): Promise<void> {
    await SecureStore.deleteItemAsync(key);
  },

  async clear(): Promise<void> {
    const keys = Object.values(STORAGE_KEYS);
    await Promise.all(keys.map(key => SecureStore.deleteItemAsync(key)));
  },
};

// Get the current API base URL
export const getApiBaseUrl = async (): Promise<string> => {
  const storedUrl = await secureStorage.getItem(STORAGE_KEYS.SERVER_URL);
  return storedUrl || API_CONFIG.baseUrl;
};

// Build full API URL
export const buildApiUrl = async (path: string): Promise<string> => {
  const baseUrl = await getApiBaseUrl();
  return `${baseUrl}/api/${API_CONFIG.apiVersion}${path}`;
};
