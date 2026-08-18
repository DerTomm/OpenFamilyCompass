package org.openfamilycompass.exception;

import org.openfamilycompass.api.v1.UserApiController;
import org.openfamilycompass.api.v1.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice(basePackageClasses = { UserApiController.class })
@Slf4j
public class GlobalApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("IllegalArgumentException: {}", ex.getMessage());
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        log.error("IllegalStateException: {}", ex.getMessage());
        return errorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error in API: {}", ex.getMessage(), ex);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact the administrator.");
    }

    /**
     * Builds the error body shared by all handlers. The {@code error} code is
     * derived from the HTTP status name so that it stays consistent with the
     * responses written directly by servlet filters such as the rate limiter.
     */
    private ResponseEntity<ApiErrorResponse> errorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.builder()
                        .error(status.name())
                        .message(message)
                        .build());
    }
}
