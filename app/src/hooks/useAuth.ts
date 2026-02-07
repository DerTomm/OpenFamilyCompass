import * as AuthSession from 'expo-auth-session';
import * as Crypto from 'expo-crypto';
import * as Linking from 'expo-linking';
import * as WebBrowser from 'expo-web-browser';
import { useCallback } from 'react';
import { Platform } from 'react-native';
import { API_CONFIG, getApiBaseUrl, secureStorage, STORAGE_KEYS } from '../api/config';
import { useAuthStore } from '../store/authStore';

// Base64 encoding that works on both Web and Native
function base64Encode(bytes: Uint8Array): string {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
  let result = '';
  const len = bytes.length;
  for (let i = 0; i < len; i += 3) {
    const b1 = bytes[i];
    const b2 = i + 1 < len ? bytes[i + 1] : 0;
    const b3 = i + 2 < len ? bytes[i + 2] : 0;

    result += chars[b1 >> 2];
    result += chars[((b1 & 3) << 4) | (b2 >> 4)];
    result += i + 1 < len ? chars[((b2 & 15) << 2) | (b3 >> 6)] : '=';
    result += i + 2 < len ? chars[b3 & 63] : '=';
  }
  return result;
}

// PKCE helpers
async function generateCodeVerifier(): Promise<string> {
  const randomBytes = await Crypto.getRandomBytesAsync(32);
  const base64 = base64Encode(randomBytes);
  return base64
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

async function generateCodeChallenge(verifier: string): Promise<string> {
  const digest = await Crypto.digestStringAsync(
    Crypto.CryptoDigestAlgorithm.SHA256,
    verifier,
    { encoding: Crypto.CryptoEncoding.BASE64 }
  );
  return digest
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

export const useAuth = () => {
  const { setTokens, logout, isAuthenticated, isLoading, user } = useAuthStore();

  // Create PKCE auth request
  const useAuthRequest = () => {
    const discovery = AuthSession.useAutoDiscovery(API_CONFIG.baseUrl);

    const [request, response, promptAsync] = AuthSession.useAuthRequest(
      {
        clientId: API_CONFIG.oauth.clientId,
        scopes: API_CONFIG.oauth.scopes,
        usePKCE: true,
        redirectUri: AuthSession.makeRedirectUri({
          scheme: 'openfamilycompass',
          path: 'callback',
        }),
      },
      discovery
    );

    return { request, response, promptAsync, discovery };
  };

  // Exchange authorization code for tokens
  const exchangeCodeForTokens = useCallback(async (code: string, redirectUri: string, codeVerifier: string) => {
    const baseUrl = await getApiBaseUrl();

    const tokenResponse = await fetch(`${baseUrl}${API_CONFIG.oauth.tokenEndpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        grant_type: 'authorization_code',
        client_id: API_CONFIG.oauth.clientId,
        code,
        redirect_uri: redirectUri,
        code_verifier: codeVerifier,
      }).toString(),
    });

    if (!tokenResponse.ok) {
      const errorText = await tokenResponse.text();
      console.error('Token exchange failed:', errorText);
      throw new Error('Token exchange failed');
    }

    const tokens = await tokenResponse.json();

    // Ensure tokens are strings before storing
    const accessToken = tokens.access_token?.toString() || '';
    const refreshToken = tokens.refresh_token?.toString() || '';
    const expiresIn = tokens.expires_in || 3600;

    if (!accessToken) {
      throw new Error('No access token received');
    }

    await setTokens(accessToken, refreshToken, expiresIn);
  }, [setTokens]);

  // Handle login with OAuth2 PKCE
  const login = useCallback(async () => {
    try {
      const baseUrl = await getApiBaseUrl();

      // Create auth request with PKCE
      // For web, use the current origin without a path (matches backend registered redirect URIs)
      // For native, use the custom scheme with callback path
      const redirectUri = Platform.OS === 'web'
        ? window.location.origin
        : AuthSession.makeRedirectUri({
            scheme: 'openfamilycompass',
            path: 'callback',
          });

      // Generate PKCE code verifier and challenge
      const codeVerifier = await generateCodeVerifier();
      const codeChallenge = await generateCodeChallenge(codeVerifier);

      // Store PKCE data in secure storage
      // Web: Persisted in localStorage for retrieval after redirect (App.tsx handles callback)
      // Native: Stored in SecureStore, but we use the closure variable below since WebBrowser waits synchronously
      await secureStorage.setItem(STORAGE_KEYS.PKCE_CODE_VERIFIER, codeVerifier);
      await secureStorage.setItem(STORAGE_KEYS.PKCE_REDIRECT_URI, redirectUri);

      const authUrl = `${baseUrl}${API_CONFIG.oauth.authorizationEndpoint}?` +
        `client_id=${API_CONFIG.oauth.clientId}&` +
        `redirect_uri=${encodeURIComponent(redirectUri)}&` +
        `response_type=code&` +
        `scope=${encodeURIComponent(API_CONFIG.oauth.scopes.join(' '))}&` +
        `code_challenge=${codeChallenge}&` +
        `code_challenge_method=S256&` +
        `prompt=login`; // Force re-authentication after logout

      // Platform-specific navigation
      if (Platform.OS === 'web') {
        window.location.href = authUrl;
      } else {
        // For native apps, use WebBrowser to open auth URL
        const result = await WebBrowser.openAuthSessionAsync(authUrl, redirectUri);

        if (result.type === 'success' && result.url) {
          // Extract authorization code from callback URL
          const url = Linking.parse(result.url);
          const code = url.queryParams?.code as string | undefined;

          if (code) {
            // Exchange code for tokens
            await exchangeCodeForTokens(code, redirectUri, codeVerifier);
          }
        }
      }
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  }, [exchangeCodeForTokens]);

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
    useAuthRequest,
  };
};
