# OpenFamilyCompass

**Guiding children's behavior and family routines — together.**

An open-source family management application for organizing chores, behaviors and rewards.
The project is community-driven, in an early stage, and feedback, ideas and contributions are highly appreciated.

## Overview

OpenFamilyCompass helps parents organize daily chores, track positive and negative behaviors, and motivate children through a point-based reward shop.

### Key Features

- Multi-user system with three roles: **Admin**, **Parent**, **Child**
- Task management with one-time and recurring tasks (daily, weekly, monthly)
- Point system with flexible point allocation by parents
- Reward shop with parent approval workflow
- Habit / behavior tracking (positive and negative)
- Transaction history for full accountability
- Avatar system (predefined icons + custom upload)
- Push notifications via Firebase Cloud Messaging
- Internationalization: English + German (i18n/i18next)

## Architecture

OpenFamilyCompass is split into two independent applications in a single monorepo:

| Module       | Path        | Stack                                                               |
| ------------ | ----------- | ------------------------------------------------------------------- |
| Backend      | `/backend`  | Java 21, Spring Boot 3.5, Spring Security (JWT), JPA, Flyway, Maven |
| Mobile App   | `/app`      | React Native 0.83, Expo SDK 55, React Navigation, React Query       |

The backend exposes a REST API under `/api/v1/**` that the Expo app consumes. A Swagger UI is available at `/swagger-ui.html`.

### Repository Layout

```
OpenFamilyCompass/
├── backend/                     # Spring Boot REST server
│   ├── src/main/java/org/openfamilycompass/
│   │   ├── api/v1/              # REST controllers (JSON, for the mobile app)
│   │   ├── config/              # Security, JWT, FCM, OpenAPI, Web
│   │   ├── security/            # TokenService, RateLimitingFilter
│   │   ├── service/             # Business logic
│   │   ├── repository/          # Spring Data JPA
│   │   ├── model/               # JPA entities
│   │   ├── dto/                 # API DTOs
│   │   └── OpenFamilyCompassApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── db/migration/        # Flyway SQL migrations (V1 … V16)
│   │   ├── messages*.properties # i18n message bundles
│   │   └── static/              # Served static assets
│   ├── pom.xml
│   └── Dockerfile
├── app/                         # Expo / React Native client
│   ├── src/
│   │   ├── api/                 # axios client, API services, config
│   │   ├── navigation/          # React Navigation (tabs + stacks)
│   │   ├── screens/             # auth, admin, parent, child, tasks, shop, …
│   │   ├── components/          # Reusable UI
│   │   ├── hooks/               # e.g. usePushNotifications
│   │   ├── store/               # Zustand stores (authStore)
│   │   ├── theme/               # Light/Dark theme context
│   │   ├── i18n/                # i18next config + translations
│   │   └── utils/
│   ├── android/                 # Native Android project (Gradle)
│   ├── app.json                 # Expo configuration
│   ├── eas.json                 # EAS Build profiles
│   ├── package.json
│   └── Dockerfile               # Optional: dev container for Expo Metro
├── docker-compose.yml           # Postgres + Backend + (optional) Expo
├── .env.example
└── start.sh / start.bat         # Convenience start scripts
```

## Technology Stack

### Backend

- **Java 21** (LTS), **Maven 3.6+**
- **Spring Boot 3.5** (Web, Data JPA, Validation, Actuator, DevTools)
- **Spring Security** + **OAuth2 Resource Server** (JWT validation)
- **JWT**: RSA-signed (RS256) with an auto-generated PKCS#12 keystore (`jwt-keys.pfx`), persisted in a Docker volume
- **PostgreSQL 16** (runtime) + **H2** (tests)
- **Flyway** for database migrations
- **Firebase Admin (google-auth-library)** for Cloud Messaging
- **springdoc-openapi** — Swagger UI at `/swagger-ui.html`
- **Lombok**, **BouncyCastle** (keystore generation)

### Mobile App

- **Expo SDK 55** / **React Native 0.83** / **React 19**
- **TypeScript 5.9**
- **React Navigation 7** (native-stack + bottom-tabs + drawer)
- **TanStack React Query 5** for server state
- **Zustand** for client state (auth, setup)
- **axios** for HTTP + JWT token refresh
- **React Native Paper** (Material Design UI)
- **i18next / react-i18next** — English + German
- **expo-secure-store** for token storage (native), `localStorage` fallback on web
- **expo-notifications** + FCM for push
- **expo-image-picker** for avatar/reward uploads

## Prerequisites

- **Docker & Docker Compose** (recommended path)
- Or for local development:
  - **Java 21+**, **Maven 3.6+**
  - **Node.js 20+**, **npm**
  - **Android Studio / Xcode** (only for native builds; Expo Go works without them)

## Quick Start with Docker Compose

```bash
git clone https://github.com/dertomm/OpenFamilyCompass.git
cd openfamilycompass

# Copy the environment template and edit credentials
cp .env.example .env

# Start everything: Postgres + Backend + Expo Metro (optional)
docker-compose up -d
```

Services:

| Service    | Container       | Port(s)            | Purpose                                   |
| ---------- | --------------- | ------------------ | ----------------------------------------- |
| `postgres` | `ofc-postgres`  | 5432               | PostgreSQL 16                             |
| `backend`  | `ofc-backend`   | 8080               | Spring Boot REST API + Swagger UI         |
| `frontend` | `ofc-frontend`  | 8081, 19000, 19001 | Expo Metro bundler (for Expo Go)          |

After startup:

- REST API: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator health: http://localhost:8080/actuator/health
- Expo Metro: http://localhost:8081 (scan QR code with Expo Go)

Default admin credentials (set via `.env` → `APP_ADMIN_DEFAULT_PASSWORD`):

- Username: `admin`
- Password: the value of `APP_ADMIN_DEFAULT_PASSWORD`

Change the admin password after the first login.

## Local Development

### Backend

```bash
# Start Postgres only
docker-compose up -d postgres

cd backend
mvn clean package
mvn spring-boot:run
```

The backend is reachable at http://localhost:8080. Flyway applies database migrations automatically on every startup.

Running tests:

```bash
cd backend
mvn test
```

### Mobile App

```bash
cd app
npm install

# Expo Dev Server (scan QR with Expo Go on your phone)
npx expo start
```

On first launch the app shows a **Server Setup** screen. Enter the URL of the backend (e.g. `http://192.168.x.x:8080` if you run it on your local network).

For native builds, EAS cloud builds and Play Store distribution see the consolidated [`app/BUILD.md`](app/BUILD.md) guide.

## Authentication

The REST API is fully stateless and uses JWT bearer tokens (RS256).

- `POST /api/v1/auth/login` → `{ accessToken, refreshToken, expiresIn }`
- `POST /api/v1/auth/refresh` → new token pair
- `GET  /api/v1/auth/me` → current user profile
- `POST /api/v1/auth/change-password`

Access tokens are valid for 1 hour, refresh tokens for 30 days. The signing key pair is auto-generated at first startup, stored in `jwt-keys.pfx` and kept in a named Docker volume so it survives restarts. See [TOKEN_AUTH_README.md](TOKEN_AUTH_README.md).

## Push Notifications

Firebase Cloud Messaging is used to notify Android devices about relevant events (task approvals, reward redemptions, behavior evaluations). Setup is documented in [PUSH_NOTIFICATIONS_README.md](PUSH_NOTIFICATIONS_README.md).

If no `firebase-service-account.json` is present on the backend, push is silently disabled.

## Internationalization

- Backend (API messages): Spring `MessageSource` + `messages.properties` / `messages_de.properties`, locale derived from `Accept-Language` and the user's profile.
- Mobile app: `i18next` + `react-i18next`, language follows `expo-localization`.

Supported languages: **English** (default) and **German**. See [I18N_README.md](I18N_README.md) for how to add another language.

## User Roles & Typical Workflows

### Admin

- Manage users and roles

### Parents

- Define tasks (one-time and recurring), rewards, behaviors
- Approve/reject completed tasks
- Approve reward redemptions
- Record behaviors (positive or negative)
- Assign bonus or penalty points

### Children

- View open tasks and mark them as done
- Check their point balance and transaction history
- Redeem points in the reward shop

## Recurring Tasks

A scheduled job creates due recurring task instances:

- **Daily** tasks: every day
- **Weekly** tasks: every week on the same weekday
- **Monthly** tasks: every month on the same day

## Avatars

- Predefined icons: cat, dog, bear, lion, elephant, giraffe, panda, unicorn
- Custom uploads (images up to 5 MB), stored under `uploads/avatars/`

## CI/CD

GitHub Actions runs:

- Build & test on every push
- Docker image build & push to GHCR on release tags
- Smoke test against the running container

Creating a release:

1. Go to **Actions** → **Create Release** (or push a git tag `vX.Y.Z-…`)
2. The workflow creates the tag, builds + tests, builds the Docker image and publishes a GitHub release.

Released images: `ghcr.io/DerTomm/openfamilycompass:<version>`.

## Security

Before deploying to production, read [SECURITY.md](SECURITY.md) and in particular:

- Change the default admin password
- Change PostgreSQL credentials
- Rotate / protect `jwt-keys.pfx` and `firebase-service-account.json`
- Use HTTPS in front of the backend
- Restrict CORS (`CORS_ALLOWED_ORIGINS`) to your own domains

## Documentation Index

- [DEPLOYMENT.md](DEPLOYMENT.md) — Docker deployment, reverse proxy, backups
- [SECURITY.md](SECURITY.md) — security best practices and checklist
- [TOKEN_AUTH_README.md](TOKEN_AUTH_README.md) — JWT auth flow
- [PUSH_NOTIFICATIONS_README.md](PUSH_NOTIFICATIONS_README.md) — FCM setup
- [I18N_README.md](I18N_README.md) — Internationalization
- [`app/BUILD.md`](app/BUILD.md) — mobile app build & distribution (Expo Go, EAS, local Gradle, Play Store)

## License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPL-3.0)**. See [LICENSE](LICENSE).

The AGPL is a strong copyleft license: you are free to use, modify and distribute the software, but any modifications — including those deployed as a network service — must be made available under the same license. If you need a proprietary/commercial license, please contact us.

## Contributing

Contributions are welcome! See [AUTHORS](AUTHORS) for guidelines. This project follows the principles of transparent, security-first, community-driven development.

---

**Good luck motivating your children!**
