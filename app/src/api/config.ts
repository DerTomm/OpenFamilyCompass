import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

// API Configuration
export const API_CONFIG = {
  // Default to localhost for development - can be changed in settings
  baseUrl: 'http://localhost:8080',
  apiVersion: 'v1',
};

// Storage keys
export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'access_token',
  REFRESH_TOKEN: 'refresh_token',
  TOKEN_EXPIRY: 'token_expiry',
  SERVER_URL: 'server_url',
  USER_PROFILE: 'user_profile',
};

// Web storage fallback using localStorage
const webStorage = {
  async getItem(key: string): Promise<string | null> {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem(key);
  },
  async setItem(key: string, value: string): Promise<void> {
    if (typeof window === 'undefined') return;
    localStorage.setItem(key, value);
  },
  async removeItem(key: string): Promise<void> {
    if (typeof window === 'undefined') return;
    localStorage.removeItem(key);
  },
  async clear(): Promise<void> {
    if (typeof window === 'undefined') return;
    const keys = Object.values(STORAGE_KEYS);
    keys.forEach(key => localStorage.removeItem(key));
  },
};

// Native storage using SecureStore
const nativeStorage = {
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

// Use appropriate storage based on platform
export const secureStorage = Platform.OS === 'web' ? webStorage : nativeStorage;

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

