package org.openfamilycompass.api.v1.dto;

import java.time.LocalDateTime;

import org.openfamilycompass.model.Notification;
import org.openfamilycompass.model.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class NotificationDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private NotificationType type;
        private String title;
        private String message;
        private Long referenceId;
        private LocalDateTime createdAt;
        private LocalDateTime readAt;
        private boolean read;

        public static Response fromEntity(Notification notification) {
            return Response.builder()
                    .id(notification.getId())
                    .type(notification.getType())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .referenceId(notification.getReferenceId())
                    .createdAt(notification.getCreatedAt())
                    .readAt(notification.getReadAt())
                    .read(notification.isRead())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnreadCountResponse {
        private int count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterDeviceRequest {
        @NotBlank
        private String token;

        @NotNull
        private String platform;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnregisterDeviceRequest {
        @NotBlank
        private String token;
    }
}
