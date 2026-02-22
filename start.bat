@echo off
REM OpenFamilyCompass - Quick Start Script (Windows)

echo.
echo 🚀 OpenFamilyCompass - Docker Deployment
echo =========================================
echo.

REM Check if .env exists
if not exist .env (
    echo ⚠️  .env Datei nicht gefunden!
    echo 📝 Kopiere .env.example zu .env...
    copy .env.example .env
    echo.
    echo ✏️  Bitte bearbeite die .env Datei und setze sichere Passwoerter:
    echo    - POSTGRES_PASSWORD
    echo    - APP_ADMIN_DEFAULT_PASSWORD
    echo.
    echo Dann fuehre dieses Script erneut aus.
    pause
    exit /b 1
)

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker laeuft nicht!
    echo Bitte starte Docker Desktop und versuche es erneut.
    pause
    exit /b 1
)

REM Stop existing containers
echo 🔄 Stoppe eventuell laufende Container...
docker-compose down

REM Start containers
echo 🐳 Starte Docker Container...
docker-compose up -d --build

echo.
echo ⏳ Warte auf Services...
timeout /t 10 /nobreak >nul

echo.
echo 🏥 Ueberpruefe Service-Status...
timeout /t 5 /nobreak >nul

echo.
echo 🎉 Deployment gestartet!
echo.
echo 📱 Zugriff:
echo    Frontend:  http://localhost:3000
echo    Backend:   http://localhost:8080
echo    API Docs:  http://localhost:8080/swagger-ui.html
echo.
echo 👤 Standard-Login:
echo    Username: admin
echo    Passwort: (siehe .env -^> APP_ADMIN_DEFAULT_PASSWORD)
echo.
echo 📊 Logs anzeigen:
echo    docker-compose logs -f
echo.
echo 🛑 Stoppen:
echo    docker-compose down
echo.
pause
