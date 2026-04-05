package org.openfamilycompass.repository;

import java.util.List;

import org.openfamilycompass.model.Notification;
import org.openfamilycompass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // All notifications for a user, newest first
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    // Only unread notifications for a user
    List<Notification> findByUserAndReadAtIsNullOrderByCreatedAtDesc(User user);

    // Number of unread notifications for a user
    long countByUserAndReadAtIsNull(User user);
}