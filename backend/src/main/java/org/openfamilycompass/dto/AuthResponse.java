package org.openfamilycompass.dto;

public record AuthResponse(
    String accessToken, 
    String refreshToken, 
    String tokenType, 
    long expiresIn
) {
    public AuthResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer", 3600);
    }
}
