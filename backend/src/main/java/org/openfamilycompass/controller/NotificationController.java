package org.openfamilycompass.controller;

import java.util.List;
import java.util.Map;

import org.openfamilycompass.model.Notification;
import org.openfamilycompass.model.User;
import org.openfamilycompass.service.NotificationService;
import org.openfamilycompass.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public String getNotifications(Authentication authentication, Model model) {
        User currentUser = (User) authentication.getPrincipal();
        List<Notification> notifications = notificationService.getNotificationsForUser(currentUser);
        model.addAttribute("notifications", notifications);
        return "notifications/list";
    }

    @GetMapping("/unread/count")
    @ResponseBody
    public long getUnreadCount(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        return notificationService.getUnreadCountForUser(currentUser);
    }

    @GetMapping("/recent")
    public String getRecentNotifications(Authentication authentication, Model model) {
        User currentUser = (User) authentication.getPrincipal();
        List<Notification> notifications = notificationService.getUnreadNotificationsForUser(currentUser);
        model.addAttribute("notifications", notifications);
        return "fragments/notifications :: notificationDropdown";
    }

    @PostMapping("/{id}/read")
    @ResponseBody
    public void markAsRead(@PathVariable Long id, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        notificationService.markAsRead(id, currentUser);
    }

    @PostMapping("/mark-all-read")
    @ResponseBody
    public void markAllAsRead(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        notificationService.markAllAsReadForUser(currentUser);
    }

    @PutMapping("/fcm-token")
    @ResponseBody
    public void updateFcmToken(@RequestBody Map<String, String> payload, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        String deviceId = payload.get("deviceId");
        String token = payload.get("token");
        if (deviceId != null && token != null) {
            userService.registerDevice(currentUser.getId(), deviceId, token);
        }
    }
}