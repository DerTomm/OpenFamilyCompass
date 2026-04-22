package org.openfamilycompass.init;

import org.openfamilycompass.backup.BackupService;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({ "!test", "!initial-data" })
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BackupService backupService;

    @Value("${app.admin.default-username}")
    private String adminUsername;

    @Value("${app.admin.default-password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {
        // Apply any pending backup restore FIRST, before other initializations
        try {
            backupService.applyPendingRestore();
        } catch (Exception e) {
            log.error("Failed to apply pending backup restore during startup", e);
            throw new RuntimeException(
                    "Critical: Pending backup restore failed. Check logs and manual intervention may be required.", e);
        }

        // Then proceed with normal initialization
        initializeAdminUser();
        initializeAvatarDirectory();
    }

    private void initializeAdminUser() {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User admin = new User();
            admin.setUsername(adminUsername.toLowerCase());
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(UserRole.ADMIN);
            admin.setFirstName(adminUsername);
            admin.setActive(true);

            userRepository.save(admin);
            // Never log the initial password in clear text. The value is taken from
            // app.admin.default-password (or APP_ADMIN_DEFAULT_PASSWORD) and must be
            // changed on first login anyway.
            log.info("Admin user '{}' created with the configured initial password.", adminUsername);
            log.warn("IMPORTANT: Change the admin password on first login.");
        } else {
            log.info("Admin user already exists.");
        }
    }

    private void initializeAvatarDirectory() {
        // Avatar directory will be created automatically when needed
        log.info("Avatar directory will be created on first upload");
    }
}
