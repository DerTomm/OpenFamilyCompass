import { useCallback } from 'react';
import * as AuthSession from 'expo-auth-session';
import * as WebBrowser from 'expo-web-browser';
import { useAuthStore } from '../store/authStore';
import { getApiBaseUrl, API_CONFIG } from '../api/config';

// Required for web browser redirect
WebBrowser.maybeCompleteAuthSession();

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
        path: 'callback',
      });

      const authUrl = `${baseUrl}${API_CONFIG.oauth.authorizationEndpoint}?` +
        `client_id=${API_CONFIG.oauth.clientId}&` +
        `redirect_uri=${encodeURIComponent(redirectUri)}&` +
        `response_type=code&` +
        `scope=${API_CONFIG.oauth.scopes.join(' ')}&` +
        `code_challenge_method=S256`;

      // Open browser for auth
      const result = await WebBrowser.openAuthSessionAsync(authUrl, redirectUri);
      
      if (result.type === 'success' && result.url) {
        // Extract authorization code from URL
        const url = new URL(result.url);
        const code = url.searchParams.get('code');
        
        if (code) {
          // Exchange code for tokens
          await exchangeCodeForTokens(code, redirectUri);
        }
      }
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
