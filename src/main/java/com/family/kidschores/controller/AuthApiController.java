package com.family.kidschores.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.family.kidschores.dto.LoginRequest;
import com.family.kidschores.dto.RefreshTokenRequest;
import com.family.kidschores.dto.TokenResponse;
import com.family.kidschores.model.User;
import com.family.kidschores.security.JwtTokenService;
import com.family.kidschores.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername().toLowerCase(),
                            loginRequest.getPassword()));

            User user = (User) authentication.getPrincipal();

            // Generate tokens
            String accessToken = jwtTokenService.generateAccessToken(user);
            String refreshToken = jwtTokenService.generateRefreshToken(user);

            TokenResponse response = new TokenResponse(
                    accessToken,
                    refreshToken,
                    jwtTokenService.getAccessTokenValidity(),
                    user.getUsername(),
                    user.getRole().name());

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();

            // Validate it's a refresh token
            if (!jwtTokenService.isRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Invalid refresh token"));
            }

            // Extract username and load user
            String username = jwtTokenService.extractUsername(refreshToken);
            User user = (User) userService.loadUserByUsername(username);

            // Validate refresh token
            if (!jwtTokenService.validateToken(refreshToken, user)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Refresh token expired"));
            }

            // Generate new tokens
            String newAccessToken = jwtTokenService.generateAccessToken(user);
            String newRefreshToken = jwtTokenService.generateRefreshToken(user);

            TokenResponse response = new TokenResponse(
                    newAccessToken,
                    newRefreshToken,
                    jwtTokenService.getAccessTokenValidity(),
                    user.getUsername(),
                    user.getRole().name());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Token refresh failed: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // For JWT, logout is client-side (delete tokens)
        // Could implement token blacklist here if needed
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    // Helper classes for responses
    record ErrorResponse(String error) {
    }

    record MessageResponse(String message) {
    }
}
