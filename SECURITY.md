# Security Policy

## 🔒 Security Best Practices for OpenFamilyCompass

This document outlines important security considerations when deploying OpenFamilyCompass, especially in production environments.

## ⚠️ Default Credentials

OpenFamilyCompass ships with **default credentials for development purposes only**. These MUST be changed in production environments.

### Default Credentials Included in Repository

| Component         | Username/ID              | Password/Secret              | Location                                          |
| ----------------- | ------------------------ | ---------------------------- | ------------------------------------------------- |
| Database          | `openfamilycompass_user` | `openfamilycompass_password` | `backend/src/main/resources/application.yml`, `docker-compose.yml` |
| Admin User        | `admin`                  | `admin`                      | `backend/src/main/resources/application.yml`      |
| JWT Keystore      | alias `jwt-key`          | `changeit`                   | `backend/src/main/resources/application.yml` (auto-generated at `jwt-keys.pfx`) |
| Android Keystore  | —                        | **Must be configured separately** | `app/android/keystore.properties`              |

## 🛡️ Production Deployment Checklist

Before deploying to production, ensure you have completed ALL of the following:

### 1. Database Security

- [ ] Change PostgreSQL password from default
- [ ] Use environment variables instead of hardcoded credentials
- [ ] Restrict database access to application network only
- [ ] Enable SSL/TLS for database connections if exposed externally

**Example using environment variables:**
```bash
export POSTGRES_PASSWORD="your_very_strong_password_here"
export SPRING_DATASOURCE_PASSWORD="your_very_strong_password_here"
```

Or using `.env` file (copy from `.env.example`):
```bash
POSTGRES_PASSWORD=your_very_strong_password_here
SPRING_DATASOURCE_PASSWORD=your_very_strong_password_here
```

### 2. Admin Account Security

- [ ] Change default admin password immediately after first login
- [ ] Use a strong password (minimum 12 characters, mixed case, numbers, symbols)
- [ ] Consider disabling default admin creation in production (modify `backend/src/main/java/org/openfamilycompass/init/DataInitializer.java`)
- [ ] Set custom admin credentials via environment variables:

```bash
export APP_ADMIN_DEFAULT_USERNAME="your_admin_username"
export APP_ADMIN_DEFAULT_PASSWORD="your_very_strong_password"
```

### 3. JWT Keystore Security

The backend signs JWT access / refresh tokens with an RSA key pair stored in a PKCS#12 keystore (`jwt-keys.pfx`). On first startup the keystore is auto-generated; it must be protected as a secret.

- [ ] Change the default keystore password from `changeit`
- [ ] Use a strong, random password (at least 32 characters)
- [ ] Persist the keystore in a secure volume (Docker volume `backend_config` in the default compose setup)
- [ ] Rotate the keystore periodically — simply delete the file and restart the backend to force a new key pair (this invalidates all existing tokens)

**Generate a secure password:**

```bash
# Linux/macOS
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

**Set via environment variables:**

```bash
export APP_SECURITY_JWT_KEYSTORE_PASSWORD="your_generated_password_here"
export APP_SECURITY_JWT_KEYSTORE_PATH="/app/config/jwt-keys.pfx"
```

### 4. Firebase / Push Notifications

- [ ] Do **not** commit `firebase-service-account.json` — the `pom.xml` already excludes it from the built JAR
- [ ] Mount the file at runtime (`FCM_SERVICE_ACCOUNT_PATH`) and keep it readable only by the backend process
- [ ] Use separate Firebase projects for development, staging and production

### 5. Android App Security

- [ ] **CRITICAL**: Create your own release keystore for the mobile app (DO NOT publish with an example keystore)
- [ ] Store keystore credentials in `app/android/keystore.properties` (or pass them via `GOOGLE_SERVICES_JSON` / EAS secrets for cloud builds)
- [ ] Ensure `app/android/keystore.properties` and `*.keystore` are in `.gitignore`
- [ ] Keep a secure backup of the keystore — losing it means you can no longer publish updates to the Play Store

**Generate a new keystore:**

```bash
keytool -genkeypair -v -keystore release.keystore -alias openfamilycompass \
  -keyalg RSA -keysize 2048 -validity 10000
```

See `app/BUILD.md` for the full local release build workflow.

### 6. Network Security

- [ ] Use HTTPS in production (configure a reverse proxy like nginx / Traefik)
- [ ] Restrict `CORS_ALLOWED_ORIGINS` to your own domains — do not use `*`
- [ ] Enable firewall rules to restrict access to PostgreSQL (port 5432) and the Metro bundler (8081)

### 7. Application Configuration

- [ ] Review JWT access-/refresh-token lifetimes in `TokenService` (1 h / 30 d by default)
- [ ] Set up proper logging (but avoid logging tokens, passwords or personal data)
- [ ] Rate limiting is enabled via `RateLimitingFilter`; review the thresholds for your environment

### 8. Docker Security

- [ ] Do not expose PostgreSQL port (5432) to the internet
- [ ] Run containers as non-root user (already configured in Dockerfile)
- [ ] Use Docker secrets instead of environment variables for sensitive data in Swarm mode
- [ ] Regularly update base images for security patches
- [ ] Use specific version tags instead of `latest`

### 9. File System Security

- [ ] Ensure upload directories have appropriate permissions
- [ ] Validate file uploads (size, type, content); the backend already enforces a 5 MB limit
- [ ] Consider using external storage (S3, etc.) instead of local filesystem
- [ ] Regularly backup the database and the `media_data` / `backend_config` volumes

## 🔐 Environment Variables for Production

Create a `.env` file based on `.env.example` and configure:

```env
# Database
POSTGRES_PASSWORD=<strong-random-password>
SPRING_DATASOURCE_PASSWORD=<same-as-postgres-password>

# Admin
APP_ADMIN_DEFAULT_PASSWORD=<strong-initial-password>

# JWT keystore (backend)
APP_SECURITY_JWT_KEYSTORE_PASSWORD=<strong-random-password>

# CORS
CORS_ALLOWED_ORIGINS=https://your-domain.example.com

# Optional: Override defaults
SERVER_PORT=8080
```

Then use with Docker Compose:
```bash
docker-compose --env-file .env up -d
```

## 🐛 Reporting Security Vulnerabilities

If you discover a security vulnerability in OpenFamilyCompass, please **DO NOT** open a public issue.

Instead, please report it privately:

1. **Email**: [Your contact email or create a security contact]
2. **GitHub Security Advisory**: Use GitHub's "Report a vulnerability" feature (if repository is public)

We will respond as quickly as possible and work with you to address the issue.

## 📚 Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [Docker Security Best Practices](https://docs.docker.com/develop/security-best-practices/)
- [PostgreSQL Security](https://www.postgresql.org/docs/current/security.html)

## 🔄 Regular Security Maintenance

- [ ] Keep dependencies up to date (use `mvn versions:display-dependency-updates`)
- [ ] Monitor for security advisories in used libraries
- [ ] Regularly review access logs for suspicious activity
- [ ] Perform periodic security audits
- [ ] Backup data regularly and test restore procedures
- [ ] Review and rotate credentials periodically

---

**Last Updated**: January 3, 2026

Remember: **Security is an ongoing process, not a one-time setup!**
