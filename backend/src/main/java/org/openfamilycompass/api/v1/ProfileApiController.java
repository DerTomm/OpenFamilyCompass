package org.openfamilycompass.api.v1;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.openfamilycompass.api.v1.dto.UserDto;
import org.openfamilycompass.model.User;
import org.openfamilycompass.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "User profile management")
public class ProfileApiController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserDto.ProfileResponse> getProfile(@AuthenticationPrincipal Jwt principal) {
        User currentUser = getCurrentUser(principal);
        return ResponseEntity.ok(UserDto.ProfileResponse.fromEntity(currentUser));
    }

    @PutMapping
    @Operation(summary = "Update current user profile")
    public ResponseEntity<UserDto.ProfileResponse> updateProfile(
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody UserDto.UpdateProfileRequest request) {

        User currentUser = getCurrentUser(principal);

        // Update username if provided and different
        if (request.getUsername() != null && !request.getUsername().equals(currentUser.getUsername())) {
            String newUsername = request.getUsername().toLowerCase().trim();
            if (userService.existsByUsername(newUsername)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
            }
            currentUser.setUsername(newUsername);
        }

        if (request.getFirstName() != null) {
            currentUser.setFirstName(request.getFirstName());
        }
        if (request.getTheme() != null) {
            currentUser.setTheme(request.getTheme());
        }
        if (request.getLanguage() != null) {
            currentUser.setLanguage(request.getLanguage());
        }
        if (request.getAvatarType() != null) {
            currentUser.setAvatarType(request.getAvatarType());
        }
        if (request.getAvatarIconName() != null) {
            currentUser.setAvatarIconName(request.getAvatarIconName());
        }

        User saved = userService.save(currentUser);
        return ResponseEntity.ok(UserDto.ProfileResponse.fromEntity(saved));
    }

    @PostMapping("/password")
    @Operation(summary = "Change current user password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt principal,
            @Valid @RequestBody UserDto.ChangePasswordRequest request) {

        User currentUser = getCurrentUser(principal);
        boolean success = userService.changePassword(
                currentUser.getId(),
                request.getCurrentPassword(),
                request.getNewPassword());

        if (!success) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/username/check")
    @Operation(summary = "Check if username is available")
    public ResponseEntity<UserDto.UsernameCheckResponse> checkUsername(
            @AuthenticationPrincipal Jwt principal,
            @RequestParam String username) {

        User currentUser = getCurrentUser(principal);
        String checkUsername = username.toLowerCase().trim();

        // Username is available if it doesn't exist OR if it's the current user's
        // username
        boolean isCurrentUsername = checkUsername.equals(currentUser.getUsername().toLowerCase());
        boolean exists = userService.existsByUsername(checkUsername);
        boolean available = isCurrentUsername || !exists;

        String message = available
                ? (isCurrentUsername ? "This is your current username" : "Username is available")
                : "Username already taken";

        return ResponseEntity.ok(new UserDto.UsernameCheckResponse(available, message));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload avatar image")
    public ResponseEntity<UserDto.ProfileResponse> uploadAvatar(
            @AuthenticationPrincipal Jwt principal,
            @RequestParam("file") MultipartFile file) {

        User currentUser = getCurrentUser(principal);

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 5MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }

        try {
            currentUser.setAvatarData(file.getBytes());
            currentUser.setAvatarType("PHOTO");
            User saved = userService.save(currentUser);
            return ResponseEntity.ok(UserDto.ProfileResponse.fromEntity(saved));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload avatar");
        }
    }

    @GetMapping("/avatar/icons")
    @Operation(summary = "Get available avatar icons")
    public ResponseEntity<List<String>> getAvatarIcons() {
        // No predefined icons available
        return ResponseEntity.ok(Collections.emptyList());
    }

    private User getCurrentUser(Jwt principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        String username = principal.getSubject();
        return userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
