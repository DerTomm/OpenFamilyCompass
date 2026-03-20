import AsyncStorage from '@react-native-async-storage/async-storage';
import Constants, { ExecutionEnvironment } from 'expo-constants';
import * as Crypto from 'expo-crypto';
import { useEffect } from 'react';
import { Platform } from 'react-native';
import { notificationsApi } from '../api/services';

// In Expo Go sind native Push-Benachrichtigungen seit SDK 53 nicht verfügbar.
// Das Modul darf dort nicht geladen werden – daher kein statisches import.
const isExpoGo = Constants.executionEnvironment === ExecutionEnvironment.StoreClient;

const DEVICE_ID_KEY = 'fcm_device_id';

/** Liefert eine stabile Geräte-ID (UUID), die unabhängig vom FCM-Token ist. */
const getOrCreateDeviceId = async (): Promise<string> => {
    let deviceId = await AsyncStorage.getItem(DEVICE_ID_KEY);
    if (!deviceId) {
        deviceId = Crypto.randomUUID();
        await AsyncStorage.setItem(DEVICE_ID_KEY, deviceId);
    }
    return deviceId;
};

/**
 * Registriert das Gerät bei Firebase Cloud Messaging (FCM) und
 * sendet den Token an das Backend, sobald der Nutzer eingeloggt ist.
 * Funktioniert nur in Development/Production Builds, nicht in Expo Go.
 */
export const usePushNotifications = (isAuthenticated: boolean) => {
    useEffect(() => {
        if (!isAuthenticated || Platform.OS === 'web' || isExpoGo) return;

        // Dynamischer Import – wird nur außerhalb von Expo Go erreicht
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
