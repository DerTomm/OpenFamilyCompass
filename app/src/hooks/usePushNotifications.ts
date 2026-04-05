import AsyncStorage from '@react-native-async-storage/async-storage';
import Constants, { ExecutionEnvironment } from 'expo-constants';
import * as Crypto from 'expo-crypto';
import { useEffect } from 'react';
import { Platform } from 'react-native';
import { notificationsApi } from '../api/services';

// Native push notifications have not been available in Expo Go since SDK 53.
// The module must not be loaded there – hence no static import.
const isExpoGo = Constants.executionEnvironment === ExecutionEnvironment.StoreClient;

const DEVICE_ID_KEY = 'fcm_device_id';

/** Returns a stable device ID (UUID), independent of the FCM token. */
const getOrCreateDeviceId = async (): Promise<string> => {
    let deviceId = await AsyncStorage.getItem(DEVICE_ID_KEY);
    if (!deviceId) {
        deviceId = Crypto.randomUUID();
        await AsyncStorage.setItem(DEVICE_ID_KEY, deviceId);
    }
    return deviceId;
};

/**
 * Registers the device with Firebase Cloud Messaging (FCM) and
 * sends the token to the backend as soon as the user is logged in.
 * Works only in Development/Production builds, not in Expo Go.
 */
export const usePushNotifications = (isAuthenticated: boolean) => {
    useEffect(() => {
        if (!isAuthenticated || Platform.OS === 'web' || isExpoGo) return;

        // Dynamic import – only reached outside of Expo Go
        // eslint-disable-next-line @typescript-eslint/no-var-requires
        const Notifications = require('expo-notifications');
        let subscription: { remove: () => void } | null = null;

        const registerDevice = async () => {
            try {
                const { status } = await Notifications.requestPermissionsAsync();
                if (status !== 'granted') {
                    console.log('[FCM] Push-Benachrichtigungen abgelehnt');
                    return;
                }

                const deviceId = await getOrCreateDeviceId();
                const tokenData = await Notifications.getDevicePushTokenAsync();
                const fcmToken = tokenData.data as string;

                await notificationsApi.registerDevice(fcmToken, Platform.OS as 'android' | 'ios', deviceId);
                console.log('[FCM] Gerät erfolgreich registriert');
            } catch (error) {
                console.error('[FCM] Fehler bei der Geräteregistrierung:', error);
            }
        };

        registerDevice();

        subscription = Notifications.addPushTokenListener(async (newToken: { data: string }) => {
            try {
                const deviceId = await getOrCreateDeviceId();
                await notificationsApi.registerDevice(
                    newToken.data,
                    Platform.OS as 'android' | 'ios',
                    deviceId,
                );
                console.log('[FCM] Aktualisierter Token registriert');
            } catch (error) {
                console.error('[FCM] Fehler beim Aktualisieren des Tokens:', error);
            }
        });

        return () => {
            subscription?.remove();
        };
    }, [isAuthenticated]);
};
