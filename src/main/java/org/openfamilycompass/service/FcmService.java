package org.openfamilycompass.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openfamilycompass.config.FcmConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for sending push notifications via Firebase Cloud Messaging REST API.
 * Uses direct HTTP calls instead of the firebase-admin SDK to reduce
 * dependencies.
 */
@Service
public class FcmService {

    private static final Logger log = LoggerFactory.getLogger(FcmService.class);
    private static final String FCM_SEND_ENDPOINT = "https://fcm.googleapis.com/v1/projects/%s/messages:send";

    private final FcmConfig fcmConfig;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String projectId;

    public FcmService(FcmConfig fcmConfig) {
        this.fcmConfig = fcmConfig;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.projectId = fcmConfig.getProjectId();
    }

    public void sendPushNotificationToUser(List<String> tokens, String title, String body) {
        log.debug("FCM: Attempting to send push notification to {} tokens", tokens != null ? tokens.size() : 0);
        log.debug("FCM: Title: {}, Body: {}", title, body);
        log.debug("FCM: Using projectId: {}", projectId);

        if (tokens == null || tokens.isEmpty()) {
            log.debug("FCM: No tokens provided, skipping push notification");
            return;
        }

        if (projectId == null || projectId.isEmpty()) {
            log.error("FCM: Project ID not configured. Set fcm.project-id in application.yml");
            return;
        }

        String accessToken = fcmConfig.getAccessToken();
        if (accessToken == null) {
            log.error("FCM: Unable to get access token. Check credentials configuration.");
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        // Send individual messages (FCM REST API doesn't support multicast directly)
        for (String token : tokens) {
            try {
                boolean success = sendSingleMessage(token, title, body, accessToken);
                if (success) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                log.error("FCM: Failed to send to token {}: {}", token, e.getMessage());
                failureCount++;
            }
        }

        log.debug("FCM: Push notification completed - Success: {}, Failure: {}", successCount, failureCount);
    }

    private boolean sendSingleMessage(String token, String title, String body, String accessToken)
            throws IOException, InterruptedException {
        // Build FCM message payload
        Map<String, Object> message = new HashMap<>();
        message.put("token", token);

        Map<String, String> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("body", body);
        message.put("notification", notification);

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);

        String jsonPayload = objectMapper.writeValueAsString(payload);

        // Build HTTP request
        String url = String.format(FCM_SEND_ENDPOINT, projectId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json; UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        // Send request
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            log.debug("FCM: Successfully sent message to token: {}", token);
            return true;
        } else {
            log.error("FCM: Failed to send message to token {}. Status: {}, Response: {}",
                    token, response.statusCode(), response.body());
            return false;
        }
    }
}