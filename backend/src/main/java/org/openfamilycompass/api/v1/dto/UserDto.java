package org.openfamilycompass.api.v1.dto;

import java.time.LocalDateTime;

import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String username;
        private String firstName;
        private UserRole role;
        private boolean active;
        private String theme;
        private String language;
        private String avatarType;
        private String avatarIconName;
        private String avatarPath;
        private int totalPoints;
        private LocalDateTime createdAt;

        public static Response fromEntity(User user) {
            return Response.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .firstName(user.getFirstName())
                    .role(user.getRole())
                    .active(user.isActive())
                    .theme(user.getTheme())
                    .language(user.getLanguage())
                    .avatarType(user.getAvatarType())
                    .avatarIconName(user.getAvatarIconName())
                    .avatarPath(user.getAvatarPath())
                    .totalPoints(user.getTotalPoints())
                    .createdAt(user.getCreatedAt())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChildResponse {
        private Long id;
        private String username;
        private String firstName;
        private String avatarType;
        private String avatarIconName;
        private String avatarPath;
        private int totalPoints;

        public static ChildResponse fromEntity(User user) {
            return ChildResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .firstName(user.getFirstName())
                    .avatarType(user.getAvatarType())
                    .avatarIconName(user.getAvatarIconName())
                    .avatarPath(user.getAvatarPath())
                    .totalPoints(user.getTotalPoints())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank
        @Size(min = 3)
        private String username;

        @NotBlank
        @Size(min = PasswordConstraints.MIN_LENGTH, max = PasswordConstraints.MAX_LENGTH,
                message = PasswordConstraints.SIZE_MESSAGE)
        private String password;

        @NotBlank
        private String firstName;

        @NotNull
        private UserRole role;

        /** ISO 639-1 language code, e.g. "en" or "de". Defaults to "en" when null. */
        private String language;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String firstName;

        @Size(min = PasswordConstraints.MIN_LENGTH, max = PasswordConstraints.MAX_LENGTH,
                message = PasswordConstraints.SIZE_MESSAGE)
        private String password;

        private Boolean active;

        private UserRole role;

        /** ISO 639-1 language code, e.g. "en" or "de". */
        private String language;

        private String avatarType;

        private String avatarIconName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileResponse {
        private Long id;
        private String username;
        private String firstName;
        private UserRole role;
        private String theme;
        private String language;
        private String avatarType;
        private String avatarIconName;
        private String avatarPath;
        private int totalPoints;

        public static ProfileResponse fromEntity(User user) {
            return ProfileResponse.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .firstName(user.getFirstName())
                    .role(user.getRole())
                    .theme(user.getTheme())
                    .language(user.getLanguage())
                    .avatarType(user.getAvatarType())
                    .avatarIconName(user.getAvatarIconName())
                    .avatarPath(user.getAvatarPath())
                    .totalPoints(user.getTotalPoints())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileRequest {
        private String username;
        private String firstName;
        private String theme;
        private String language;
        private String avatarType;
        private String avatarIconName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = PasswordConstraints.MIN_LENGTH, max = PasswordConstraints.MAX_LENGTH,
                message = PasswordConstraints.SIZE_MESSAGE)
        private String newPassword;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsernameCheckResponse {
        private boolean available;
        private String message;
    }
}
