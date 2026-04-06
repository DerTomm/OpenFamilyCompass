package org.openfamilycompass.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.openfamilycompass.model.Notification;
import org.openfamilycompass.model.NotificationType;
import org.openfamilycompass.model.User;
import org.openfamilycompass.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
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
    private final MessageSource messageSource;

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
        log.debug("NOTIFICATION: Found {} FCM tokens for user {}", tokens.size(), user.getId());
        fcmService.sendPushNotificationToUser(tokens, title, message);

        return savedNotification;
    }

    /**
     * Creates a notification whose title and message are resolved from the
     * message source using the target user's preferred language.
     *
     * @param user        the notification recipient (their {@code language} field
     *                    determines the locale)
     * @param type        notification type
     * @param titleKey    message key for the title (no arguments)
     * @param messageKey  message key for the body text
     * @param messageArgs arguments passed to the message format (may be null)
     * @param referenceId id of the referenced entity
     */
    @Transactional
    public Notification createLocalizedNotification(User user, NotificationType type,
            String titleKey, String messageKey, Object[] messageArgs, Long referenceId) {
        Locale locale = resolveLocale(user);
        String title = messageSource.getMessage(titleKey, null, locale);
        String message = messageSource.getMessage(messageKey, messageArgs, locale);
        return createNotification(user, type, title, message, referenceId);
    }

    /**
     * Like {@link #createLocalizedNotification} but appends an optional note
     * (e.g. a rejection reason) to the message body. The note prefix
     * ("Note: " / "Notiz: ") is also resolved from the message source using
     * the key {@code notification.note.suffix}.
     *
     * @param note raw note text entered by a user; pass {@code null} to omit
     */
    @Transactional
    public Notification createLocalizedNotification(User user, NotificationType type,
            String titleKey, String messageKey, Object[] messageArgs, String note, Long referenceId) {
        Locale locale = resolveLocale(user);
        String title = messageSource.getMessage(titleKey, null, locale);
        String message = messageSource.getMessage(messageKey, messageArgs, locale);
        if (note != null && !note.isBlank()) {
            message += messageSource.getMessage("notification.note.suffix", new Object[] { note }, locale);
        }
        return createNotification(user, type, title, message, referenceId);
    }

    /**
     * Derives a {@link Locale} from the user's stored language preference.
     * Returns {@link Locale#ROOT} for null/blank/"en" so that Spring's
     * {@code MessageSource} falls back to the default {@code messages.properties}
     * file instead of looking for the non-existent {@code messages_en.properties}.
     */
    private Locale resolveLocale(User user) {
        String lang = user.getLanguage();
        if (lang == null || lang.isBlank() || "en".equalsIgnoreCase(lang)) {
            return Locale.ROOT;
        }
        return Locale.forLanguageTag(lang);
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

        // Ensure the user actually owns the notification
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