# Push Notifications Setup Guide

This guide explains how to set up push notifications for the OpenFamilyCompass application using Firebase Cloud Messaging (FCM).

## Overview

The application uses FCM to send push notifications to Android devices when certain events occur (e.g., reward redemptions, task completions). The setup requires two JSON configuration files from Firebase.

## Prerequisites

- A Google account
- Android Studio
- Firebase Console access

## Step 1: Create a Firebase Project

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Click "Create a project" (or select an existing project)
3. Enter your project name (e.g., "OpenFamilyCompass")
4. Follow the setup wizard to create the project

## Step 2: Set Up Android App in Firebase

1. In your Firebase project, click "Add app" and select Android
2. Enter your Android package name: `org.openfamilycompass.android`
3. Download the `google-services.json` file
4. Place the file in: `android-app/app/google-services.json`

## Step 3: Set Up Server Authentication

1. In Firebase Console, go to Project Settings > Service Accounts
2. Click "Generate new private key"
3. Download the JSON file (this is your service account key)
4. Rename the file to `firebase-service-account.json`
5. Place the file in: `src/main/resources/firebase-service-account.json`

**Security Note**: Never commit this file to version control. Add it to `.gitignore`.

## Step 4: Configure Android App

The Android app is already configured to use FCM. Ensure the following:

- `google-services.json` is in `android-app/app/`
- The app has notification permissions (requested at runtime for Android 13+)
- Firebase dependencies are in `android-app/app/build.gradle`

## Step 5: Configure Backend

The backend is already configured to use FCM. Ensure:

- `firebase-service-account.json` is in `src/main/resources/`
- Firebase Admin SDK is in `pom.xml`
- Logging level is set to DEBUG for troubleshooting (optional):
  ```yaml
  logging:
    level:
      org.openfamilycompass: DEBUG
  ```

## Step 6: Test the Setup

1. Start the backend server: `mvn spring-boot:run`
2. Check logs for: "Firebase initialized successfully"
3. Build and install the Android app: `cd android-app && ./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk`
4. Log in to the app and create a notification (e.g., redeem a reward)
5. The app should receive a push notification

## Troubleshooting

### Backend Issues
- **"FirebaseApp with name [DEFAULT] doesn't exist"**: Check if `firebase-service-account.json` is present and valid
- **No push notifications sent**: Check FCM service logs for errors

### Android Issues
- **No notifications received**: Ensure notification permissions are granted in Android settings
- **FCM token not sent**: Check app logs for FCM token retrieval

### Common Problems
- Ensure the Firebase project matches between Android and server configs
- Verify package name in Firebase matches `org.openfamilycompass.android`
- Restart server after adding `firebase-service-account.json`

## Security Considerations

- The `firebase-service-account.json` contains sensitive credentials
- Never expose it in public repositories
- Use environment-specific service accounts for production

## Alternative: Local Notifications Only

If FCM setup is not desired, the app can show local notifications without server push. Contact the development team for implementation details.