# OpenFamilyCompass - Android App

## Überblick

Dies ist eine Android-App, die als WebView-Wrapper für die OpenFamilyCompass Web-Anwendung dient.

## Features

- **WebView-basiert**: Zeigt die Web-Anwendung im nativen Browser-Komponenten an
- **Session-Management**: Speichert Cookies und Login-Status automatisch
- **Offline-fähig**: Behält die Session für 30 Tage
- **Server-Konfiguration**: Flexible Eingabe der Server-URL in den Einstellungen
- **Pull-to-Refresh**: Aktualisierung der Seite durch Herunterziehen

## Voraussetzungen

- Android 7.0 (API Level 24) oder höher
- Zugriff auf einen laufenden Kids Chores Server

## Installation

1. **APK Erstellen**:
   ```bash
   cd android-app
   ./gradlew assembleRelease
   ```

2. **APK Installieren**:
   - Die APK-Datei befindet sich unter: `app/build/outputs/apk/release/app-release-unsigned.apk`
   - Übertrage die APK auf dein Android-Gerät
   - Aktiviere "Unbekannte Quellen" in den Android-Einstellungen
   - Installiere die APK

## Konfiguration

### Server-URL einstellen

1. Öffne die App
2. Tippe auf das Menü (⋮) und wähle "Einstellungen"
3. Gib die Server-URL ein, z.B.:
   - `http://192.168.178.100:8080` (lokales Netzwerk)
   - `https://meinserver.de` (Internet)
4. Tippe auf "Speichern"

### Erste Anmeldung

1. Nach dem Speichern der Server-URL wird die Login-Seite geladen
2. Melde dich mit deinem Benutzernamen und PIN an
3. Die App speichert deine Session automatisch
4. Beim nächsten App-Start bist du automatisch angemeldet

## Verwendung

### Navigation

- **Zurück-Taste**: Navigiert durch die Browser-Historie
- **Menü → Aktualisieren**: Lädt die aktuelle Seite neu
- **Menü → Einstellungen**: Öffnet die Server-Konfiguration
- **Menü → Abmelden**: Löscht die gespeicherte Session und meldet dich ab

### Session-Verwaltung

- Die App speichert deine Session-Cookies automatisch
- Sessions bleiben 30 Tage gültig
- Bei jedem App-Start wird geprüft, ob die Session noch gültig ist
- Bei Bedarf wirst du automatisch zur Login-Seite weitergeleitet

## Technische Details

### Architektur

- **Programmiersprache**: Kotlin
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **WebView**: Verwendet Android System WebView
- **Speicher**: SharedPreferences für Session-Daten

### Komponenten

1. **MainActivity**: Hauptaktivität mit WebView
2. **SettingsActivity**: Konfiguration der Server-URL
3. **SessionManager**: Verwaltung von Cookies und Session-Status

### Sicherheit

- HTTPS-Unterstützung für sichere Verbindungen
- Cookie-basierte Authentifikation
- Lokale Speicherung der Session-Daten (verschlüsselt durch Android)

## Fehlerbehebung

### App zeigt "Server konfigurieren"

**Problem**: Keine Server-URL hinterlegt  
**Lösung**: Gehe zu den Einstellungen und gib die Server-URL ein

### Session läuft immer ab

**Problem**: Session wird nicht korrekt gespeichert  
**Lösung**: 
1. Überprüfe, ob die Server-URL korrekt ist
2. Melde dich ab und neu an
3. Stelle sicher, dass Cookies im Browser aktiviert sind

### WebView zeigt Fehlerseite

**Problem**: Server nicht erreichbar  
**Lösung**: 
1. Überprüfe die Server-URL in den Einstellungen
2. Stelle sicher, dass der Server läuft
3. Prüfe die Netzwerkverbindung

## Entwicklung

### Projekt-Struktur

```
android-app/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/kidschores/app/
│   │       │   ├── MainActivity.kt
│   │       │   ├── SettingsActivity.kt
│   │       │   └── SessionManager.kt
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   ├── menu/
│   │       │   └── values/
│   │       └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/
├── build.gradle
└── settings.gradle
```

### Build-Varianten

- **Debug**: Entwicklungsversion mit Debugging-Informationen
- **Release**: Produktionsversion mit ProGuard-Optimierung

### Testing

```bash
# Debug-Build erstellen und installieren
./gradlew installDebug

# Unit-Tests ausführen
./gradlew test

# Instrumentierte Tests ausführen
./gradlew connectedAndroidTest
```

## License

Dieses Projekt ist für den privaten Gebrauch bestimmt.
