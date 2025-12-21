package com.family.kidschores.init;

import com.family.kidschores.model.User;
import com.family.kidschores.model.UserRole;
import com.family.kidschores.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
            admin.setUsername(adminUsername);
            admin.setPin(passwordEncoder.encode(adminPin));
            admin.setRole(UserRole.ADMIN);
            admin.setActive(true);

            userRepository.save(admin);
            log.info("Admin user created: username='{}', PIN='{}'", adminUsername, adminPin);
            log.warn("WICHTIG: Bitte ändere das Admin-Passwort nach der ersten Anmeldung!");
        } else {
            log.info("Admin user already exists.");
        }
    }

    private void initializeAvatarDirectory() {
        // Avatar-Verzeichnis wird automatisch erstellt, wenn benötigt
        log.info("Avatar directory will be created on first upload");
    }
}
