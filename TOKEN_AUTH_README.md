# Token Authentication

## Overview

The REST API of OpenFamilyCompass is **fully stateless** and authenticates every request via a **JWT bearer token** (RS256, signed with an RSA key). The Expo/React Native mobile app uses this API exclusively.

## Architecture

### Backend (Spring Boot)

#### Token characteristics

- **Algorithm**: `RS256` (RSA + SHA-256)
- **Access token lifetime**: 1 hour
- **Refresh token lifetime**: 30 days
- **Claims**: `sub` (username), `iss: self`, `iat`, `exp`, `scope`, `roles`, plus `type: refresh` on refresh tokens
- **Role hierarchy**: an `ADMIN` role implicitly also grants `PARENT` (see `SecurityConfig.jwtAuthenticationConverter`).

#### Key management

JWT signing keys are auto-generated on first startup:

- Generated with BouncyCastle (2048-bit RSA, self-signed cert, valid one year).
- Stored in a PKCS#12 keystore — default `jwt-keys.pfx` in the working directory.
- Persisted in a Docker-named volume (`backend_config`) so tokens survive restarts.

Relevant configuration properties (with defaults):

```yaml
app:
  security:
    jwt:
      keystore-path: jwt-keys.pfx         # APP_SECURITY_JWT_KEYSTORE_PATH
      keystore-password: changeit          # APP_SECURITY_JWT_KEYSTORE_PASSWORD
      key-alias: jwt-key                   # APP_SECURITY_JWT_KEY_ALIAS
```

If the keystore file does not exist, `JwtConfig#loadOrGenerateKeyPair` creates it. No manual `keytool` step is required.

#### Key components

- **`config/JwtConfig`** — loads/generates the keystore, exposes `JwtEncoder` and `JwtDecoder` beans (Nimbus).
- **`security/TokenService`** — `generateToken(Authentication)`, `generateRefreshToken(Authentication)`, `validateToken(String)`, `getUsernameFromToken(String)`.
- **`config/SecurityConfig`** — stateless `SecurityFilterChain` using Spring Security's `oauth2ResourceServer().jwt(...)` with a custom `JwtAuthenticationConverter` (reads the `roles` claim).
- **`security/RateLimitingFilter`** — simple in-memory rate limiting placed before `UsernamePasswordAuthenticationFilter`.
- **`api/v1/AuthApiController`** — public auth endpoints.

#### Endpoints

All endpoints are public (do not require authentication) unless stated otherwise.

| Method | Path                              | Description                                                       |
| ------ | --------------------------------- | ----------------------------------------------------------------- |
| POST   | `/api/v1/auth/login`              | Authenticate with username/password, return token pair            |
| POST   | `/api/v1/auth/refresh`            | Exchange a refresh token for a new token pair (rotated)           |
| GET    | `/api/v1/auth/me`                 | Current user's profile (requires a valid access token)            |
| POST   | `/api/v1/auth/change-password`    | Change the current user's password (requires a valid access token)|

Public static resources, Swagger UI (`/swagger-ui.html`, `/v3/api-docs/**`), Spring Actuator (`/actuator/**`) and reward images (`GET /api/v1/rewards/*/image`) are also whitelisted.

### Mobile app (Expo / React Native)

#### Key components

- **`app/src/api/config.ts`** — stores the server URL, access token, refresh token and token expiry. On native devices the values are stored in **expo-secure-store**; on web they fall back to `localStorage`.
- **`app/src/api/client.ts`** — single axios instance with two interceptors:
  - **Request interceptor**: attaches `Authorization: Bearer <accessToken>` from secure storage.
  - **Response interceptor**: on `401` or `403` (to also recover from tokens lacking role claims), transparently calls `POST /api/v1/auth/refresh`, stores the rotated tokens, retries the original request, and serializes concurrent failures through a shared queue.
- **`app/src/store/authStore.ts`** — Zustand store that exposes `initialize`, `login`, `logout`, `completeSetup`, etc., and the derived selectors `selectIsAdmin` / `selectIsChild`.
- **`app/src/navigation/AppNavigator.tsx`** — renders the Auth stack, the Server Setup screen or the authenticated tab navigator depending on the store state.

Tokens are never stored in component state or React Query caches. On logout the React Query cache is cleared in `App.tsx` to prevent cross-account data leakage.

## Authentication flow

### Initial login (mobile app)

1. On first launch the user enters the backend URL on the **Server Setup** screen (persisted via `expo-secure-store`).
2. The user submits username/password on the **Login** screen.
3. The app calls `POST /api/v1/auth/login`.
4. The backend authenticates via `AuthenticationManager` and returns:
   ```json
   {
     "accessToken":  "eyJhbGciOi…",
     "refreshToken": "eyJhbGciOi…",
     "tokenType":    "Bearer",
     "expiresIn":    3600
   }
   ```
5. The app stores both tokens plus `Date.now() + expiresIn*1000` as expiry.

### Authenticated requests

Every API call is issued through `apiClient` (axios) which automatically:

- sets the `baseURL` to `<server>/api/v1`,
- reads the current access token from secure storage and adds `Authorization: Bearer …`.

### Automatic token refresh

On any `401`/`403` response, the client:

1. Calls `POST /api/v1/auth/refresh` with the stored refresh token.
2. On success: persists the rotated tokens and retries the original request (and any queued parallel requests).
3. On failure: clears `ACCESS_TOKEN`, `REFRESH_TOKEN`, `TOKEN_EXPIRY`, `USER_PROFILE` and lets the `authStore` move back to the login screen.

### Session persistence

- Tokens live in `expo-secure-store` (Keystore / Keychain on device) and survive app restarts.
- On startup `authStore.initialize()` reads the stored tokens; if only the refresh token is still valid, the first API call silently refreshes the pair.
- After 30 days of inactivity the refresh token expires and a new login is required.

## Configuration

### Backend

`backend/src/main/resources/application.yml` ships sensible defaults; override via environment variables or a `.env` file when using Docker:

```env
APP_SECURITY_JWT_KEYSTORE_PATH=/app/config/jwt-keys.pfx
APP_SECURITY_JWT_KEYSTORE_PASSWORD=<strong-password>
APP_SECURITY_JWT_KEY_ALIAS=jwt-key
```

The default Docker-compose setup mounts the `backend_config` named volume at `/app/config` so the keystore persists across restarts.

### Mobile app

No configuration is required — tokens are fully managed by `authStore` and `apiClient`. The server URL is set once by the user on the Server Setup screen and stored in `expo-secure-store` under `server_url`.

## Testing the API

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}'
```

Response:

```json
{
  "accessToken":  "eyJhbGciOi…",
  "refreshToken": "eyJhbGciOi…",
  "tokenType":    "Bearer",
  "expiresIn":    3600
}
```

### Refresh

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refresh-token>"}'
```

### Authenticated request

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <access-token>"
```

Browse the full API at http://localhost:8080/swagger-ui.html.

## Security considerations

1. **Token storage**: `expo-secure-store` uses the Android Keystore / iOS Keychain.
2. **HTTPS in production**: always front the backend with a reverse proxy (nginx, Traefik, …) that terminates TLS.
3. **Keystore password**: change `APP_SECURITY_JWT_KEYSTORE_PASSWORD` from the default `changeit`.
4. **Key rotation**: delete `jwt-keys.pfx` and restart the backend to force a new key pair (invalidates all existing tokens).
5. **Short-lived access tokens**: 1 hour bounds the exposure window if a token leaks.
6. **Refresh-token rotation**: every call to `/refresh` issues a new refresh token; the old one remains valid until it expires, so consider pairing rotation with server-side revocation for high-security deployments.
7. **Logout**: tokens are removed client-side. For enhanced security a denylist / revocation store can be added to `TokenService`.
8. **CORS**: configured in `SecurityConfig#corsConfigurationSource`; restrict allowed origins for production deployments.
9. **Rate limiting**: `RateLimitingFilter` throttles authentication endpoints at the Spring Security filter level.

## Benefits

1. **Persistent sessions** — users stay signed in until the refresh token expires (30 days).
2. **Stateless backend** — no HTTP session state for the API; horizontally scalable.
3. **Automatic recovery** — the axios interceptor transparently refreshes expired tokens and queues parallel requests during a refresh.
4. **Uniform authentication** — every API client (mobile app, Swagger, curl) uses the same stateless JWT flow.

## Future enhancements

- **Biometric unlock** for the mobile app before re-authentication
- **Server-side refresh-token revocation** (denylist or one-time rotation)
- **Device management** — list active sessions per user, revoke specific devices
- **Device-bound tokens** tied to an FCM registration for stronger account hygiene
