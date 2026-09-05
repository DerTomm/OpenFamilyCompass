package org.openfamilycompass.api.v1.dto;

import java.time.LocalDateTime;

import org.openfamilycompass.model.Behavior;
import org.openfamilycompass.model.BehaviorEvaluation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class BehaviorDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String title;
        private String guideline;
        private int plusPoints;
        private int minusPoints;
        private UserDto.ChildResponse user;
        private int rank;
        private boolean active;
        private LocalDateTime createdAt;

        public static Response fromEntity(Behavior behavior) {
            return Response.builder()
                    .id(behavior.getId())
                    .title(behavior.getTitle())
                    .guideline(behavior.getGuideline())
                    .plusPoints(behavior.getPoints())
                    .minusPoints(behavior.getMinusPoints())
                    .user(behavior.getUser() != null ? UserDto.ChildResponse.fromEntity(behavior.getUser()) : null)
                    .rank(behavior.getRank())
                    .active(behavior.isActive())
                    .createdAt(behavior.getCreatedAt())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank
        private String title;

        @NotBlank
        private String guideline;

        @Min(0)
        private int plusPoints;

        @Min(0)
        private int minusPoints;

        private Long userId;

        private Integer rank;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String title;
        private String guideline;

        @Min(0)
        private Integer plusPoints;

        @Min(0)
        private Integer minusPoints;

        private Long userId;

        private Integer rank;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationResponse {
        private Long id;
        private Response behavior;
        private UserDto.ChildResponse user;
        private int currentPoints;
        private String remarks;
        private boolean committed;
        private UserDto.Response createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static EvaluationResponse fromEntity(BehaviorEvaluation eval) {
            return EvaluationResponse.builder()
                    .id(eval.getId())
                    .behavior(Response.fromEntity(eval.getBehavior()))
                    .user(UserDto.ChildResponse.fromEntity(eval.getUser()))
                    .currentPoints(eval.getCurrentPoints())
                    .remarks(eval.getRemarks())
                    .committed(eval.isCommitted())
                    .createdBy(eval.getCreatedBy() != null ? UserDto.Response.fromEntity(eval.getCreatedBy()) : null)
                    .createdAt(eval.getCreatedAt())
                    .updatedAt(eval.getUpdatedAt())
                    .build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveEvaluationRequest {
        @NotNull
        private Long behaviorId;

        @NotNull
        private Long userId;

        @NotNull
        private Integer currentPoints;

        private String remarks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommitEvaluationsRequest {
        @NotNull
        private Long userId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommitEvaluationsResponse {
        private int totalPointsAwarded;
        private int evaluationsCommitted;
    }
}
