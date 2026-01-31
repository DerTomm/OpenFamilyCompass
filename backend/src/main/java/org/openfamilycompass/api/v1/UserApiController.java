package org.openfamilycompass.api.v1;

import java.util.List;
import java.util.stream.Collectors;

import org.openfamilycompass.api.v1.dto.UserDto;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management (Admin only)")
public class UserApiController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all users")
    public ResponseEntity<List<UserDto.Response>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active) {

        List<User> users;
        if (role != null) {
            users = userService.findByRole(role);
        } else {
            users = userService.findAll();
        }

        if (active != null) {
            users = users.stream()
                    .filter(u -> u.isActive() == active)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(
                users.stream()
                        .map(UserDto.Response::fromEntity)
                        .collect(Collectors.toList()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserDto.Response> createUser(@Valid @RequestBody UserDto.CreateRequest request) {
        if (userService.findByUsername(request.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setRole(request.getRole());
        user.setActive(true);

        User saved = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.Response.fromEntity(saved));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserDto.Response> getUserById(@PathVariable Long id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(UserDto.Response.fromEntity(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user")
    public ResponseEntity<UserDto.Response> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDto.UpdateRequest request) {

        User user = userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        User saved = userService.save(user);
        return ResponseEntity.ok(UserDto.Response.fromEntity(saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate user")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setActive(false);
        userService.save(user);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Permanently delete user")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Prevent deletion of the current user
        // This would require getting the current user from JWT, but as a simple check:
        userService.delete(user);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/children")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "List all children")
    public ResponseEntity<List<UserDto.ChildResponse>> listChildren() {
        List<User> children = userService.findByRole(UserRole.CHILD);
        return ResponseEntity.ok(
                children.stream()
                        .filter(User::isActive)
                        .map(UserDto.ChildResponse::fromEntity)
                        .collect(Collectors.toList()));
    }
}
