package org.openfamilycompass.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration Tests for REST API Authentication (JWT).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("REST Auth Integration Tests")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Find or create test user
        User testUser = userRepository.findByUsername("testparent").orElse(null);
        if (testUser == null) {
            testUser = new User();
            testUser.setUsername("testparent");
            testUser.setFirstName("Test Parent");
            testUser.setPassword(passwordEncoder.encode("testpassword"));
            testUser.setRole(UserRole.PARENT);
            userRepository.save(testUser);
        }
    }

    @Test
    @DisplayName("Login should return 200 and tokens with valid credentials")
    void login_WithValidCredentials_ShouldSucceed() throws Exception {
        // Since we are using TestSecurityConfig which mocks the security chain,
        // we might not get actual JWTs unless the AuthController is fully functional in this context.
        // However, the AuthController logic (password check, token generation) should run if the endpoint is reachable.
        
        // Note: The TokenService requires RSA keys. If they are generated at runtime, this should work.
        
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testparent\",\"password\":\"testpassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Login should return 401 with invalid password")
    void login_WithInvalidPassword_ShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testparent\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login should return 401 with non-existent user")
    void login_WithNonExistentUser_ShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nonexistent\",\"password\":\"password\"}"))
                .andExpect(status().isUnauthorized());
    }
}
