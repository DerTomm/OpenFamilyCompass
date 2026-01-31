import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { ApiErrorResponse } from '../types/api';
import { API_CONFIG, getApiBaseUrl, secureStorage, STORAGE_KEYS } from './config';

// Track if we're currently refreshing to prevent multiple simultaneous refresh attempts
let isRefreshing = false;
let refreshAttempts = 0;
const MAX_REFRESH_ATTEMPTS = 1;

// Create axios instance
const createApiClient = (): AxiosInstance => {
  const client = axios.create({
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  // Request interceptor - add auth token
  client.interceptors.request.use(
    async (config: InternalAxiosRequestConfig) => {
      // Set base URL dynamically
      config.baseURL = `${await getApiBaseUrl()}/api/${API_CONFIG.apiVersion}`;

      // Add auth token if available
      const token = await secureStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }

      return config;
    },
    (error) => Promise.reject(error)
  );

  // Response interceptor - handle errors and token refresh
  client.interceptors.response.use(
    (response) => {
      // Reset refresh attempts on successful request
      refreshAttempts = 0;
      return response;
    },
    async (error: AxiosError<ApiErrorResponse>) => {
      const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

      // Check if this is an authentication error
      if (error.response?.status === 401) {
        // Prevent infinite loops - don't retry if already retried or max attempts reached
        if (originalRequest._retry || refreshAttempts >= MAX_REFRESH_ATTEMPTS || isRefreshing) {
          console.warn('Token refresh failed or max attempts reached. Clearing auth state.');
          await secureStorage.clear();
          isRefreshing = false;
          refreshAttempts = 0;
          // Redirect to login will be handled by the navigation guard
          return Promise.reject(error);
        }

        const refreshToken = await secureStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN);

        if (refreshToken) {
          originalRequest._retry = true;
          isRefreshing = true;
          refreshAttempts++;

          try {
            const baseUrl = await getApiBaseUrl();
            const response = await axios.post(`${baseUrl}/oauth2/token`, {
              grant_type: 'refresh_token',
              refresh_token: refreshToken,
              client_id: API_CONFIG.oauth.clientId,
            }, {
              headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            });

            const { access_token, refresh_token, expires_in } = response.data;

            // Store new tokens
            await secureStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, access_token);
            await secureStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, refresh_token);
            await secureStorage.setItem(
              STORAGE_KEYS.TOKEN_EXPIRY,
              (Date.now() + expires_in * 1000).toString()
            );

            isRefreshing = false;
            refreshAttempts = 0;

            // Retry original request with new token
            originalRequest.headers.Authorization = `Bearer ${access_token}`;
            return client(originalRequest);
          } catch (refreshError: any) {
            console.error('Token refresh failed:', refreshError);

            // Check for invalid_grant error (400) from OAuth2 token endpoint
            const isInvalidGrant = refreshError?.response?.status === 400 &&
              refreshError?.response?.data?.error === 'invalid_grant';

            if (isInvalidGrant) {
              console.warn('Invalid grant error - refresh token is invalid or expired');
            }

            // Clear all auth state on any refresh error
            await secureStorage.clear();
            isRefreshing = false;
            refreshAttempts = 0;
            return Promise.reject(refreshError);
          }
        } else {
          // No refresh token available - clear state
          await secureStorage.clear();
          isRefreshing = false;
          refreshAttempts = 0;
        }
      }

      return Promise.reject(error);
    }
  );

  return client;
};

export const apiClient = createApiClient();

// Type-safe API methods
export const api = {
  get: <T>(url: string, params?: object) =>
    apiClient.get<T>(url, { params }).then(res => res.data),

  post: <T>(url: string, data?: object) =>
    apiClient.post<T>(url, data).then(res => res.data),

  put: <T>(url: string, data?: object) =>
    apiClient.put<T>(url, data).then(res => res.data),

  delete: <T>(url: string) =>
    apiClient.delete<T>(url).then(res => res.data),
};
