package org.openfamilycompass.api.v1;

import java.util.List;
import java.util.stream.Collectors;

import org.openfamilycompass.api.v1.dto.NotificationDto;
import org.openfamilycompass.model.Notification;
import org.openfamilycompass.model.User;
import org.openfamilycompass.repository.UserDeviceRepository;
import org.openfamilycompass.service.NotificationService;
import org.openfamilycompass.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Notifications", description = "Push notifications management")
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserService userService;
    private final UserDeviceRepository userDeviceRepository;

    @GetMapping
    @Operation(summary = "Get notifications for current user")
    public ResponseEntity<List<NotificationDto.Response>> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "20") int limit) {

        User currentUser = getCurrentUser(jwt);
        List<Notification> notifications;

        if (unreadOnly) {
            notifications = notificationService.findUnreadByUser(currentUser);
        } else {
            notifications = notificationService.findByUser(currentUser);
        }

        notifications = notifications.stream()
                .limit(limit)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                notifications.stream()
                        .map(NotificationDto.Response::fromEntity)
                        .collect(Collectors.toList()));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<Void> markNotificationAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        User currentUser = getCurrentUser(jwt);
        Notification notification = notificationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        }

        notificationService.markAsRead(notification);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllNotificationsAsRead(@AuthenticationPrincipal Jwt jwt) {
        User currentUser = getCurrentUser(jwt);
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<NotificationDto.UnreadCountResponse> getUnreadNotificationCount(
            @AuthenticationPrincipal Jwt jwt) {

        User currentUser = getCurrentUser(jwt);
        int count = notificationService.countUnread(currentUser);
        return ResponseEntity.ok(NotificationDto.UnreadCountResponse.builder().count(count).build());
    }

    @PostMapping("/register-device")
    @Transactional
    @Operation(summary = "Register device for push notifications")
    public ResponseEntity<Void> registerDevice(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NotificationDto.RegisterDeviceRequest request) {

        User currentUser = getCurrentUser(jwt);
        // Prefer the stable device ID (UUID) sent by the client.
        // Fall back to token-based ID for backwards compatibility with older
        // app versions.
        String deviceId = (request.getDeviceId() != null && !request.getDeviceId().isBlank())
                ? request.getDeviceId()
                : request.getPlatform() + "_"
                        + request.getToken().substring(0, Math.min(20, request.getToken().length()));

        userDeviceRepository.upsertDevice(currentUser.getId(), deviceId, request.getToken());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/unregister-device")
    @Transactional
    @Operation(summary = "Unregister device from push notifications")
    public ResponseEntity<Void> unregisterDevice(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NotificationDto.UnregisterDeviceRequest request) {

        User currentUser = getCurrentUser(jwt);
        userDeviceRepository.deleteByUserAndFcmToken(currentUser, request.getToken());

        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser(Jwt jwt) {
        String username = jwt.getSubject();
        return userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
