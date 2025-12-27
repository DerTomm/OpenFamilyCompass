package org.openfamilycompass.init;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-username}")
    private String adminUsername;

    @Value("${app.admin.default-pin}")
    private String adminPin;

    @Override
    public void run(String... args) throws Exception {
        initializeAdminUser();
        initializeAvatarDirectory();
    }

    private void initializeAdminUser() {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User admin = new User();
            admin.setUsername(adminUsername.toLowerCase());
            admin.setPin(passwordEncoder.encode(adminPin));
            admin.setRole(UserRole.ADMIN);
            admin.setActive(true);

            userRepository.save(admin);
            log.info("Admin user created: username='{}', PIN='{}'", adminUsername, adminPin);
            log.warn("IMPORTANT: Please change the admin password after the first login!");
        } else {
            log.info("Admin user already exists.");
        }
    }

    private void initializeAvatarDirectory() {
        // Avatar directory will be created automatically when needed
        log.info("Avatar directory will be created on first upload");
    }
}
