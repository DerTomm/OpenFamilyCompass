# OpenFamilyCompass - Deployment Guide

## 🐳 Docker Deployment (Empfohlen)

Diese Anleitung beschreibt das Deployment von OpenFamilyCompass mit Docker Compose.

### Voraussetzungen

- Docker (>= 20.10)
- Docker Compose (>= 2.0)
- Mindestens 2GB freier RAM
- Mindestens 5GB freier Speicherplatz

### Schnellstart

1. **Repository klonen**
   ```bash
   git clone https://github.com/dertomm/OpenFamilyCompass.git
   cd OpenFamilyCompass
   ```

2. **Umgebungsvariablen konfigurieren**
   ```bash
   cp .env.example .env
   ```
   
   Bearbeite die `.env` Datei und setze sichere Passwörter:
   ```env
   POSTGRES_PASSWORD=dein_sicheres_db_passwort
   APP_ADMIN_DEFAULT_PASSWORD=dein_admin_passwort
   ```

3. **Docker Container starten**
   ```bash
   docker-compose up -d
   ```

4. **Zugriff auf die Anwendung**
   - **Frontend**: http://localhost:3000
   - **Backend API**: http://localhost:8080
   - **API Dokumentation**: http://localhost:8080/swagger-ui.html

5. **Standard-Login**
   - **Username**: admin
   - **Passwort**: Das in `.env` gesetzte `APP_ADMIN_DEFAULT_PASSWORD`

### Services

Die Docker Compose Konfiguration startet 3 Services:

| Service | Port | Beschreibung |
|---------|------|--------------|
| `postgres` | 5432 | PostgreSQL 16 Datenbank |
| `backend` | 8080 | Spring Boot REST API |
| `frontend` | 3000 | React Native Web (Nginx) |

### Konfiguration

#### Ports ändern

Ändere die Ports in der `.env` Datei:

```env
POSTGRES_PORT=5432
BACKEND_PORT=8080
FRONTEND_PORT=3000
```

#### Backend API URL

Wenn das Backend auf einem anderen Server läuft, passe die URL an:

```env
EXPO_PUBLIC_API_URL=http://deine-server-ip:8080
```

**Wichtig**: Nach Änderung muss das Frontend neu gebaut werden:
```bash
docker-compose up -d --build frontend
```

### Docker Befehle

#### Status überprüfen
```bash
docker-compose ps
```

#### Logs anzeigen
```bash
# Alle Services
docker-compose logs -f

# Nur Backend
docker-compose logs -f backend

# Nur Frontend
docker-compose logs -f frontend
```

#### Services neu starten
```bash
docker-compose restart
```

#### Services stoppen
```bash
docker-compose stop
```

#### Services stoppen und entfernen
```bash
docker-compose down
```

#### Datenbank auch löschen
```bash
docker-compose down -v
```

#### Container neu bauen
```bash
docker-compose up -d --build
```

### Datenbank

#### Datenbank Backup erstellen
```bash
docker exec ofc-postgres pg_dump -U openfamilycompass_user openfamilycompass_db > backup.sql
```

#### Datenbank Backup wiederherstellen
```bash
cat backup.sql | docker exec -i ofc-postgres psql -U openfamilycompass_user -d openfamilycompass_db
```

### Produktions-Deployment

#### Sicherheitsempfehlungen

1. **Sichere Passwörter verwenden**
   ```bash
   # Zufällige Passwörter generieren
   openssl rand -base64 32
   ```

2. **HTTPS aktivieren** (mit Reverse Proxy)
   - Nginx oder Traefik als Reverse Proxy
   - Let's Encrypt für SSL-Zertifikate

3. **Firewall konfigurieren**
   - Nur Port 80/443 öffnen
   - Interne Ports (5432, 8080) schließen

4. **Regelmäßige Backups**
   - Automatisierte Datenbank-Backups einrichten
   - Media-Verzeichnis sichern (`media_data` Volume)

#### Nginx Reverse Proxy Beispiel

```nginx
server {
    listen 80;
    server_name example.com;
    
    location / {
        proxy_pass http://localhost:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Updates

#### Application Update

1. Code aktualisieren
   ```bash
   git pull
   ```

2. Container neu bauen
   ```bash
   docker-compose up -d --build
   ```

3. Datenbank-Migrationen laufen automatisch (Flyway)

### Troubleshooting

#### Frontend zeigt "Network Error"

- Prüfe `EXPO_PUBLIC_API_URL` in `.env`
- Backend muss erreichbar sein
- CORS-Einstellungen prüfen

#### Backend startet nicht

```bash
# Logs prüfen
docker-compose logs backend

# Datenbank erreichbar?
docker-compose exec backend ping postgres
```

#### Datenbank-Connection-Fehler

```bash
# Postgres Status prüfen
docker-compose exec postgres pg_isready -U openfamilycompass_user

# Passwörter in .env prüfen
```

#### Port bereits belegt

```bash
# Belegten Port finden
netstat -tuln | grep 8080

# Anderen Port in .env setzen
BACKEND_PORT=8081
```

### Monitoring

#### Healthchecks

Alle Services haben Healthchecks konfiguriert:

```bash
# Postgres
curl http://localhost:5432

# Backend
curl http://localhost:8080/actuator/health

# Frontend
curl http://localhost:3000/health
```

### Ressourcen

- **GitHub**: https://github.com/dertomm/OpenFamilyCompass
- **Dokumentation**: Siehe README.md
- **Issues**: https://github.com/dertomm/OpenFamilyCompass/issues
