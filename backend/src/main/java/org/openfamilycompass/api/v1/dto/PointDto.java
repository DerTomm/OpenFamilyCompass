package org.openfamilycompass.api.v1.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.openfamilycompass.model.PointTransaction;
import org.openfamilycompass.model.PointTransactionType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class PointDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BalanceResponse {
        private Long userId;
        private int totalPoints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionResponse {
        private Long id;
        private Long userId;
        private int points;
        private PointTransactionType type;
        private String description;
        private Long referenceId;
        private String remarks;
        private UserDto.Response createdBy;
        private LocalDateTime createdAt;
        private Integer balanceAfter;

        public static TransactionResponse fromEntity(PointTransaction tx) {
            return TransactionResponse.builder()
                    .id(tx.getId())
                    .userId(tx.getUser().getId())
                    .points(tx.getPoints())
                    .type(tx.getType())
                    .description(tx.getDescription())
                    .referenceId(tx.getReferenceId())
                    .remarks(tx.getRemarks())
                    .createdBy(tx.getCreatedBy() != null ? UserDto.Response.fromEntity(tx.getCreatedBy()) : null)
                    .createdAt(tx.getCreatedAt())
                    .build();
        }

        public static TransactionResponse fromEntityWithBalance(PointTransaction tx, int balanceAfter) {
            TransactionResponse response = fromEntity(tx);
            response.setBalanceAfter(balanceAfter);
            return response;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionsResponse {
        private List<TransactionResponse> transactions;
        private int total;
        private int limit;
        private int offset;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AwardPointsRequest {
        @NotNull
        private Long userId;

        @Min(1)
        private int points;

        @NotBlank
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddPointsRequest {
        @NotNull
        private Long userId;

        @NotNull
        private Integer points;

        @NotNull
        private PointTransactionType type;

        @NotBlank
        private String description;
    }
}
