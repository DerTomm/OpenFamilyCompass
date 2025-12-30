package org.openfamilycompass.config;

import java.util.List;

import org.openfamilycompass.model.Notification;
import org.openfamilycompass.model.User;
import org.openfamilycompass.service.NotificationService;
import org.openfamilycompass.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class ThemeControllerAdvice {

    private final NotificationService notificationService;
    private final UserService userService;

    @ModelAttribute
    public void addThemeToModel(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            String theme = user.getTheme();

            if (theme == null || theme.isEmpty()) {
                theme = "LIGHT";
            }

            model.addAttribute("userTheme", theme.toLowerCase());

            // Add notifications for navbar
            List<Notification> notifications = notificationService.getUnreadNotificationsForUser(user);
            model.addAttribute("notifications", notifications);
        } else {
            model.addAttribute("userTheme", "light");
            model.addAttribute("notifications", List.of());
        }
    }
}
