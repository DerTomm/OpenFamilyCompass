# Token Authentication Implementation

## Overview

The application now supports JWT token-based authentication for the Android app, while maintaining form-based authentication for the web interface.

## Architecture

### Backend (Spring Boot)

#### Token Configuration
- **Access Token**: 15 minutes validity
- **Refresh Token**: 30 days validity
- **Algorithm**: HMAC SHA-256
- **Secret**: Configured in `application.properties` (jwt.secret)

#### Key Components

1. **JwtTokenService** (`src/main/java/com/family/kidschores/security/JwtTokenService.java`)
   - Generates access and refresh tokens
   - Validates tokens
   - Extracts user information from tokens

2. **JwtAuthenticationFilter** (`src/main/java/com/family/kidschores/security/JwtAuthenticationFilter.java`)
   - Intercepts requests with `Authorization: Bearer <token>` header
   - Validates token and sets SecurityContext
   - Positioned before UsernamePasswordAuthenticationFilter

3. **AuthApiController** (`src/main/java/com/family/kidschores/controller/AuthApiController.java`)
   - `POST /api/auth/login` - Authenticates user and returns tokens
   - `POST /api/auth/refresh` - Refreshes expired access token using refresh token
   - `POST /api/auth/logout` - Client-side logout (JWT is stateless)

#### Security Configuration
- `/api/auth/**` endpoints are public (no authentication required)
- API requests use stateless sessions (`SessionCreationPolicy.STATELESS`)
- CSRF protection disabled for `/api/**` endpoints
- Form-based authentication still active for web UI

### Android App

#### Key Components

1. **AuthApiClient** (`android-app/app/src/main/java/com/family/kidschores/AuthApiClient.kt`)
   - HTTP client for authentication API calls
   - Coroutine-based async operations
   - Methods:
     - `suspend fun login(username: String, password: String): Result<TokenResponse>`
     - `suspend fun refreshToken(refreshToken: String): Result<TokenResponse>`

2. **SessionManager** (`android-app/app/src/main/java/com/family/kidschores/SessionManager.kt`)
   - Stores tokens in SharedPreferences
   - Methods:
     - `saveTokens(accessToken, refreshToken, expiresIn, username, role)`
     - `getAccessToken()`, `getRefreshToken()`
     - `isAccessTokenValid()` - Checks if access token is still valid (with 1-minute buffer)
     - `hasValidSession()` - Checks if refresh token exists
     - `getUserRole()` - Returns stored user role
     - `clearSession()` - Removes all authentication data

3. **MainActivity** (`android-app/app/src/main/java/com/family/kidschores/MainActivity.kt`)
   - Automatically refreshes tokens on app start if access token expired
   - Injects `Authorization: Bearer <token>` header into WebView requests
   - Handles 401 errors by attempting token refresh
   - Falls back to login page if refresh token expired

## Authentication Flow

### Initial Login (Form-based in WebView)
1. User opens app
2. If no valid session, loads `/login` page
3. User submits login form
4. Web app authenticates via Spring Security
5. User is redirected to `/dashboard`

### Token-based Authentication (API)
1. App can call `/api/auth/login` with username/password
2. Backend validates credentials
3. Backend generates access token (15 min) and refresh token (30 days)
4. App stores tokens in SharedPreferences
5. All subsequent requests include `Authorization: Bearer <accessToken>` header

### Token Refresh
1. On app start or when access token expires (< 1 minute remaining):
   - App calls `/api/auth/refresh` with refresh token
   - Backend validates refresh token
   - Backend issues new access token and refresh token
   - App stores new tokens
2. If refresh fails (expired/invalid):
   - App redirects to login page
   - User must re-authenticate

### Automatic Session Persistence
- Tokens are stored locally in SharedPreferences
- On app restart:
  - If refresh token exists and is valid → auto-login
  - If access token expired → automatic refresh
  - If refresh token expired (after 30 days) → require new login

## Configuration

### Backend (`src/main/resources/application.properties`)
```properties
# JWT Configuration
jwt.secret=your-very-long-secret-key-at-least-256-bits-change-this-in-production
jwt.access-token-validity=900000    # 15 minutes in milliseconds
jwt.refresh-token-validity=2592000000  # 30 days in milliseconds
```

**Important**: Change `jwt.secret` in production to a secure random string!

### Android App
No configuration needed - tokens are automatically managed by SessionManager.

## Testing

### Test Login API
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Expected response:
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "username": "admin",
  "role": "PARENT"
}
```

### Test Refresh API
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<your-refresh-token>"}'
```

### Test Authenticated Request
```bash
curl http://localhost:8080/api/some-endpoint \
  -H "Authorization: Bearer <your-access-token>"
```

## Security Considerations

1. **Token Storage**: Tokens are stored in SharedPreferences which is reasonably secure for Android apps
2. **HTTPS**: Always use HTTPS in production to prevent token interception
3. **Secret Key**: Use a strong random secret key (at least 256 bits) in production
4. **Token Expiry**: Short-lived access tokens (15 min) limit exposure if compromised
5. **Refresh Token Rotation**: Consider implementing refresh token rotation for enhanced security
6. **Logout**: Tokens are cleared client-side; for enhanced security, consider token blacklisting on server

## Benefits

1. **Persistent Sessions**: Users stay logged in for 30 days (until refresh token expires)
2. **Security**: Short-lived access tokens with automatic refresh
3. **Stateless API**: Backend doesn't need to maintain session state
4. **Hybrid Approach**: Web UI still uses traditional form-based auth, mobile app uses tokens
5. **Automatic Recovery**: App automatically refreshes tokens when needed

## Migration Notes

### Removed from Android App
- Cookie-based authentication
- CookieManager usage
- Manual cookie extraction and restoration
- `detectAndSaveSession()` method
- `restoreCookies()` method

### Added to Android App
- JWT token storage and management
- Authorization header injection into WebView
- Automatic token refresh on app start
- Token refresh on 401 errors
- Coroutine-based API client

## Future Enhancements

1. **Biometric Authentication**: Add fingerprint/face unlock for re-authentication
2. **Token Rotation**: Implement refresh token rotation for enhanced security
3. **Token Blacklisting**: Add server-side token revocation on logout
4. **Custom Login Screen**: Replace WebView login with native Android UI
5. **Push Notifications**: Add device token registration for notifications
6. **Multi-Device Support**: Track active sessions per user
