# Security Policy

## 🔒 Security Best Practices for OpenFamilyCompass

This document outlines important security considerations when deploying OpenFamilyCompass, especially in production environments.

## ⚠️ Default Credentials

OpenFamilyCompass ships with **default credentials for development purposes only**. These MUST be changed in production environments.

### Default Credentials Included in Repository

| Component | Username/ID | Password/Secret | Location |
|-----------|-------------|-----------------|----------|
| Database | `openfamilycompass_user` | `openfamilycompass_password` | `application.yml`, `docker-compose.yml` |
| Admin User | `admin` | `admin` | `application.yml` |
| OAuth Client | `openfamilycompass-client` | `change-me-in-production` | `application.yml` |
| Android Keystore | - | **Must be configured separately** | `keystore.properties` |

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
- [ ] Consider disabling default admin creation in production (modify `DataInitializer.java`)
- [ ] Set custom admin credentials via environment variables:

```bash
export APP_ADMIN_DEFAULT_USERNAME="your_admin_username"
export APP_ADMIN_DEFAULT_PASSWORD="your_very_strong_password"
```

### 3. OAuth2 Security

- [ ] Generate a strong, random OAuth2 client secret
- [ ] Use environment variable for OAuth2 client secret
- [ ] Never commit the actual secret to version control

**Generate a secure secret:**
```bash
# Linux/macOS
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

**Set via environment variable:**
```bash
export SPRING_SECURITY_OAUTH2_CLIENT_SECRET="your_generated_secret_here"
```

### 4. Android App Security

- [ ] **CRITICAL**: Create your own release keystore (DO NOT use example keystore)
- [ ] Copy `keystore.properties.example` to `keystore.properties`
- [ ] Fill in your actual keystore details
- [ ] Ensure `keystore.properties` is NOT committed to version control (check `.gitignore`)

**Generate a new keystore:**
```bash
keytool -genkey -v -keystore my-release-key.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

### 5. Network Security

- [ ] Use HTTPS in production (configure reverse proxy like nginx)
- [ ] Update OAuth2 redirect URIs from localhost to your production domain
- [ ] Configure CORS appropriately (don't use `*` in production)
- [ ] Enable firewall rules to restrict access

### 6. Application Configuration

- [ ] Review and adjust session timeout settings
- [ ] Enable Spring Security's CSRF protection for web endpoints
- [ ] Configure secure cookie settings (`HttpOnly`, `Secure`, `SameSite`)
- [ ] Set up proper logging (but avoid logging sensitive data)
- [ ] Configure rate limiting for authentication endpoints

### 7. Docker Security

- [ ] Do not expose PostgreSQL port (5432) to the internet
- [ ] Run containers as non-root user (already configured in Dockerfile)
- [ ] Use Docker secrets instead of environment variables for sensitive data in Swarm mode
- [ ] Regularly update base images for security patches
- [ ] Use specific version tags instead of `latest`

### 8. File System Security

- [ ] Ensure upload directories have appropriate permissions
- [ ] Validate file uploads (size, type, content)
- [ ] Consider using external storage (S3, etc.) instead of local filesystem
- [ ] Regularly backup database and uploads

## 🔐 Environment Variables for Production

Create a `.env` file based on `.env.example` and configure:

```env
# Database
POSTGRES_PASSWORD=<strong-random-password>
SPRING_DATASOURCE_PASSWORD=<same-as-postgres-password>

# OAuth2
SPRING_SECURITY_OAUTH2_CLIENT_SECRET=<strong-random-secret>

# Admin
APP_ADMIN_DEFAULT_PASSWORD=<strong-initial-password>

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
