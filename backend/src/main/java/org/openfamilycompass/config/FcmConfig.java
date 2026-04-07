package org.openfamilycompass.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;

import jakarta.annotation.PostConstruct;

/**
 * Configuration for Firebase Cloud Messaging (FCM) using REST API.
 * Loads the service account credentials from the filesystem path configured via
 * {@code fcm.service-account-path} (env: {@code FCM_SERVICE_ACCOUNT_PATH}).
 * The file is never bundled into the application JAR or Docker image.
 */
@Configuration
public class FcmConfig {

    private static final Logger log = LoggerFactory.getLogger(FcmConfig.class);

    @Value("${fcm.service-account-path}")
    private String serviceAccountPath;

    private GoogleCredentials googleCredentials;
    private String projectId;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initialize() {
        File serviceAccountFile = new File(serviceAccountPath);
        if (!serviceAccountFile.exists()) {
            log.warn("Firebase service account file not found at '{}'. Push notifications will not work.",
                    serviceAccountFile.getAbsolutePath());
            return;
        }

        try {
            // Read the JSON content to extract project_id
            String jsonContent = Files.readString(serviceAccountFile.toPath());
            JsonNode jsonNode = objectMapper.readTree(jsonContent);
            this.projectId = jsonNode.get("project_id").asText();
            log.info("FCM: Extracted project_id: {}", projectId);

            // Load credentials from the same file using a fresh stream
            try (FileInputStream fis = new FileInputStream(serviceAccountFile)) {
                this.googleCredentials = GoogleCredentials.fromStream(fis)
                        .createScoped("https://www.googleapis.com/auth/firebase.messaging");
            }

            log.info("FCM credentials initialized successfully for project: {}", projectId);
        } catch (IOException e) {
            log.error("Failed to initialize FCM credentials from '{}': {}", serviceAccountPath, e.getMessage());
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
