package com.family.kidschores.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.family.kidschores.model.User;
import com.family.kidschores.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AvatarController {

    private final UserService userService;

    // Liste der verfügbaren Avatar-Icons
    private static final List<String> AVAILABLE_ICONS = Arrays.asList(
            "lion", "panda", "dog", "cat", "elephant",
            "tiger", "rabbit", "bear", "fox", "owl",
            "penguin", "monkey", "koala", "giraffe", "zebra",
            "unicorn", "dragon", "robot", "astronaut", "superhero");

    /**
     * GET /api/avatar/icons - Liste aller verfügbaren Icons
     */
    @GetMapping("/api/avatar/icons")
    public ResponseEntity<Map<String, Object>> getAvailableIcons() {
        Map<String, Object> response = new HashMap<>();
        response.put("icons", AVAILABLE_ICONS);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /profile/avatar/select-icon - Icon als Avatar auswählen
     */
    @PostMapping("/profile/avatar/select-icon")
    public ResponseEntity<Map<String, String>> selectIcon(
            @RequestParam("iconName") String iconName,
            @AuthenticationPrincipal User currentUser) {

        if (!AVAILABLE_ICONS.contains(iconName)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Ungültiger Icon-Name");
            return ResponseEntity.badRequest().body(error);
        }

        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setAvatarType("ICON");
        user.setAvatarIconName(iconName);
        user.setAvatarData(null); // Foto löschen falls vorhanden
        userService.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Avatar erfolgreich aktualisiert");
        response.put("avatarType", "ICON");
        response.put("iconName", iconName);

        log.info("User {} selected icon avatar: {}", user.getUsername(), iconName);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /profile/avatar/upload - Foto als Avatar hochladen
     */
    @PostMapping("/profile/avatar/upload")
    public ResponseEntity<Map<String, String>> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {

        // Validierung
        if (file.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Keine Datei ausgewählt");
            return ResponseEntity.badRequest().body(error);
        }

        // Dateigröße prüfen (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Datei zu groß (max. 5MB)");
            return ResponseEntity.badRequest().body(error);
        }

        // Dateityp prüfen
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Ungültiger Dateityp (nur Bilder erlaubt)");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            User user = userService.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setAvatarType("PHOTO");
            user.setAvatarData(file.getBytes());
            user.setAvatarIconName(null); // Icon-Name löschen
            userService.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Avatar erfolgreich hochgeladen");
            response.put("avatarType", "PHOTO");

            log.info("User {} uploaded photo avatar ({} bytes)", user.getUsername(), file.getSize());
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Error uploading avatar", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Fehler beim Hochladen");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * GET /avatar/current - Aktuelles Avatar-Bild abrufen
     */
    @GetMapping("/avatar/current")
    public ResponseEntity<byte[]> getCurrentAvatar(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Wenn Foto hochgeladen wurde
        if ("PHOTO".equals(user.getAvatarType()) && user.getAvatarData() != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(user.getAvatarData());
        }

        // Wenn Icon gewählt wurde, redirect zu SVG
        if ("ICON".equals(user.getAvatarType()) && user.getAvatarIconName() != null) {
            try {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(java.net.URI.create("/images/avatars/" + user.getAvatarIconName() + ".svg"))
                        .build();
            } catch (Exception e) {
                log.error("Error redirecting to icon", e);
            }
        }

        // Fallback: No Content (Client zeigt Default-Icon)
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /avatar/{userId} - Avatar eines bestimmten Users abrufen (für
     * Admins/Parents)
     */
    @GetMapping("/avatar/{userId}")
    public ResponseEntity<byte[]> getUserAvatar(@PathVariable Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("PHOTO".equals(user.getAvatarType()) && user.getAvatarData() != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(user.getAvatarData());
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /profile/avatar - Avatar zurücksetzen auf DEFAULT
     */
    @DeleteMapping("/profile/avatar")
    public ResponseEntity<Map<String, String>> resetAvatar(@AuthenticationPrincipal User currentUser) {
        User user = userService.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setAvatarType("DEFAULT");
        user.setAvatarIconName(null);
        user.setAvatarData(null);
        userService.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Avatar zurückgesetzt");

        log.info("User {} reset avatar to default", user.getUsername());
        return ResponseEntity.ok(response);
    }
}
