package org.openfamilycompass.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserDevice;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.UserDeviceRepository;
import org.openfamilycompass.repository.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Transactional
    public User createUser(@NonNull String username, @NonNull String password, @NonNull UserRole role,
            @NonNull String firstName) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        User user = new User();
        user.setUsername(username.toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setFirstName(firstName);
        user.setActive(true);

        return userRepository.save(user);
    }

    public boolean existsByUsername(@NonNull String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional
    public User updatePassword(@NonNull Long userId, @NonNull String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    @Transactional
    public boolean changePassword(@NonNull Long userId, @NonNull String currentPassword, @NonNull String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false; // Current password is incorrect
        }

        // Update to new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    @Transactional
    public void deactivateUser(@NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void delete(@NonNull User user) {
        // Delete all related data (devices, etc.)
        userDeviceRepository.deleteAllByUser(user);

        // Delete the user
        userRepository.delete(user);
    }

    @Transactional
    public void deleteById(@NonNull Long userId) {
        userRepository.deleteById(userId);
    }

    public Optional<User> findById(@NonNull Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(@NonNull String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> findAllByRole(@NonNull UserRole role) {
        return userRepository.findByRole(role);
    }

    public List<User> findByRole(@NonNull UserRole role) {
        return userRepository.findByRole(role);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findAllActive() {
        return userRepository.findByActiveTrue();
    }

    @Transactional
    public User save(@NonNull User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void registerDevice(@NonNull Long userId, @NonNull String deviceId, @NonNull String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<UserDevice> existingDevice = userDeviceRepository.findByUserAndDeviceId(user, deviceId);
        if (existingDevice.isPresent()) {
            // Update existing
            UserDevice device = existingDevice.get();
            device.setFcmToken(fcmToken);
            device.setUpdatedAt(LocalDateTime.now());
            userDeviceRepository.save(device);
        } else {
            // Create new
            UserDevice device = new UserDevice();
            device.setUser(user);
            device.setDeviceId(deviceId);
            device.setFcmToken(fcmToken);
            device.setCreatedAt(LocalDateTime.now());
            device.setUpdatedAt(LocalDateTime.now());
            userDeviceRepository.save(device);
        }
    }

    public List<String> getFcmTokensForUser(@NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return userDeviceRepository.findByUser(user).stream()
                .map(UserDevice::getFcmToken)
                .toList();
    }

    @Transactional
    public void unregisterDevice(@NonNull String deviceId) {
        userDeviceRepository.deleteByDeviceId(deviceId);
    }
}
