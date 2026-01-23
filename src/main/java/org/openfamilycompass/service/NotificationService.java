package org.openfamilycompass.service;

import java.time.LocalDateTime;
import java.util.List;

import org.openfamilycompass.model.Notification;
import org.openfamilycompass.model.NotificationType;
import org.openfamilycompass.model.User;
import org.openfamilycompass.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final FcmService fcmService;
    private final UserService userService;

    @Transactional
    public Notification createNotification(User user, NotificationType type, String title, String message,
            Long referenceId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setCreatedAt(LocalDateTime.now());

        Notification savedNotification = notificationRepository.save(notification);

        // Send push notification to all user's devices
        log.debug("NOTIFICATION: Created notification for user {}, sending push...", user.getId());
        List<String> tokens = userService.getFcmTokensForUser(user.getId());
        log.debug("NOTIFICATION: Found {} FCM tokens for user", tokens.size());
        fcmService.sendPushNotificationToUser(tokens, title, message);

        return savedNotification;
    }

    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Notification> getUnreadNotificationsForUser(User user) {
        return notificationRepository.findByUserAndReadAtIsNullOrderByCreatedAtDesc(user);
    }

    public long getUnreadCountForUser(User user) {
        return notificationRepository.countByUserAndReadAtIsNull(user);
    }

    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        // Sicherstellen, dass der User die Benachrichtigung auch besitzt
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Notification does not belong to user");
        }

        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsReadForUser(User user) {
        List<Notification> unreadNotifications = getUnreadNotificationsForUser(user);
        for (Notification notification : unreadNotifications) {
            notification.markAsRead();
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    public java.util.Optional<Notification> findById(Long id) {
        return notificationRepository.findById(id);
    }

    public List<Notification> findByUser(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Notification> findUnreadByUser(User user) {
        return notificationRepository.findByUserAndReadAtIsNullOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void markAsRead(Notification notification) {
        notification.markAsRead();
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        markAllAsReadForUser(user);
    }

    public int countUnread(User user) {
        return (int) notificationRepository.countByUserAndReadAtIsNull(user);
    }
}