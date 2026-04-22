# Push Notifications Setup Guide

This guide explains how to set up push notifications for OpenFamilyCompass using Firebase Cloud Messaging (FCM).

## Overview

- The **Spring Boot backend** authenticates against FCM using a Google service account JSON and sends notifications when relevant events occur (task approvals, reward redemptions, behavior evaluations, …).
- The **Expo / React Native app** registers its FCM device token via `expo-notifications` and forwards it to the backend through the `/api/v1/notifications/device` endpoint.

If the backend does not find a Firebase service account file on startup, push notifications are silently disabled — the rest of the app keeps working.

## Prerequisites

- A Google account
- Access to the [Firebase Console](https://console.firebase.google.com/)
- EAS CLI or a local Android build environment (for the mobile app)

## Step 1: Create a Firebase project

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** (or select an existing one) and follow the wizard.

## Step 2: Register the Android app in Firebase

1. In your Firebase project, click **Add app** → Android.
2. Use the package name configured in `app/app.json`:
   ```
   org.openfamilycompass.android
   ```
3. Download `google-services.json`.

### Delivering `google-services.json` to the build

Depending on how you build the Android app, place the file as follows:

**Local Gradle build** (`app/android/app/gradlew bundleRelease` etc.):

- Copy the file to `app/android/app/google-services.json`.

**EAS Cloud build** (`eas build --platform android …`):

- Upload the file as an EAS secret and reference it via `GOOGLE_SERVICES_JSON`. The helper script `app/scripts/copy-google-services.js` (registered as `eas-build-post-install` in `package.json`) copies it to `app/android/app/google-services.json` during the build.

Never commit the real `google-services.json` to a public repository.

## Step 3: Create a server service account

1. In Firebase Console open **Project settings** → **Service accounts**.
2. Click **Generate new private key** and download the JSON file.
3. Rename it to `firebase-service-account.json`.

### Delivering the service account to the backend

The backend reads the path from the `FCM_SERVICE_ACCOUNT_PATH` property (default: `firebase-service-account.json`, relative to the process working directory).

**Local development:**

- Place the file at `backend/firebase-service-account.json` (which maps to the default path), or
- Set `FCM_SERVICE_ACCOUNT_PATH=/absolute/path/to/firebase-service-account.json`.

**Docker / docker-compose:**

- Mount the file into the container at `/app/config/firebase-service-account.json`. An example bind-mount is commented in `docker-compose.yml`:

  ```yaml
  # backend:
  #   volumes:
  #     - ./firebase-service-account.json:/app/config/firebase-service-account.json:ro
  ```

- The default `FCM_SERVICE_ACCOUNT_PATH` for the Docker image already points to `/app/config/firebase-service-account.json`.

The file is intentionally excluded from the built JAR (`<exclude>firebase-service-account.json</exclude>` in `backend/pom.xml`) and must be supplied at runtime.

**Never commit the service account JSON to version control.**

## Step 4: Verify the mobile app configuration

The Expo configuration in `app/app.json` already contains the necessary permission declarations and the `expo-notifications` plugin:

```jsonc
{
  "expo": {
    "android": {
      "package": "org.openfamilycompass.android",
      "permissions": [
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.VIBRATE"
        // …
      ]
    },
    "plugins": [
      ["expo-notifications", { "color": "#2196F3", "defaultChannel": "default" }]
    ]
  }
}
```

Runtime token registration and foreground notification handling live in:

- `app/src/hooks/usePushNotifications.ts`
- `app/App.tsx` (foreground notification handler, excluding Expo Go)

## Step 5: Run and test

1. **Backend:**

   ```bash
   cd backend
   mvn spring-boot:run
   ```

   Look for `Firebase initialized successfully` in the logs. If the service account file is missing, a warning is logged and FCM is disabled.

2. **Mobile app** (native build — push notifications are **not** available in Expo Go):

   ```bash
   cd app
   npx eas build --profile preview --platform android
   # or local build:
   cd android && ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. Log in on the device, accept the notification permission prompt, and trigger an event that produces a notification (e.g. a child redeeming a reward). The parent's device should receive a push notification within a few seconds.

## Troubleshooting

### Backend

- **`FirebaseApp with name [DEFAULT] doesn't exist`** — the service account file was not found at `FCM_SERVICE_ACCOUNT_PATH`.
- **No notifications delivered** — increase logging for `org.openfamilycompass.service.FcmService` to `DEBUG`.

### Mobile app

- **No FCM token printed** — ensure the Firebase project's package name matches `org.openfamilycompass.android`.
- **Notifications not appearing on Android 13+** — verify that the `POST_NOTIFICATIONS` permission was granted.
- **Works on Expo Go but not on production build** — Expo Go does not support FCM in SDK 53+. Use a development/preview build instead.

### Common problems

- The Firebase project must be the same on both sides (server service account + `google-services.json`).
- Restart the backend after adding or changing the service account file.
- The signing key used for the Android build must match the one registered in Firebase (SHA-1 / SHA-256 fingerprint).

## Security considerations

- `firebase-service-account.json` grants FCM send access — treat it as a secret.
- Never commit it to a public repository; the `.gitignore` already excludes it.
- Use separate Firebase projects for development, staging and production.

## Alternative: local notifications only

If you do not need server-side push, the app can still show local notifications created on-device. Disable FCM by simply not providing `firebase-service-account.json`; the backend will skip notification delivery, and the app will silently fall back to no remote notifications.
