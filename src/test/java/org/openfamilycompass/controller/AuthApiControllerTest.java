package org.openfamilycompass.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openfamilycompass.dto.LoginRequest;
import org.openfamilycompass.dto.RefreshTokenRequest;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.security.JwtTokenService;
import org.openfamilycompass.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AuthApiController.class)
class AuthApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(UserRole.PARENT);
    }

    @Test
    void login_ShouldReturnTokens_WhenCredentialsValid() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("1234");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userService.loadUserByUsername("testuser")).thenReturn(testUser);
        when(jwtTokenService.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtTokenService.generateRefreshToken(testUser)).thenReturn("refresh-token");

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenCredentialsInvalid() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("wrongpin");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_ShouldReturnNewAccessToken_WhenRefreshTokenValid() throws Exception {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("valid-refresh-token");

        when(jwtTokenService.isRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenService.extractUsername("valid-refresh-token")).thenReturn("testuser");
        when(userService.loadUserByUsername("testuser")).thenReturn(testUser);
        when(jwtTokenService.validateToken("valid-refresh-token", testUser)).thenReturn(true);
        when(jwtTokenService.generateAccessToken(testUser)).thenReturn("new-access-token");

        // When/Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void refresh_ShouldReturnUnauthorized_WhenTokenNotRefreshToken() throws Exception {
        // Given
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken("access-token");

        when(jwtTokenService.isRefreshToken("access-token")).thenReturn(false);

        // When/Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }
}
