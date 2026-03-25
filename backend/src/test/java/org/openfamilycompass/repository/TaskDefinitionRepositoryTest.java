package org.openfamilycompass.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.openfamilycompass.model.RecurrenceType;
import org.openfamilycompass.model.TaskDefinition;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TaskDefinitionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskDefinitionRepository taskDefinitionRepository;

    @Test
    void findByCreatedBy_ShouldReturnTaskDefinitionsCreatedByUser() {
        // Given
        User parent1 = createUser("parent1", UserRole.PARENT);
        User parent2 = createUser("parent2", UserRole.PARENT);
        entityManager.persist(parent1);
        entityManager.persist(parent2);

        TaskDefinition task1 = createTaskDefinition("Task 1", parent1);
        TaskDefinition task2 = createTaskDefinition("Task 2", parent1);
        TaskDefinition task3 = createTaskDefinition("Task 3", parent2);
        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        // When
        List<TaskDefinition> parent1Tasks = taskDefinitionRepository.findByCreatedBy(parent1);

        // Then
        assertThat(parent1Tasks).hasSize(2);
        assertThat(parent1Tasks).extracting(TaskDefinition::getTitle)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
    }

    @Test
    void findByCreatedBy_ShouldReturnEmptyList_WhenNoTasksCreatedByUser() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        entityManager.persist(parent);
        entityManager.flush();

        // When
        List<TaskDefinition> tasks = taskDefinitionRepository.findByCreatedBy(parent);

        // Then
        assertThat(tasks).isEmpty();
    }

    @Test
    void findByAssignedUsersContaining_ShouldReturnTaskDefinitionsAssignedToUser() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        User child1 = createUser("child1", UserRole.CHILD);
        User child2 = createUser("child2", UserRole.CHILD);
        entityManager.persist(parent);
        entityManager.persist(child1);
        entityManager.persist(child2);

        TaskDefinition task1 = createTaskDefinition("Task 1", parent);
        task1.setAssignedUsers(Set.of(child1));
        TaskDefinition task2 = createTaskDefinition("Task 2", parent);
        task2.setAssignedUsers(Set.of(child1, child2));
        TaskDefinition task3 = createTaskDefinition("Task 3", parent);
        task3.setAssignedUsers(Set.of(child2));
        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        // When
        List<TaskDefinition> child1Tasks = taskDefinitionRepository.findByAssignedUsersContaining(child1);

        // Then
        assertThat(child1Tasks).hasSize(2);
        assertThat(child1Tasks).extracting(TaskDefinition::getTitle)
                .containsExactlyInAnyOrder("Task 1", "Task 2");
    }

    @Test
    void findByAssignedUsersContaining_ShouldReturnEmptyList_WhenUserNotAssignedToAnyTasks() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        User child = createUser("child", UserRole.CHILD);
        entityManager.persist(parent);
        entityManager.persist(child);

        TaskDefinition task = createTaskDefinition("Task 1", parent);
        task.setAssignedUsers(new HashSet<>()); // Empty set
        entityManager.persist(task);
        entityManager.flush();

        // When
        List<TaskDefinition> tasks = taskDefinitionRepository.findByAssignedUsersContaining(child);

        // Then
        assertThat(tasks).isEmpty();
    }

    @Test
    void findById_ShouldReturnTaskDefinition_WhenExists() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        entityManager.persist(parent);

        TaskDefinition task = createTaskDefinition("Test Task", parent);
        TaskDefinition savedTask = entityManager.persistAndFlush(task);

        // When
        Optional<TaskDefinition> found = taskDefinitionRepository.findById(savedTask.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Task");
        assertThat(found.get().getCreatedBy()).isEqualTo(parent);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<TaskDefinition> found = taskDefinitionRepository.findById(999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void save_ShouldPersistTaskDefinition() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        entityManager.persist(parent);

        TaskDefinition task = createTaskDefinition("New Task", parent);

        // When
        TaskDefinition saved = taskDefinitionRepository.save(task);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(entityManager.find(TaskDefinition.class, saved.getId())).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("New Task");
        assertThat(saved.getCreatedBy()).isEqualTo(parent);
    }

    @Test
    void save_ShouldUpdateExistingTaskDefinition() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        entityManager.persist(parent);

        TaskDefinition task = createTaskDefinition("Original Task", parent);
        TaskDefinition saved = entityManager.persistAndFlush(task);

        // When
        saved.setTitle("Updated Task");
        saved.setDescription("Updated Description");
        TaskDefinition updated = taskDefinitionRepository.save(saved);

        // Then
        assertThat(updated.getTitle()).isEqualTo("Updated Task");
        assertThat(updated.getDescription()).isEqualTo("Updated Description");
        TaskDefinition found = entityManager.find(TaskDefinition.class, saved.getId());
        assertThat(found.getTitle()).isEqualTo("Updated Task");
    }

    @Test
    void findAll_ShouldReturnAllTaskDefinitions() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        entityManager.persist(parent);

        TaskDefinition task1 = createTaskDefinition("Task 1", parent);
        TaskDefinition task2 = createTaskDefinition("Task 2", parent);
        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.flush();

        // When
        List<TaskDefinition> allTasks = (List<TaskDefinition>) taskDefinitionRepository.findAll();

        // Then
        assertThat(allTasks).hasSizeGreaterThanOrEqualTo(2);
        assertThat(allTasks).extracting(TaskDefinition::getTitle)
                .contains("Task 1", "Task 2");
    }

    @Test
    void existsById_ShouldReturnTrue_WhenTaskDefinitionExists() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        entityManager.persist(parent);

        TaskDefinition task = createTaskDefinition("Test Task", parent);
        TaskDefinition saved = entityManager.persistAndFlush(task);

        // When
        boolean exists = taskDefinitionRepository.existsById(saved.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsById_ShouldReturnFalse_WhenTaskDefinitionDoesNotExist() {
        // When
        boolean exists = taskDefinitionRepository.existsById(999L);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void deleteById_ShouldRemoveTaskDefinition() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        entityManager.persist(parent);

        TaskDefinition task = createTaskDefinition("Task to Delete", parent);
        TaskDefinition saved = entityManager.persistAndFlush(task);
        assertThat(taskDefinitionRepository.existsById(saved.getId())).isTrue();

        // When
        taskDefinitionRepository.deleteById(saved.getId());

        // Then
        assertThat(taskDefinitionRepository.existsById(saved.getId())).isFalse();
    }

    private User createUser(String username, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setFirstName(username.substring(0, 1).toUpperCase() + username.substring(1));
        user.setPassword("password");
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private TaskDefinition createTaskDefinition(String title, User createdBy) {
        TaskDefinition task = new TaskDefinition();
        task.setTitle(title);
        task.setDescription("Test Description");
        task.setBasePoints(10);
        task.setRecurrenceType(RecurrenceType.ONCE);
        task.setCreatedBy(createdBy);
        task.setStartDate(LocalDate.now());
        task.setSeriesEndDate(LocalDate.now().plusDays(7));
        return task;
    }
}