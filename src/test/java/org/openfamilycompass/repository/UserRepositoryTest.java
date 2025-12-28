package org.openfamilycompass.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsername_ShouldReturnUser_WhenExists() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("encoded-password");
        user.setRole(UserRole.PARENT);
        entityManager.persistAndFlush(user);

        // When
        Optional<User> found = userRepository.findByUsername("testuser");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void findByUsername_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<User> found = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void findByRole_ShouldReturnUsersWithRole() {
        // Given
        User parent1 = createUser("parent1", UserRole.PARENT);
        User parent2 = createUser("parent2", UserRole.PARENT);
        User admin = createUser("admin", UserRole.ADMIN);
        entityManager.persist(parent1);
        entityManager.persist(parent2);
        entityManager.persist(admin);
        entityManager.flush();

        // When
        List<User> parents = userRepository.findByRole(UserRole.PARENT);

        // Then
        assertThat(parents).hasSize(2);
        assertThat(parents).extracting(User::getUsername)
                .containsExactlyInAnyOrder("parent1", "parent2");
    }

    @Test
    void save_ShouldPersistUser() {
        // Given
        User user = createUser("newuser", UserRole.CHILD);

        // When
        User saved = userRepository.save(user);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(entityManager.find(User.class, saved.getId())).isNotNull();
    }

    private User createUser(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("password");
        user.setRole(role);
        user.setActive(true);
        return user;
    }
}
