package org.openfamilycompass.service;

import java.util.List;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Transactional
    public User createUser(@NonNull String username, @NonNull String pin, @NonNull UserRole role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        User user = new User();
        user.setUsername(username.toLowerCase());
        user.setPin(passwordEncoder.encode(pin));
        user.setRole(role);
        user.setActive(true);

        return userRepository.save(user);
    }

    @Transactional
    public User updatePin(@NonNull Long userId, @NonNull String newPin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPin(passwordEncoder.encode(newPin));
        return userRepository.save(user);
    }

    @Transactional
    public void deactivateUser(@NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setActive(false);
        userRepository.save(user);
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

    public List<User> findAllActive() {
        return userRepository.findByActiveTrue();
    }

    @Transactional
    public User save(@NonNull User user) {
        return userRepository.save(user);
    }
}
