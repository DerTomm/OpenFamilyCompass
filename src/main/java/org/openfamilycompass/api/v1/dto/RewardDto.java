package org.openfamilycompass.api.v1.dto;

import java.time.LocalDateTime;

import org.openfamilycompass.model.Reward;
import org.openfamilycompass.model.RewardRedemption;
import org.openfamilycompass.model.RewardStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class RewardDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private int pointsCost;
        private String imagePath;
        private boolean active;
        private LocalDateTime createdAt;

        public static Response fromEntity(Reward reward) {
            return Response.builder()
                    .id(reward.getId())
                    .title(reward.getTitle())
                    .description(reward.getDescription())
                    .pointsCost(reward.getPointsCost())
                    .imagePath(reward.getImagePath())
                    .active(reward.isActive())
                    .createdAt(reward.getCreatedAt())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank
        private String title;

        private String description;

        @Min(1)
        private int pointsCost;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String title;
        private String description;

        @Min(1)
        private Integer pointsCost;

        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedemptionResponse {
        private Long id;
        private UserDto.ChildResponse user;
        private Response reward;
        private int pointsSpent;
        private RewardStatus status;
        private LocalDateTime requestedAt;
        private LocalDateTime approvedAt;
        private UserDto.Response approvedBy;
        private LocalDateTime deliveredAt;
        private String notes;

        public static RedemptionResponse fromEntity(RewardRedemption redemption) {
            return RedemptionResponse.builder()
                    .id(redemption.getId())
                    .user(UserDto.ChildResponse.fromEntity(redemption.getUser()))
                    .reward(Response.fromEntity(redemption.getReward()))
                    .pointsSpent(redemption.getPointsSpent())
                    .status(redemption.getStatus())
                    .requestedAt(redemption.getRequestedAt())
                    .approvedAt(redemption.getApprovedAt())
                    .approvedBy(redemption.getApprovedBy() != null
                            ? UserDto.Response.fromEntity(redemption.getApprovedBy())
                            : null)
                    .deliveredAt(redemption.getDeliveredAt())
                    .notes(redemption.getNotes())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestRedemptionRequest {
        @NotNull
        private Long rewardId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectRedemptionRequest {
        private String notes;
    }
}
