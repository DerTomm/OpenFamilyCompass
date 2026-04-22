# OpenFamilyCompass - Deployment Guide

## 🐳 Docker Deployment (Recommended)

This guide describes the deployment of OpenFamilyCompass using Docker Compose.

### Prerequisites

- Docker (>= 20.10)
- Docker Compose (>= 2.0)
- At least 2 GB free RAM
- At least 5 GB free disk space

### Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/dertomm/OpenFamilyCompass.git
   cd OpenFamilyCompass
   ```

2. **Configure environment variables**
   ```bash
   cp .env.example .env
   ```
   
   Edit the `.env` file and set secure passwords:
   ```env
   POSTGRES_PASSWORD=your_secure_db_password
   APP_ADMIN_DEFAULT_PASSWORD=your_admin_password
   ```

3. **Start Docker containers**
   ```bash
   docker-compose up -d
   ```

4. **Access the application**
   - **Expo Go App** (Android/iOS): 
     - Install [Expo Go](https://expo.dev/go) on your device
     - Scan the QR code in the logs: `docker-compose logs frontend`
     - Or connect manually with: `exp://localhost:8081`
   - **Backend API**: http://localhost:8080
   - **API Documentation**: http://localhost:8080/swagger-ui.html

5. **Default login**
   - **Username**: admin
   - **Password**: The `APP_ADMIN_DEFAULT_PASSWORD` set in `.env`

### Services

The Docker Compose configuration starts three services:

| Service    | Port          | Description                                                  |
| ---------- | ------------- | ------------------------------------------------------------ |
| `postgres` | 5432          | PostgreSQL 16 database                                       |
| `backend`  | 8080          | Spring Boot REST API + Swagger UI                            |
| `frontend` | 8081          | Expo Metro bundler (for Expo Go)                             |
| `frontend` | 19000 / 19001 | Expo dev server (legacy / current)                           |

### Configuration

#### Change ports

Change the ports in the `.env` file:

```env
POSTGRES_PORT=5432
BACKEND_PORT=8080
METRO_PORT=8081
EXPO_PORT=19000
EXPO_PORT2=19001
```

#### Backend API URL for mobile app

If the backend is running on a different server, adjust the URL in `.env`:

```env
EXPO_PUBLIC_API_URL=http://your-server-ip:8080
```

**Important**: After changes, the frontend container must be restarted:
```bash
docker-compose restart frontend
```

#### Expo Dev Server for remote access

If you want to access from another device (e.g. a smartphone on the same network):

```env
EXPO_HOST=192.168.1.100  # Your server IP
```

Then:
```bash
docker-compose up -d --build frontend
```

### Docker Commands

#### Check status
```bash
docker-compose ps
```

#### Show logs
```bash
# All services
docker-compose logs -f

# Backend only
docker-compose logs -f backend

# Frontend only
docker-compose logs -f frontend
```

#### Restart services
```bash
docker-compose restart
```

#### Stop services
```bash
docker-compose stop
```

#### Stop and remove services
```bash
docker-compose down
```

#### Also delete database
```bash
docker-compose down -v
```

#### Rebuild containers
```bash
docker-compose up -d --build
```

### Database

#### Create database backup
```bash
docker exec ofc-postgres pg_dump -U openfamilycompass_user openfamilycompass_db > backup.sql
```

#### Restore database backup
```bash
cat backup.sql | docker exec -i ofc-postgres psql -U openfamilycompass_user -d openfamilycompass_db
```

### Production Deployment

#### Security recommendations

1. **Use secure passwords**
   ```bash
   # Generate random passwords
   openssl rand -base64 32
   ```

2. **Enable HTTPS** (with reverse proxy)
   - Nginx or Traefik as reverse proxy
   - Let's Encrypt for SSL certificates

3. **Configure firewall**
   - Open only ports 80/443
   - Close internal ports (5432, 8080)

4. **Regular backups**
   - Set up automated database backups
   - Back up the media directory (`media_data` volume)

#### Nginx Reverse Proxy Example

```nginx
server {
    listen 80;
    server_name example.com;

    # Backend: REST API + Swagger UI
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

The mobile app is distributed via the Play Store / EAS builds and does not need to be served through the reverse proxy. The Expo Metro bundler (port 8081) is only intended for local development with Expo Go.

### Updates

#### Application Update

1. Update code
   ```bash
   git pull
   ```

2. Rebuild containers
   ```bash
   docker-compose up -d --build
   ```

3. Database migrations run automatically (Flyway)

### Troubleshooting

#### Frontend shows "Network Error"

- Check `EXPO_PUBLIC_API_URL` in `.env`
- Check the server URL stored in the app (Server Setup screen on first launch)
- Backend must be reachable from the device (use the host IP, not `localhost`, when testing on a real phone)
- Check `CORS_ALLOWED_ORIGINS` in `.env` / the backend configuration

#### Backend does not start

```bash
# Check logs
docker-compose logs backend

# Is the database reachable?
docker-compose exec backend ping postgres
```

#### Database connection error

```bash
# Check Postgres status
docker-compose exec postgres pg_isready -U openfamilycompass_user

# Check passwords in .env
```

#### Port already in use

```bash
# Find port in use
netstat -tuln | grep 8080

# Set a different port in .env
BACKEND_PORT=8081
```

### Monitoring

#### Healthchecks

All services have healthchecks configured:

```bash
# Postgres
docker-compose exec postgres pg_isready -U openfamilycompass_user

# Backend (Spring Boot Actuator)
curl http://localhost:8080/actuator/health

# Frontend (Expo Metro bundler)
curl http://localhost:8081/status
```

### Resources

- **GitHub**: https://github.com/dertomm/OpenFamilyCompass
- **Documentation**: See [README.md](README.md)
- **Issues**: https://github.com/dertomm/OpenFamilyCompass/issues
