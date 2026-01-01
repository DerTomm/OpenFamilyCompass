package org.openfamilycompass.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.openfamilycompass.backup.BackupService;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserService userService;
    private final BackupService backupService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<User> users = userService.findAllActive();
        model.addAttribute("users", users);
        return "admin/dashboard";
    }

    // User Management
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userService.findAllActive();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/users/create")
    public String createUserForm(Model model) {
        model.addAttribute("roles", UserRole.values());
        return "admin/user-create";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
            @RequestParam String password,
            @RequestParam UserRole role,
            @RequestParam String firstName,
            @RequestParam(required = false) String avatarPath) {
        userService.createUser(username, password, role, firstName);

        return "redirect:/admin/users";
    }

    // Backup & Restore
    @GetMapping("/backup")
    public String backupPage(Model model, org.springframework.security.web.csrf.CsrfToken csrfToken) {
        // CSRF token is automatically added to model by Spring Security
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
        return "admin/backup";
    }

    @PostMapping("/backup/create")
    public ResponseEntity<byte[]> createBackup(
            @RequestParam(required = false, defaultValue = "Automated backup") String description) {
        try {
            log.info("Creating database backup with description: {}", description);
            byte[] backupData = backupService.createBackup(description);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "backup_" + timestamp + ".zip";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(backupData);
        } catch (Exception e) {
            log.error("Backup failed", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/backup/restore")
    public String restoreBackup(@RequestParam("backupFile") MultipartFile file, Model model) {
        try {
            if (file.isEmpty()) {
                model.addAttribute("error", "Please select a backup file");
                return "admin/backup";
            }

            log.info("Validating and scheduling backup file for restoration: {}", file.getOriginalFilename());
            byte[] backupData = file.getBytes();
            backupService.restoreBackup(backupData);

            model.addAttribute("warning", "Backup validated and scheduled for restoration. " +
                    "The server must be restarted to apply the backup. " +
                    "No changes have been made to the running database yet.");
            return "admin/backup";
        } catch (Exception e) {
            log.error("Restore scheduling failed", e);
            model.addAttribute("error", "Restore failed: " + e.getMessage());
            return "admin/backup";
        }
    }
}
