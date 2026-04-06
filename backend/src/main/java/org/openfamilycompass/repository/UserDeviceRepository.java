package org.openfamilycompass.repository;

import java.util.List;
import java.util.Optional;

import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

        List<UserDevice> findByUser(User user);

        Optional<UserDevice> findByDeviceId(String deviceId);

        Optional<UserDevice> findByUserAndDeviceId(User user, String deviceId);

        void deleteByDeviceId(String deviceId);

        void deleteAllByUser(User user);

        @Modifying
        @Transactional
        @Query("DELETE FROM UserDevice d WHERE d.user = :user AND d.fcmToken = :fcmToken")
        void deleteByUserAndFcmToken(@Param("user") User user, @Param("fcmToken") String fcmToken);

        @Modifying
        @Query(value = """
                        INSERT INTO user_devices (user_id, device_id, fcm_token, created_at, updated_at)
                        VALUES (:userId, :deviceId, :fcmToken, NOW(), NOW())
                        ON CONFLICT (device_id) DO UPDATE
                          SET user_id = EXCLUDED.user_id,
                              fcm_token = EXCLUDED.fcm_token,
                              updated_at = NOW()
                        """, nativeQuery = true)
        void upsertDevice(@Param("userId") Long userId,
                        @Param("deviceId") String deviceId,
                        @Param("fcmToken") String fcmToken);
}