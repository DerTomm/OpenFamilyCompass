# EAS Build Guide for OpenFamilyCompass Android App

## Prerequisites
✅ EAS CLI installed: `npm install -g eas-cli`
✅ eas.json configuration created
✅ Android configuration updated in app.json

## Build Profiles

### 1. Development Build (for testing)
```bash
cd app
eas build --profile development --platform android
```
- Creates debug APK
- Includes development tools
- For internal testing

### 2. Preview Build (pre-release)
```bash
eas build --profile preview --platform android
```
- Creates APK without debug tools
- For beta testers
- Internal distribution

### 3. Production Build (release)
```bash
eas build --profile production --platform android
```
- Creates production APK
- For Google Play Store or direct distribution

## Getting Started

### 1. EAS Login
```bash
eas login
```
You need an Expo account (free at https://expo.dev)

### 2. Configure project
```bash
eas build:configure
```

### 3. Start first build
```bash
eas build --profile preview --platform android
```

## Local Build (without EAS Cloud)

If you want to build locally without Expo Cloud:

```bash
# Prebuild - generates native Android project
npx expo prebuild --platform android

# Open with Android Studio or:
cd android
./gradlew assembleDebug

# APK can then be found at:
# android/app/build/outputs/apk/debug/app-debug.apk
```

## API Configuration for Production

**Important:** Adjust the backend URL for production in `app/src/api/config.ts`:

```typescript
export const getApiBaseUrl = async (): Promise<string> => {
  if (Platform.OS === 'web') {
    return 'http://localhost:8080';
  }
  // For Android app: use your real backend URL
  return 'https://your-domain.com'; // <-- ADJUST HERE
};
```

## Signing the app (for production)

For Google Play Store you need a keystore:

```bash
# EAS can automatically create a keystore
eas build --profile production --platform android

# Or manually:
keytool -genkeypair -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
```

## Next Steps

1. ✅ Create EAS account
2. ✅ Create first preview build
3. ⏳ Test app on device
4. ⏳ Production build for Play Store
