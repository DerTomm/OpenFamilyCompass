package org.openfamilycompass.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;

@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);

    public void sendPushNotificationToUser(List<String> tokens, String title, String body) {
        log.debug("FCM: Attempting to send push notification to {} tokens", tokens != null ? tokens.size() : 0);
        log.debug("FCM: Title: {}, Body: {}", title, body);

        if (tokens == null || tokens.isEmpty()) {
            log.debug("FCM: No tokens provided, skipping push notification");
            return;
        }

        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            log.debug("FCM: Sending multicast message...");
            var response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.debug("FCM: Multicast response - Success: {}, Failure: {}", response.getSuccessCount(),
                    response.getFailureCount());

            for (int i = 0; i < response.getResponses().size(); i++) {
                var resp = response.getResponses().get(i);
                if (!resp.isSuccessful()) {
                    log.error("FCM: Failed to send to token {}: {}", tokens.get(i), resp.getException().getMessage());
                }
            }
        } catch (Exception e) {
            log.error("FCM: Error sending FCM multicast message", e);
        }
    }
}