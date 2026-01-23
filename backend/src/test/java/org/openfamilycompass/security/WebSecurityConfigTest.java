package org.openfamilycompass.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openfamilycompass.integration.TestSecurityConfig;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Comprehensive security tests for the OpenFamilyCompass application.
 * Tests both web UI session-based authentication and basic security
 * configuration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("Security Configuration Tests")
public class WebSecurityConfigTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private UserService userService;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private org.springframework.security.core.userdetails.User createTestUser(String username, String password,
                        UserRole role) {
                return new org.springframework.security.core.userdetails.User(
                                username,
                                passwordEncoder.encode(password),
                                true,
                                true,
                                true,
                                true,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        }

        // ===== LOGIN TESTS =====

        @Test
        @DisplayName("Successful login redirects to dashboard")
        void testLogin_Success() throws Exception {
                // Given
                when(userService.loadUserByUsername("parent"))
                                .thenReturn(createTestUser("parent", "password123", UserRole.PARENT));

                // When/Then
                mockMvc.perform(formLogin("/perform_login")
                                .user("username", "parent")
                                .password("password", "password123"))
                                .andExpect(authenticated())
                                .andExpect(redirectedUrl("/dashboard"));
        }

        @Test
        @DisplayName("Login with wrong password fails")
        void testLogin_InvalidPassword() throws Exception {
                // Given
                when(userService.loadUserByUsername("parent"))
                                .thenReturn(createTestUser("parent", "password123", UserRole.PARENT));

                // When/Then
                mockMvc.perform(formLogin("/perform_login")
                                .user("username", "parent")
                                .password("password", "wrongPassword"))
                                .andExpect(unauthenticated())
                                .andExpect(redirectedUrl("/login?error=true"));
        }

        @Test
        @DisplayName("Login with non-existent user fails")
        void testLogin_NonExistentUser() throws Exception {
                // Given
                when(userService.loadUserByUsername("nonexistent"))
                                .thenThrow(new UsernameNotFoundException("User not found"));

                // When/Then
                mockMvc.perform(formLogin("/perform_login")
                                .user("username", "nonexistent")
                                .password("password", "anyPassword"))
                                .andExpect(unauthenticated())
                                .andExpect(redirectedUrl("/login?error=true"));
        }

        // ===== LOGOUT TESTS =====

        @Test
        @DisplayName("Logout successfully clears authentication")
        void testLogout_Success() throws Exception {
                // Given
                when(userService.loadUserByUsername("parent"))
                                .thenReturn(createTestUser("parent", "password123", UserRole.PARENT));

                // When: Login first
                mockMvc.perform(formLogin("/perform_login")
                                .user("username", "parent")
                                .password("password", "password123"))
                                .andExpect(authenticated());

                // Then: Logout
                mockMvc.perform(logout("/logout"))
                                .andExpect(unauthenticated())
                                .andExpect(redirectedUrl("/"));
        }

        // ===== ACCESS CONTROL TESTS =====

        @Test
        @DisplayName("Unauthenticated users are redirected to login")
        void testUnauthenticatedAccess() throws Exception {
                mockMvc.perform(get("/admin/users"))
                                .andExpect(status().is3xxRedirection());
        }

        @Test
        @WithMockUser(username = "parent", roles = "PARENT")
        @DisplayName("Parent cannot access admin endpoints")
        void testParentAccessToAdmin() throws Exception {
                mockMvc.perform(get("/admin/users"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "child", roles = "CHILD")
        @DisplayName("Child cannot access parent endpoints")
        void testChildAccessToParent() throws Exception {
                mockMvc.perform(get("/parent/family"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "child", roles = "CHILD")
        @DisplayName("Child cannot access admin endpoints")
        void testChildAccessToAdmin() throws Exception {
                mockMvc.perform(get("/admin/users"))
                                .andExpect(status().isForbidden());
        }

        // ===== PUBLIC RESOURCES TESTS =====

        @Test
        @DisplayName("Public resources are accessible without authentication")
        void testPublicResources() throws Exception {
                mockMvc.perform(get("/css/main.css"))
                                .andExpect(result -> {
                                        int status = result.getResponse().getStatus();
                                        // Should return 200 or 404, but NOT 401/403 (not protected)
                                        if (status == 401 || status == 403) {
                                                throw new AssertionError("Public resource is protected: " + status);
                                        }
                                });

                mockMvc.perform(get("/js/main.js"))
                                .andExpect(result -> {
                                        int status = result.getResponse().getStatus();
                                        if (status == 401 || status == 403) {
                                                throw new AssertionError("Public resource is protected: " + status);
                                        }
                                });
        }

        // ===== API ENDPOINT TESTS =====

        @Test
        @DisplayName("API avatar endpoint is public")
        void testPublicApiEndpoint() throws Exception {
                mockMvc.perform(get("/api/avatar/icons"))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        @DisplayName("Authenticated user can access public API")
        void testAuthenticatedAccessToPublicApi() throws Exception {
                mockMvc.perform(get("/api/avatar/icons"))
                                .andExpect(status().isOk());
        }
}
