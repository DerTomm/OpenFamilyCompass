package org.openfamilycompass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.core.userdetails.User.withUsername;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;

@SpringBootTest
class JwtTokenServiceTest {

    @Autowired
    private JwtTokenService jwtTokenService;

    private User testUser;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPin("encodedPin");
        testUser.setRole(UserRole.PARENT);

        userDetails = withUsername("testuser")
                .password("encodedPin")
                .roles("PARENT")
                .build();
    }

    @Test
    void generateAccessToken_ShouldCreateValidToken() {
        // When
        String token = jwtTokenService.generateAccessToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void generateRefreshToken_ShouldCreateValidToken() {
        // When
        String token = jwtTokenService.generateRefreshToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        // Given
        String token = jwtTokenService.generateAccessToken(testUser);

        // When
        String username = jwtTokenService.extractUsername(token);

        // Then
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void validateToken_ShouldReturnTrue_ForValidToken() {
        // Given
        String token = jwtTokenService.generateAccessToken(testUser);

        // When
        Boolean isValid = jwtTokenService.validateToken(token, userDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_ShouldReturnFalse_ForWrongUsername() {
        // Given
        String token = jwtTokenService.generateAccessToken(testUser);
        UserDetails wrongUser = withUsername("wronguser")
                .password("password")
                .roles("PARENT")
                .build();

        // When
        Boolean isValid = jwtTokenService.validateToken(token, wrongUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void isRefreshToken_ShouldReturnTrue_ForRefreshToken() {
        // Given
        String refreshToken = jwtTokenService.generateRefreshToken(testUser);

        // When
        Boolean isRefresh = jwtTokenService.isRefreshToken(refreshToken);

        // Then
        assertThat(isRefresh).isTrue();
    }

    @Test
    void isRefreshToken_ShouldReturnFalse_ForAccessToken() {
        // Given
        String accessToken = jwtTokenService.generateAccessToken(testUser);

        // When
        Boolean isRefresh = jwtTokenService.isRefreshToken(accessToken);

        // Then
        assertThat(isRefresh).isFalse();
    }

    @Test
    void extractExpiration_ShouldReturnFutureDate() {
        // Given
        String token = jwtTokenService.generateAccessToken(testUser);

        // When
        Date expiration = jwtTokenService.extractExpiration(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }

    @Test
    void extractClaim_ShouldReturnCorrectRole() {
        // Given
        String token = jwtTokenService.generateAccessToken(testUser);

        // When
        String role = jwtTokenService.extractClaim(token, claims -> claims.get("role", String.class));

        // Then
        assertThat(role).isEqualTo("PARENT");
    }

    @Test
    void extractClaim_ShouldReturnCorrectUserId() {
        // Given
        String token = jwtTokenService.generateAccessToken(testUser);

        // When
        Integer userId = jwtTokenService.extractClaim(token, claims -> claims.get("userId", Integer.class));

        // Then
        assertThat(userId).isEqualTo(1);
    }
}
