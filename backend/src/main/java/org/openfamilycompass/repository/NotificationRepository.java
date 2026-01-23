package org.openfamilycompass.repository;

import java.util.List;

import org.openfamilycompass.model.Notification;
import org.openfamilycompass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Alle Benachrichtigungen eines Users, neueste zuerst
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    // Nur ungelesene Benachrichtigungen eines Users
    List<Notification> findByUserAndReadAtIsNullOrderByCreatedAtDesc(User user);

    // Anzahl ungelesener Benachrichtigungen eines Users
    long countByUserAndReadAtIsNull(User user);
}