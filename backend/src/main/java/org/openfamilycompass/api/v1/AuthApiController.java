package org.openfamilycompass.api.v1;

import org.openfamilycompass.api.v1.dto.AuthDto;
import org.openfamilycompass.api.v1.dto.UserDto;
import org.openfamilycompass.model.User;
import org.openfamilycompass.security.TokenService;
import org.openfamilycompass.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and token management")
@Slf4j
public class AuthApiController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    @Operation(summary = "Login with username and password", description = "Returns access and refresh tokens upon successful authentication.")
    @ApiResponse(responseCode = "200", description = "Successfully authenticated", content = @Content(schema = @Schema(implementation = AuthDto.TokenResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/login")
    public ResponseEntity<AuthDto.TokenResponse> login(@RequestBody @Valid AuthDto.LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            
            String token = tokenService.generateToken(authentication);
            String refreshToken = tokenService.generateRefreshToken(authentication);
            
            log.info("Login successful for user: {}", request.getUsername());
            
            return ResponseEntity.ok(AuthDto.TokenResponse.builder()
                    .accessToken(token)
                    .refreshToken(refreshToken)
                    .expiresIn(3600) // 1 hour
                    .build());
        } catch (AuthenticationException e) {
            log.warn("Login failed for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "Refresh access token", description = "Returns a new access token using a valid refresh token.")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully", content = @Content(schema = @Schema(implementation = AuthDto.TokenResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthDto.TokenResponse> refresh(@RequestBody @Valid AuthDto.RefreshTokenRequest request) {
        log.info("Token refresh attempt");
        try {
            // Validate signature, expiration and that the token was actually issued
            // as a refresh token (carries "type": "refresh"). This prevents access
            // tokens from being accepted here.
            if (!tokenService.validateRefreshToken(request.getRefreshToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String username = tokenService.getUsernameFromRefreshToken(request.getRefreshToken());

            // Reload the user from the database so the freshly issued access token
            // reflects the current role and active state.
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(),
                    null,
                    user.getAuthorities());

            String newToken = tokenService.generateToken(authentication);
            // Rotate the refresh token on every refresh.
            String newRefreshToken = tokenService.generateRefreshToken(authentication);

            return ResponseEntity.ok(AuthDto.TokenResponse.builder()
                    .accessToken(newToken)
                    .refreshToken(newRefreshToken)
                    .expiresIn(3600)
                    .build());

        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user info")
    public ResponseEntity<UserDto.Response> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = jwt.getSubject();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(UserDto.Response.fromEntity(user));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change current user's password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AuthDto.ChangePasswordRequest request) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = jwt.getSubject();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().build();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.save(user);

        return ResponseEntity.noContent().build();
    }
}
