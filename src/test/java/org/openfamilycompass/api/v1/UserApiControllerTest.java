package org.openfamilycompass.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openfamilycompass.api.v1.dto.UserDto;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserApiControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserApiController userApiController;

    private User adminUser;
    private User parentUser;
    private User childUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setFirstName("Admin");
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setActive(true);
        adminUser.setCreatedAt(LocalDateTime.now());

        parentUser = new User();
        parentUser.setId(2L);
        parentUser.setUsername("parent");
        parentUser.setFirstName("Parent");
        parentUser.setRole(UserRole.PARENT);
        parentUser.setActive(true);
        parentUser.setCreatedAt(LocalDateTime.now());

        childUser = new User();
        childUser.setId(3L);
        childUser.setUsername("child");
        childUser.setFirstName("Child");
        childUser.setRole(UserRole.CHILD);
        childUser.setActive(true);
        childUser.setTotalPoints(100);
        childUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void listUsers_ShouldReturnAllUsers() {
        when(userService.findAll()).thenReturn(List.of(adminUser, parentUser, childUser));

        ResponseEntity<List<UserDto.Response>> response = userApiController.listUsers(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
        assertThat(response.getBody().get(0).getUsername()).isEqualTo("admin");
        assertThat(response.getBody().get(1).getUsername()).isEqualTo("parent");
        assertThat(response.getBody().get(2).getUsername()).isEqualTo("child");
    }

    @Test
    void listUsers_WithRoleFilter_ShouldReturnFilteredUsers() {
        when(userService.findByRole(UserRole.CHILD)).thenReturn(List.of(childUser));

        ResponseEntity<List<UserDto.Response>> response = userApiController.listUsers(UserRole.CHILD, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getRole()).isEqualTo(UserRole.CHILD);
    }

    @Test
    void getUserById_ShouldReturnUser() {
        when(userService.findById(3L)).thenReturn(Optional.of(childUser));

        ResponseEntity<UserDto.Response> response = userApiController.getUserById(3L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(3L);
        assertThat(response.getBody().getUsername()).isEqualTo("child");
        assertThat(response.getBody().getTotalPoints()).isEqualTo(100);
    }

    @Test
    void createUser_ShouldCreateUser() {
        when(userService.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userService.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            user.setCreatedAt(LocalDateTime.now());
            return user;
        });

        UserDto.CreateRequest request = new UserDto.CreateRequest();
        request.setUsername("newuser");
        request.setPassword("test1234");
        request.setFirstName("New User");
        request.setRole(UserRole.CHILD);

        ResponseEntity<UserDto.Response> response = userApiController.createUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getUsername()).isEqualTo("newuser");
        assertThat(response.getBody().getFirstName()).isEqualTo("New User");
        assertThat(response.getBody().getRole()).isEqualTo(UserRole.CHILD);
    }

    @Test
    void listChildren_ShouldReturnOnlyChildren() {
        when(userService.findByRole(UserRole.CHILD)).thenReturn(List.of(childUser));

        ResponseEntity<List<UserDto.ChildResponse>> response = userApiController.listChildren();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getFirstName()).isEqualTo("Child");
        assertThat(response.getBody().get(0).getTotalPoints()).isEqualTo(100);
    }
}
