import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { ApiErrorResponse } from '../types/api';
import { API_CONFIG, getApiBaseUrl, secureStorage, STORAGE_KEYS } from './config';

// Track if we're currently refreshing to prevent multiple simultaneous refresh attempts
let isRefreshing = false;
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  
  failedQueue = [];
};

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
      // Set base URL dynamically if not already set or if it's relative
      if (!config.baseURL || !config.baseURL.startsWith('http')) {
         config.baseURL = `${await getApiBaseUrl()}/api/${API_CONFIG.apiVersion}`;
      }

      // Add auth token if available
      try {
        const token = await secureStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
        if (token && !config.headers.Authorization) {
            config.headers.Authorization = `Bearer ${token}`;
        }
      } catch (e) {
        console.warn('Error reading token from storage:', e);
      }

      return config;
    },
    (error) => Promise.reject(error)
  );

  // Response interceptor - handle errors and token refresh
  client.interceptors.response.use(
    (response) => {
      return response;
    },
    async (error: AxiosError<ApiErrorResponse>) => {
      const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

      // Check if this is an authentication/authorization error
      // NOTE: 403 can happen when the access token is valid but missing role claims (e.g. after a refresh);
      // in that case a refresh can fix the token and the retry will succeed.
      if ((error.response?.status === 401 || error.response?.status === 403) && !originalRequest._retry) {
        if (isRefreshing) {
          return new Promise(function(resolve, reject) {
            failedQueue.push({resolve, reject});
          }).then(token => {
            originalRequest.headers.Authorization = 'Bearer ' + token;
            return client(originalRequest);
          }).catch(err => {
            return Promise.reject(err);
          });
        }

        originalRequest._retry = true;
        isRefreshing = true;

        const refreshToken = await secureStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN);

        if (refreshToken) {
          try {
            const baseUrl = await getApiBaseUrl();
            const response = await axios.post(`${baseUrl}/api/v1/auth/refresh`, {
              refreshToken: refreshToken
            });

            const { accessToken, refreshToken: newRefreshToken, expiresIn } = response.data;

            // Store new tokens
            await secureStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, accessToken);
            if (newRefreshToken) {
              await secureStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, newRefreshToken);
            }
            await secureStorage.setItem(
              STORAGE_KEYS.TOKEN_EXPIRY,
              (Date.now() + expiresIn * 1000).toString()
            );

            // Update header for future requests
            client.defaults.headers.common['Authorization'] = 'Bearer ' + accessToken;
            originalRequest.headers.Authorization = 'Bearer ' + accessToken;

            processQueue(null, accessToken);
            return client(originalRequest);
          } catch (refreshError: any) {
            processQueue(refreshError, null);
            console.error('Token refresh failed:', refreshError);

            // Clear all auth state on any refresh error
            await secureStorage.clear();
            
            // Redirect will be handled by auth state change
            return Promise.reject(refreshError);
          } finally {
             isRefreshing = false;
          }
        } else {
          // No refresh token available - clear state
          await secureStorage.clear();
          return Promise.reject(error);
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
