import { useCallback, useEffect } from 'react';
import * as AuthSession from 'expo-auth-session';
import * as Crypto from 'expo-crypto';
import { useAuthStore } from '../store/authStore';
import { getApiBaseUrl, API_CONFIG } from '../api/config';
import { Platform } from 'react-native';

// PKCE helpers
function generateCodeVerifier(): string {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return base64URLEncode(array);
}

async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return base64URLEncode(new Uint8Array(digest));
}

function base64URLEncode(buffer: Uint8Array): string {
  let binary = '';
  buffer.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary)
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

  // Handle login with OAuth2 PKCE
  const login = useCallback(async () => {
    try {
      const baseUrl = await getApiBaseUrl();
      
      // Create auth request with PKCE
      const redirectUri = AuthSession.makeRedirectUri({
        scheme: 'openfamilycompass',
      });
      
      console.log('Redirect URI:', redirectUri);
      console.log('Base URL:', baseUrl);

      // Generate PKCE code verifier and challenge
      const codeVerifier = generateCodeVerifier();
      const codeChallenge = await generateCodeChallenge(codeVerifier);
      
      // Store code verifier for token exchange
      sessionStorage.setItem('pkce_code_verifier', codeVerifier);
      sessionStorage.setItem('pkce_redirect_uri', redirectUri);

      const authUrl = `${baseUrl}${API_CONFIG.oauth.authorizationEndpoint}?` +
        `client_id=${API_CONFIG.oauth.clientId}&` +
        `redirect_uri=${encodeURIComponent(redirectUri)}&` +
        `response_type=code&` +
        `scope=${encodeURIComponent(API_CONFIG.oauth.scopes.join(' '))}&` +
        `code_challenge=${codeChallenge}&` +
        `code_challenge_method=S256`;

      console.log('Auth URL:', authUrl);

      // For web, redirect directly instead of popup
      window.location.href = authUrl;
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  }, []);

  // Exchange authorization code for tokens
  const exchangeCodeForTokens = async (code: string, redirectUri: string) => {
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
      }).toString(),
    });

    if (!tokenResponse.ok) {
      throw new Error('Token exchange failed');
    }

    const tokens = await tokenResponse.json();
    await setTokens(tokens.access_token, tokens.refresh_token, tokens.expires_in);
  };

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
