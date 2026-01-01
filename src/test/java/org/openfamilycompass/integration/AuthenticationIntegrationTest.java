package org.openfamilycompass.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration-Tests für die formularbasierte Web-Authentifizierung.
 * 
 * Hinweis: Die Anwendung verwendet derzeit Session-basierte Authentifizierung
 * für die Web-UI. Eine REST API mit JWT/OIDC-Authentifizierung ist noch nicht
 * implementiert. Diese Tests validieren die vorhandene formularbasierte
 * Authentifizierung.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Web-Authentifizierung Integration Tests")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setUsername("testparent");
        testUser.setFirstName("Test Parent");
        testUser.setPassword(passwordEncoder.encode("testpassword"));
        testUser.setRole(UserRole.PARENT);
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Login sollte mit gültigen Zugangsdaten erfolgreich sein")
    void login_WithValidCredentials_ShouldSucceed() throws Exception {
        mockMvc.perform(formLogin("/perform_login")
                .user("username", "testparent")
                .password("password", "testpassword"))
                .andExpect(authenticated())
                .andExpect(redirectedUrlPattern("/**/dashboard"));
    }

    @Test
    @DisplayName("Login sollte mit ungültigem Passwort fehlschlagen")
    void login_WithInvalidPassword_ShouldFail() throws Exception {
        mockMvc.perform(formLogin("/perform_login")
                .user("username", "testparent")
                .password("password", "wrongpassword"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrlPattern("/**/login?error*"));
    }

    @Test
    @DisplayName("Login sollte mit nicht existierendem Benutzer fehlschlagen")
    void login_WithNonExistentUser_ShouldFail() throws Exception {
        mockMvc.perform(formLogin("/perform_login")
                .user("username", "nonexistent")
                .password("password", "password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrlPattern("/**/login?error*"));
    }

    @Test
    @DisplayName("Zugriff auf geschützte Ressource ohne Authentifizierung sollte zur Login-Seite umleiten")
    void accessProtectedResource_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "testparent", roles = "PARENT")
    @DisplayName("Zugriff auf Dashboard mit Authentifizierung sollte erfolgreich sein")
    void accessDashboard_WithAuthentication_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/parent/dashboard"));
    }
}
