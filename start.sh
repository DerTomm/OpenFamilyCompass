#!/bin/bash
# OpenFamilyCompass - Quick Start Script

set -e

echo "🚀 OpenFamilyCompass - Docker Deployment"
echo "========================================="
echo ""

# Check if .env exists
if [ ! -f .env ]; then
    echo "⚠️  .env Datei nicht gefunden!"
    echo "📝 Kopiere .env.example zu .env..."
    cp .env.example .env
    echo ""
    echo "✏️  Bitte bearbeite die .env Datei und setze sichere Passwörter:"
    echo "   - POSTGRES_PASSWORD"
    echo "   - APP_ADMIN_DEFAULT_PASSWORD"
    echo ""
    echo "Dann führe dieses Script erneut aus."
    exit 1
fi

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker läuft nicht!"
    echo "Bitte starte Docker Desktop und versuche es erneut."
    exit 1
fi

# Check if containers are already running
if docker-compose ps | grep -q "Up"; then
    echo "🔄 Container laufen bereits. Stoppe sie erst..."
    docker-compose down
fi

# Start containers
echo "🐳 Starte Docker Container..."
docker-compose up -d --build

echo ""
echo "⏳ Warte auf Services..."
sleep 10

# Check health
echo ""
echo "🏥 Überprüfe Service-Status..."

# Check Postgres
if docker-compose exec -T postgres pg_isready -U openfamilycompass_user > /dev/null 2>&1; then
    echo "✅ Postgres: Bereit"
else
    echo "❌ Postgres: Fehler"
fi

# Wait for backend
echo "⏳ Warte auf Backend (kann bis zu 90 Sekunden dauern)..."
for i in {1..30}; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✅ Backend: Bereit"
        break
    fi
    sleep 3
done

# Check frontend
if curl -sf http://localhost:3000/health > /dev/null 2>&1; then
    echo "✅ Frontend: Bereit"
else
    echo "⚠️  Frontend: Startet noch..."
fi

echo ""
echo "🎉 Deployment abgeschlossen!"
echo ""
echo "📱 Zugriff:"
echo "   Expo Go:    exp://localhost:8081"
echo "   Metro:      http://localhost:8081"
echo "   Backend:    http://localhost:8080"
echo "   API Docs:   http://localhost:8080/swagger-ui.html"
echo ""
echo "📲 Expo Go Setup:"
echo "   1. Installiere Expo Go auf Android/iOS"
echo "   2. QR-Code: docker-compose logs frontend"
echo "   3. Oder verbinde manuell: exp://localhost:8081"
echo ""
echo "👤 Standard-Login:"
echo "   Username: admin"
echo "   Passwort: (siehe .env -> APP_ADMIN_DEFAULT_PASSWORD)"
echo ""
echo "📊 Logs anzeigen:"
echo "   docker-compose logs -f"
echo ""
echo "🛑 Stoppen:"
echo "   docker-compose down"
