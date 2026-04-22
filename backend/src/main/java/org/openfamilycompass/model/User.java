package org.openfamilycompass.model;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password; // Encrypted password

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "theme", length = 10)
    private String theme = "LIGHT"; // LIGHT or DARK

    @Column(name = "avatar_type", length = 20)
    private String avatarType = "DEFAULT"; // DEFAULT, ICON, PHOTO

    @Column(name = "avatar_icon_name", length = 50)
    private String avatarIconName; // Name of the selected icon (e.g. "lion", "panda")

    @Column(name = "avatar_data", columnDefinition = "BYTEA")
    private byte[] avatarData; // Photo upload as Blob

    @Column(name = "avatar_content_type", length = 50)
    private String avatarContentType; // MIME type of the uploaded photo

    @Column(name = "total_points", nullable = false)
    private int totalPoints = 0;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "language", length = 2)
    private String language; // User preferred language: "en" or "de", null means use browser default

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // UserDetails Interface Methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    /**
     * Returns the avatar path/URL for this user, or {@code null} if no avatar is
     * set or the avatar is rendered client-side.
     *
     * <ul>
     *   <li>PHOTO: returns {@code "/api/v1/users/{id}/avatar"} - fetched by the
     *   client.</li>
     *   <li>ICON / DEFAULT / null: returns {@code null}. Icon avatars are
     *   resolved client-side from {@code avatarIconName} against a bundled
     *   emoji/icon set; no server-side asset path is exposed.</li>
     * </ul>
     */
    public String getAvatarPath() {
        if ("PHOTO".equals(avatarType) && avatarData != null) {
            return "/api/v1/users/" + id + "/avatar";
        }
        return null;
    }
}
