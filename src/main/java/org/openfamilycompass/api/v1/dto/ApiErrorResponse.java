package org.openfamilycompass.api.v1.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {
    private String error;
    private String message;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
