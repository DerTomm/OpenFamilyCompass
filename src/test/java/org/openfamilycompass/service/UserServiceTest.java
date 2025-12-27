package org.openfamilycompass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPin("encodedPin");
        testUser.setRole(UserRole.PARENT);
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        UserDetails userDetails = userService.loadUserByUsername("testuser");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPin");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void createUser_ShouldEncodePasswordAndSaveUser() {
        // Given
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.createUser("testuser", "rawPassword", UserRole.PARENT);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        verify(passwordEncoder).encode("rawPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void findByUsername_ShouldReturnUser_WhenExists() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.findByUsername("testuser");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void updatePin_ShouldUpdateUserPin() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPin")).thenReturn("newEncodedPin");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userService.updatePin(1L, "newPin");

        // Then
        assertThat(result.getPin()).isEqualTo("newEncodedPin");
        verify(passwordEncoder).encode("newPin");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deactivateUser_ShouldSetActiveToFalse() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        userService.deactivateUser(1L);

        // Then
        assertThat(testUser.isActive()).isFalse();
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }
}
