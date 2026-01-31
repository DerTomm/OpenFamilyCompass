# EAS Build Guide für OpenFamilyCompass Android App

## Voraussetzungen
✅ EAS CLI installiert: `npm install -g eas-cli`
✅ eas.json Konfiguration erstellt
✅ Android-Konfiguration in app.json aktualisiert

## Build-Profile

### 1. Development Build (zum Testen)
```bash
cd app
eas build --profile development --platform android
```
- Erstellt Debug APK
- Beinhaltet Development Tools
- Für internes Testen

### 2. Preview Build (Pre-Release)
```bash
eas build --profile preview --platform android
```
- Erstellt APK ohne Debug-Tools
- Für Beta-Tester
- Internal Distribution

### 3. Production Build (Release)
```bash
eas build --profile production --platform android
```
- Erstellt Production APK
- Für Google Play Store oder direkte Distribution

## Erste Schritte

### 1. EAS Login
```bash
eas login
```
Du benötigst einen Expo Account (kostenlos bei https://expo.dev)

### 2. Projekt konfigurieren
```bash
eas build:configure
```

### 3. Ersten Build starten
```bash
eas build --profile preview --platform android
```

## Lokaler Build (ohne EAS Cloud)

Falls Du lokal ohne Expo Cloud bauen möchtest:

```bash
# Prebuild - generiert native Android Projekt
npx expo prebuild --platform android

# Mit Android Studio öffnen oder:
cd android
./gradlew assembleDebug

# APK findet sich dann in:
# android/app/build/outputs/apk/debug/app-debug.apk
```

## API Konfiguration für Production

**Wichtig:** In `app/src/api/config.ts` die Backend-URL für Production anpassen:

```typescript
export const getApiBaseUrl = async (): Promise<string> => {
  if (Platform.OS === 'web') {
    return 'http://localhost:8080';
  }
  // Für Android App: Nutze deine echte Backend-URL
  return 'https://deine-domain.de'; // <-- HIER ANPASSEN
};
```

## App signieren (für Production)

Für Google Play Store benötigst Du einen Keystore:

```bash
# EAS kann automatisch einen Keystore erstellen
eas build --profile production --platform android

# Oder manuell:
keytool -genkeypair -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
```

## Nächste Schritte

1. ✅ EAS Account erstellen
2. ✅ Ersten Preview Build erstellen
3. ⏳ App auf Gerät testen
4. ⏳ Production Build für Play Store
