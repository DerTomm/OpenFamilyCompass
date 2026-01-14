package org.openfamilycompass.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;

import jakarta.annotation.PostConstruct;

/**
 * Configuration for Firebase Cloud Messaging (FCM) using REST API.
 * Loads the service account credentials for OAuth2 authentication.
 */
@Configuration
public class FcmConfig {

    private static final Logger log = LoggerFactory.getLogger(FcmConfig.class);

    private GoogleCredentials googleCredentials;
    private String projectId;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initialize() {
        try {
            ClassPathResource resource = new ClassPathResource("firebase-service-account.json");

            if (!resource.exists()) {
                log.warn("Firebase service account file not found. Push notifications will not work.");
                return;
            }

            // Read the JSON content to extract project_id
            String jsonContent = new String(resource.getInputStream().readAllBytes());
            JsonNode jsonNode = objectMapper.readTree(jsonContent);
            this.projectId = jsonNode.get("project_id").asText();

            log.info("Extracted project_id: {}", projectId);

            // Now load credentials from a new stream of the same resource
            this.googleCredentials = GoogleCredentials.fromStream(resource.getInputStream())
                    .createScoped("https://www.googleapis.com/auth/firebase.messaging");

            log.info("FCM credentials loaded successfully: {}", googleCredentials != null);

            log.info("FCM credentials initialized successfully for project: {}", projectId);
        } catch (IOException e) {
            log.error("Failed to initialize FCM credentials", e);
        }
    }

    /**
     * Returns the Google credentials for FCM authentication.
     * Refreshes the access token if needed.
     */
    public GoogleCredentials getCredentials() {
        if (googleCredentials != null) {
            try {
                googleCredentials.refresh();
            } catch (IOException e) {
                log.error("Failed to refresh FCM credentials", e);
            }
        }
        return googleCredentials;
    }

    /**
     * Returns the current access token for FCM API requests.
     */
    public String getAccessToken() {
        GoogleCredentials credentials = getCredentials();
        log.debug("FCM getAccessToken: credentials = {}", credentials);
        if (credentials != null && credentials.getAccessToken() != null) {
            log.debug("FCM access token: {}", credentials.getAccessToken().getTokenValue());
            return credentials.getAccessToken().getTokenValue();
        }
        log.warn("FCM: No access token available");
        return null;
    }

    /**
     * Returns the project ID from the service account JSON.
     */
    public String getProjectId() {
        return projectId;
    }
}
