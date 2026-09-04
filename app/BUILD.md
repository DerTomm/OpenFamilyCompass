# Mobile App Build Guide

This single document covers every build workflow for the Expo / React Native mobile app of OpenFamilyCompass. It replaces the previous `QUICK_BUILD.md`, `LOCAL_BUILD_GUIDE.md`, `EXPO_GO_GUIDE.md`, `BUILD_ANDROID.md` and `ANDROID_BUILD.md`.

## Table of contents

1. [Which workflow should I use?](#which-workflow-should-i-use)
2. [Prerequisites](#prerequisites)
3. [Backend reachability from the app](#backend-reachability-from-the-app)
4. [Workflow A — Expo Go (fastest dev loop)](#workflow-a--expo-go-fastest-dev-loop)
5. [Workflow B — Expo Dev Client build](#workflow-b--expo-dev-client-build)
6. [Workflow C — EAS cloud build (APK / AAB)](#workflow-c--eas-cloud-build-apk--aab)
7. [Workflow D — Local Gradle release build](#workflow-d--local-gradle-release-build)
8. [Keystore & signing](#keystore--signing)
9. [Versioning](#versioning)
10. [Google Play Console: internal testing](#google-play-console-internal-testing)
11. [Troubleshooting](#troubleshooting)
12. [Reference: common commands](#reference-common-commands)

## Which workflow should I use?

| Goal                                             | Workflow       | Output              | Requires Android SDK locally | Signing              |
| ------------------------------------------------ | -------------- | ------------------- | ---------------------------- | -------------------- |
| Iterate on JS/UI fast on a real device           | **A — Expo Go**| Hot-reloaded JS     | no                           | —                    |
| Iterate with native modules not in Expo Go       | **B — Dev Client** | Debug APK        | optional (EAS can do it)     | debug                |
| Distribute a test build to family / testers      | **C — EAS preview** | signed APK / AAB | no (runs in Expo cloud)      | EAS-managed          |
| Publish to the Play Store from the command line  | **D — Local Gradle** | signed AAB      | **yes**                      | your own keystore    |

Most contributors only need **A**. For Play Store releases use **C** or **D**.

## Prerequisites

Install once:

- **Node.js ≥ 20** + **npm** (`node -v`, `npm -v`)
- **Java 21** (for workflow D / local native builds)
- **Android Studio** with SDK + platform tools (only for workflow D; EAS handles this in the cloud)
- A free **Expo account** at https://expo.dev (for workflow B/C)
- **EAS CLI** (for workflow B/C):

  ```bash
  npm install -g eas-cli
  # or run via npx:  npx eas-cli <command>
  ```

- **Google Play Developer account** ($25 one-time, only needed for Play Store distribution)

Install JS dependencies after cloning:

```bash
cd app
npm install
```

## Backend reachability from the app

The mobile app persists the backend URL in `expo-secure-store` (first-launch **Server Setup** screen). The compiled default in `app/src/api/config.ts` is `http://localhost:8080`, which is **only** correct when running the app on the same host (web build / emulator).

On a real phone, `localhost` points at the phone itself. Use your PC's LAN IP instead:

1. Find the IP:

   ```bash
   ipconfig      # Windows
   ip addr       # Linux/macOS
   ```

2. Enter `http://192.168.x.y:8080` on the **Server Setup** screen when the app starts for the first time. The value is persisted on the device.

3. Alternatively, set `EXPO_PUBLIC_API_URL=http://192.168.x.y:8080` before starting Expo to bake the URL into the JS bundle.

For production builds, the end user simply types in your public `https://…` URL once.

## Workflow A — Expo Go (fastest dev loop)

Great for JS-only changes (UI, screens, API calls). Push notifications are **not** available in Expo Go.

1. Install **Expo Go** from the Play Store / App Store on your phone.
2. Start the dev server:

   ```bash
   cd app
   npx expo start
   ```

3. Make sure the phone and your PC are on the same Wi-Fi, then scan the QR code with Expo Go. Code changes hot-reload automatically.

### Useful flags

```bash
npx expo start             # default
npx expo start --lan       # force LAN mode (best when Docker is involved)
npx expo start --tunnel    # go through Expo's tunnel if networks differ
npx expo start --clear     # clear Metro cache
```

### Windows Firewall

If the phone cannot connect, open port 8081:

```powershell
# Run PowerShell as administrator
netsh advfirewall firewall add rule name="Expo CLI" dir=in action=allow protocol=TCP localport=8081
```

## Workflow B — Expo Dev Client build

Needed for native modules that are not available in the stock Expo Go app (e.g. `expo-notifications` with FCM).

```bash
cd app
npx eas-cli login
npx eas-cli build --profile development --platform android
```

The resulting APK is a **development client** that embeds all native modules and connects to your Metro server for JS code. Install it once, then run `npx expo start --dev-client`.

## Workflow C — EAS cloud build (APK / AAB)

Build profiles are defined in `app/eas.json`:

| Profile          | Build type  | Distribution | Typical use                      |
| ---------------- | ----------- | ------------ | -------------------------------- |
| `development`    | APK (debug) | internal     | Dev Client (workflow B)          |
| `preview`        | APK         | internal     | Tester / sideload install        |
| `production`     | AAB         | store        | Play Store upload                |
| `production-apk` | APK         | internal     | Play-Store-signed direct install |

### Steps

```bash
cd app

# 1. Login once
npx eas-cli login

# 2. Configure credentials once per project (EAS can manage signing for you)
npx eas-cli build:configure

# 3. Build — pick the profile you need
npx eas-cli build --profile preview    --platform android
npx eas-cli build --profile production --platform android
```

A cloud build takes 10–20 minutes and prints a download URL when it finishes:

```
✅ Build successful!
📱 Download: https://expo.dev/artifacts/eas/...
```

### `google-services.json` for FCM

The helper script `app/scripts/copy-google-services.js` runs automatically as `eas-build-post-install` (see `app/package.json`). Upload your `google-services.json` as an EAS secret named `GOOGLE_SERVICES_JSON`; the script copies it to `app/android/app/google-services.json` during the build.

### Free tier limits

- EAS Free Tier: 30 builds / month — plenty for a family project.

## Workflow D — Local Gradle release build

Use this when you want to sign with your own keystore and avoid Expo cloud altogether.

### 1. Create a release keystore (once)

```bash
cd app/android/app

keytool -genkeypair -v -storetype PKCS12 \
  -keystore release.keystore \
  -alias openfamilycompass \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Answer the prompts. **Back up `release.keystore` and its passwords in at least two secure locations** — if you lose them, you can no longer publish Play Store updates for this app.

### 2. Configure `keystore.properties`

Create `app/android/keystore.properties` (already in `.gitignore`):

```properties
storePassword=YOUR-KEYSTORE-PASSWORD
keyPassword=YOUR-KEY-PASSWORD
keyAlias=openfamilycompass
storeFile=release.keystore
```

### 3. Wire signing into `android/app/build.gradle`

In `android/app/build.gradle` (above the `android { }` block), load the properties:

```gradle
def keystorePropertiesFile = rootProject.file("keystore.properties")
def keystoreProperties = new Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}
```

Inside `android { }` add:

```gradle
signingConfigs {
    release {
        if (keystorePropertiesFile.exists()) {
            storeFile     file(keystoreProperties['storeFile'])
            storePassword keystoreProperties['storePassword']
            keyAlias      keystoreProperties['keyAlias']
            keyPassword   keystoreProperties['keyPassword']
        }
    }
}

buildTypes {
    release {
        signingConfig  signingConfigs.release
        minifyEnabled  enableMinifyInReleaseBuilds
        proguardFiles  getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

If `android/local.properties` is missing, create it pointing at your Android SDK:

```properties
sdk.dir=C:\\Android\\Sdk
```

### 4. Build

```bash
cd app
npm install

# AAB — for the Play Store
cd android
./gradlew bundleRelease          # Linux/macOS
.\gradlew bundleRelease          # Windows PowerShell

# APK — for manual sideload testing
./gradlew assembleRelease
```

Outputs:

| Artifact           | Path                                                          | Size      |
| ------------------ | ------------------------------------------------------------- | --------- |
| AAB (Play Store)   | `android/app/build/outputs/bundle/release/app-release.aab`    | ~30–40 MB |
| APK (universal)    | `android/app/build/outputs/apk/release/app-release.apk`       | ~50–60 MB |

Installed footprint on the device is typically ~70–80 MB.

## Keystore & signing

- **Never** commit `release.keystore` or `keystore.properties`.
- Verify `.gitignore` already excludes them:

  ```
  android/keystore.properties
  android/app/release.keystore
  ```

- For EAS builds you can either let EAS generate/host the keystore (recommended for simplicity) or upload your own via `eas credentials`.
- Losing the keystore means Google Play will reject future updates because the signing identity changes.

## Versioning

The visible app version is maintained once in `app/package.json`. Expo, the Android
Gradle build, and the in-app version display read it from there. Every Play Store
upload must also have a new `versionCode`; that build number remains in
`app/app.json`.

```jsonc
{
  // app/package.json
  "version": "1.1.2"
}
```

```jsonc
{
  // app/app.json
  "expo": {
    "android": {
      "versionCode": 3
    }
  }
}
```

`versionCode` must be strictly higher than any previously uploaded build. Play Store error "Version already exists" means you forgot to bump it.

## Google Play Console: internal testing

1. Open https://play.google.com/console and select (or create) your app:
   - Category: Parenting / Family
   - Contact email, privacy policy URL, at least 2 screenshots (1080×1920)
2. **Testing → Internal testing → Create new release**.
3. Upload the AAB from workflow C or D.
4. Add release notes and click **Save → Review release → Start rollout**.
5. **Testers** tab: create an email list and add your testers.
6. After ~1–2 hours share the opt-in link. Testers accept it once; all future builds roll out via the Play Store automatically.

For a bigger audience, **Closed testing** works the same way with more tester-count headroom.

## Troubleshooting

### Expo Go cannot reach the dev server
- Phone and PC must be on the same Wi-Fi.
- Try `npx expo start --tunnel`.
- Open port 8081 in the Windows Firewall (see workflow A).

### Expo Go reports "Network Error" hitting the API
- The baked-in default `http://localhost:8080` is wrong on a phone. Enter your PC's LAN IP on the Server Setup screen, or set `EXPO_PUBLIC_API_URL` and restart Expo with `--clear`.
- Check the backend logs and the `CORS_ALLOWED_ORIGINS` setting.

### Gradle: "SDK location not found"
Create `android/local.properties` with `sdk.dir=...` as shown above.

### Gradle: "Keystore file not found"
- Ensure `android/keystore.properties` exists and points at a real `storeFile`.
- Paths in `keystore.properties` are **relative to `android/app/`**.

### Gradle: `Execution failed for task ':app:signReleaseBundle'`
- Wrong password in `keystore.properties` (case-sensitive).
- Keystore created with a different alias than configured.

### EAS: "No credentials configured"
```bash
npx eas-cli credentials
# Android → Set up new credentials  (let EAS manage them)
```

### EAS: build fails in Gradle
- Check for incompatible dependencies in `package.json`.
- Reproduce locally: `cd android && ./gradlew clean`.

### Play Console: "Version already exists"
Bump `android.versionCode` in `app/app.json`.

### App crashes right after install
- Verify the backend URL on the Server Setup screen.
- Collect logs with `adb logcat`:

  ```bash
  adb logcat | Select-String "OpenFamilyCompass"   # PowerShell
  adb logcat | grep OpenFamilyCompass              # bash
  ```

- Test with a debug build first: `npx expo run:android`.

### `Duplicate class found`
```bash
cd android
./gradlew clean
cd ..
npm install
cd android
./gradlew bundleRelease
```

## Reference: common commands

```bash
# Dependencies
npm install
npx expo install --fix

# Expo dev server
npx expo start
npx expo start --lan
npx expo start --tunnel
npx expo start --clear
npx expo start --dev-client

# EAS
npx eas-cli login
npx eas-cli build:configure
npx eas-cli build --profile development  --platform android
npx eas-cli build --profile preview      --platform android
npx eas-cli build --profile production   --platform android
npx eas-cli build:list
npx eas-cli credentials

# Gradle (from app/android/)
./gradlew clean
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew bundleRelease

# Local prebuild (regenerate native Android project from Expo config)
npx expo prebuild --platform android
```

---

If you run into a case that isn't covered here, please open an issue or extend this file via a pull request — keeping the build documentation in a single place is the whole point.
