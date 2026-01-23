import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { getApiBaseUrl, secureStorage, STORAGE_KEYS, API_CONFIG } from './config';
import { ApiErrorResponse } from '../types/api';

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
    (response) => response,
    async (error: AxiosError<ApiErrorResponse>) => {
      const originalRequest = error.config;

      // If 401 and we have a refresh token, try to refresh
      if (error.response?.status === 401 && originalRequest) {
        const refreshToken = await secureStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN);
        
        if (refreshToken) {
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

            // Retry original request
            originalRequest.headers.Authorization = `Bearer ${access_token}`;
            return client(originalRequest);
          } catch (refreshError) {
            // Refresh failed - clear tokens and redirect to login
            await secureStorage.clear();
            throw refreshError;
          }
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
