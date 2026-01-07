package org.openfamilycompass.repository;

import java.util.List;
import java.util.Optional;

import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    List<UserDevice> findByUser(User user);

    Optional<UserDevice> findByDeviceId(String deviceId);

    Optional<UserDevice> findByUserAndDeviceId(User user, String deviceId);

    void deleteByDeviceId(String deviceId);
}