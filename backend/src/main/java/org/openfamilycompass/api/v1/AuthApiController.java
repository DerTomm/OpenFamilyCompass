package org.openfamilycompass.api.v1;

import org.openfamilycompass.api.v1.dto.AuthDto;
import org.openfamilycompass.api.v1.dto.UserDto;
import org.openfamilycompass.model.User;
import org.openfamilycompass.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and token management")
public class AuthApiController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public ResponseEntity<UserDto.Response> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getSubject();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(UserDto.Response.fromEntity(user));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change current user's password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AuthDto.ChangePasswordRequest request) {
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
