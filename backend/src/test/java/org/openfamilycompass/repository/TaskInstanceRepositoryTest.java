package org.openfamilycompass.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.openfamilycompass.model.TaskDefinition;
import org.openfamilycompass.model.TaskInstance;
import org.openfamilycompass.model.TaskStatus;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TaskInstanceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskInstanceRepository taskInstanceRepository;

    @Test
    void findByAssignedUser_ShouldReturnTaskInstancesAssignedToUser() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        User child1 = createUser("child1", UserRole.CHILD);
        User child2 = createUser("child2", UserRole.CHILD);
        entityManager.persist(parent);
        entityManager.persist(child1);
        entityManager.persist(child2);

        TaskDefinition taskDef = createTaskDefinition("Test Task", parent);
        entityManager.persist(taskDef);

        TaskInstance task1 = createTaskInstance(taskDef, child1, LocalDate.now());
        TaskInstance task2 = createTaskInstance(taskDef, child1, LocalDate.now().plusDays(1));
        TaskInstance task3 = createTaskInstance(taskDef, child2, LocalDate.now());
        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        // When
        List<TaskInstance> child1Tasks = taskInstanceRepository.findByAssignedUser(child1);

        // Then
        assertThat(child1Tasks).hasSize(2);
        assertThat(child1Tasks).extracting(TaskInstance::getAssignedUser)
                .allMatch(user -> user.equals(child1));
    }

    @Test
    void findByAssignedUserAndStatus_ShouldReturnTaskInstancesWithSpecificStatus() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        User child = createUser("child", UserRole.CHILD);
        entityManager.persist(parent);
        entityManager.persist(child);

        TaskDefinition taskDef = createTaskDefinition("Test Task", parent);
        entityManager.persist(taskDef);

        TaskInstance pendingTask = createTaskInstance(taskDef, child, LocalDate.now());
        pendingTask.setStatus(TaskStatus.PENDING);
        TaskInstance completedTask = createTaskInstance(taskDef, child, LocalDate.now().plusDays(1));
        completedTask.setStatus(TaskStatus.CHILD_COMPLETED);
        entityManager.persist(pendingTask);
        entityManager.persist(completedTask);
        entityManager.flush();

        // When
        List<TaskInstance> pendingTasks = taskInstanceRepository.findByAssignedUserAndStatus(child, TaskStatus.PENDING);

        // Then
        assertThat(pendingTasks).hasSize(1);
        assertThat(pendingTasks.get(0).getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(pendingTasks.get(0).getAssignedUser()).isEqualTo(child);
    }

    @Test
    void findByStatus_ShouldReturnTaskInstancesWithSpecificStatus() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        User child1 = createUser("child1", UserRole.CHILD);
        User child2 = createUser("child2", UserRole.CHILD);
        entityManager.persist(parent);
        entityManager.persist(child1);
        entityManager.persist(child2);

        TaskDefinition taskDef = createTaskDefinition("Test Task", parent);
        entityManager.persist(taskDef);

        TaskInstance pendingTask = createTaskInstance(taskDef, child1, LocalDate.now());
        pendingTask.setStatus(TaskStatus.PENDING);
        TaskInstance completedTask = createTaskInstance(taskDef, child2, LocalDate.now());
        completedTask.setStatus(TaskStatus.CHILD_COMPLETED);
        entityManager.persist(pendingTask);
        entityManager.persist(completedTask);
        entityManager.flush();

        // When
        List<TaskInstance> pendingTasks = taskInstanceRepository.findByStatus(TaskStatus.PENDING);

        // Then
        assertThat(pendingTasks).hasSize(1);
        assertThat(pendingTasks.get(0).getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void findByAssignedUserAndStatusIn_ShouldReturnTaskInstancesWithStatusesInList() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        User child = createUser("child", UserRole.CHILD);
        entityManager.persist(parent);
        entityManager.persist(child);

        TaskDefinition taskDef = createTaskDefinition("Test Task", parent);
        entityManager.persist(taskDef);

        TaskInstance pendingTask = createTaskInstance(taskDef, child, LocalDate.now());
        pendingTask.setStatus(TaskStatus.PENDING);
        TaskInstance inProgressTask = createTaskInstance(taskDef, child, LocalDate.now().plusDays(1));
        inProgressTask.setStatus(TaskStatus.IN_PROGRESS);
        TaskInstance completedTask = createTaskInstance(taskDef, child, LocalDate.now().plusDays(2));
        completedTask.setStatus(TaskStatus.CHILD_COMPLETED);
        entityManager.persist(pendingTask);
        entityManager.persist(inProgressTask);
        entityManager.persist(completedTask);
        entityManager.flush();

        // When
        List<TaskInstance> activeTasks = taskInstanceRepository.findByAssignedUserAndStatusIn(
                child, List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS));

        // Then
        assertThat(activeTasks).hasSize(2);
        assertThat(activeTasks).extracting(TaskInstance::getStatus)
                .containsExactlyInAnyOrder(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
    }

    @Test
    void findPendingApprovalForUser_ShouldReturnChildCompletedTasksForUser() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        User child = createUser("child", UserRole.CHILD);
        entityManager.persist(parent);
        entityManager.persist(child);

        TaskDefinition taskDef = createTaskDefinition("Test Task", parent);
        entityManager.persist(taskDef);

        TaskInstance pendingTask = createTaskInstance(taskDef, child, LocalDate.now());
        pendingTask.setStatus(TaskStatus.PENDING);
        TaskInstance completedTask = createTaskInstance(taskDef, child, LocalDate.now().plusDays(1));
        completedTask.setStatus(TaskStatus.CHILD_COMPLETED);
        TaskInstance approvedTask = createTaskInstance(taskDef, child, LocalDate.now().plusDays(2));
        approvedTask.setStatus(TaskStatus.APPROVED);
        entityManager.persist(pendingTask);
        entityManager.persist(completedTask);
        entityManager.persist(approvedTask);
        entityManager.flush();

        // When
        List<TaskInstance> pendingApproval = taskInstanceRepository.findPendingApprovalForUser(child);

        // Then
        assertThat(pendingApproval).hasSize(1);
        assertThat(pendingApproval.get(0).getStatus()).isEqualTo(TaskStatus.CHILD_COMPLETED);
        assertThat(pendingApproval.get(0).getAssignedUser()).isEqualTo(child);
    }

    @Test
    void findByDueDateBeforeAndStatusNotIn_ShouldReturnOverdueTasksExcludingStatuses() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        User child = createUser("child", UserRole.CHILD);
        entityManager.persist(parent);
        entityManager.persist(child);

        TaskDefinition taskDef = createTaskDefinition("Test Task", parent);
        entityManager.persist(taskDef);

        LocalDate today = LocalDate.now();
        TaskInstance overduePending = createTaskInstance(taskDef, child, today.minusDays(1));
        overduePending.setStatus(TaskStatus.PENDING);
        TaskInstance overdueInProgress = createTaskInstance(taskDef, child, today.minusDays(2));
        overdueInProgress.setStatus(TaskStatus.IN_PROGRESS);
        TaskInstance completedTask = createTaskInstance(taskDef, child, today.minusDays(3));
        completedTask.setStatus(TaskStatus.CHILD_COMPLETED);
        entityManager.persist(overduePending);
        entityManager.persist(overdueInProgress);
        entityManager.persist(completedTask);
        entityManager.flush();

        // When
        List<TaskInstance> overdueTasks = taskInstanceRepository.findByDueDateBeforeAndStatusNotIn(
                today, List.of(TaskStatus.CHILD_COMPLETED, TaskStatus.APPROVED, TaskStatus.REJECTED));

        // Then
        assertThat(overdueTasks).hasSize(2);
        assertThat(overdueTasks).extracting(TaskInstance::getStatus)
                .containsExactlyInAnyOrder(TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
    }

    @Test
    void findByStatusAndDueDateBefore_ShouldReturnTasksWithStatusAndOverdue() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        User child = createUser("child", UserRole.CHILD);
        entityManager.persist(parent);
        entityManager.persist(child);

        TaskDefinition taskDef = createTaskDefinition("Test Task", parent);
        entityManager.persist(taskDef);

        LocalDate today = LocalDate.now();
        TaskInstance overduePending = createTaskInstance(taskDef, child, today.minusDays(1));
        overduePending.setStatus(TaskStatus.PENDING);
        TaskInstance futurePending = createTaskInstance(taskDef, child, today.plusDays(1));
        futurePending.setStatus(TaskStatus.PENDING);
        TaskInstance overdueCompleted = createTaskInstance(taskDef, child, today.minusDays(2));
        overdueCompleted.setStatus(TaskStatus.CHILD_COMPLETED);
        entityManager.persist(overduePending);
        entityManager.persist(futurePending);
        entityManager.persist(overdueCompleted);
        entityManager.flush();

        // When
        List<TaskInstance> overduePendingTasks = taskInstanceRepository.findByStatusAndDueDateBefore(
                TaskStatus.PENDING, today);

        // Then
        assertThat(overduePendingTasks).hasSize(1);
        assertThat(overduePendingTasks.get(0).getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(overduePendingTasks.get(0).getDueDate()).isBefore(today);
    }

    @Test
    void findById_ShouldReturnTaskInstance_WhenExists() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        User child = createUser("child", UserRole.CHILD);
        entityManager.persist(parent);
        entityManager.persist(child);

        TaskDefinition taskDef = createTaskDefinition("Test Task", parent);
        entityManager.persist(taskDef);

        TaskInstance task = createTaskInstance(taskDef, child, LocalDate.now());
        TaskInstance saved = entityManager.persistAndFlush(task);

        // When
        Optional<TaskInstance> found = taskInstanceRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getAssignedUser()).isEqualTo(child);
        assertThat(found.get().getTaskDefinition()).isEqualTo(taskDef);
    }

    @Test
    void save_ShouldPersistTaskInstance() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        User child = createUser("child", UserRole.CHILD);
        entityManager.persist(parent);
        entityManager.persist(child);

        TaskDefinition taskDef = createTaskDefinition("Test Task", parent);
        entityManager.persist(taskDef);

        TaskInstance task = createTaskInstance(taskDef, child, LocalDate.now());

        // When
        TaskInstance saved = taskInstanceRepository.save(task);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(entityManager.find(TaskInstance.class, saved.getId())).isNotNull();
    }

    @Test
    void deleteByTaskDefinition_Id_ShouldDeleteAllTaskInstancesForDefinition() {
        // Given
        User parent = createUser("parent", UserRole.PARENT);
        User child1 = createUser("child1", UserRole.CHILD);
        User child2 = createUser("child2", UserRole.CHILD);
        entityManager.persist(parent);
        entityManager.persist(child1);
        entityManager.persist(child2);

        TaskDefinition taskDef1 = createTaskDefinition("Task 1", parent);
        TaskDefinition taskDef2 = createTaskDefinition("Task 2", parent);
        entityManager.persist(taskDef1);
        entityManager.persist(taskDef2);

        TaskInstance task1 = createTaskInstance(taskDef1, child1, LocalDate.now());
        TaskInstance task2 = createTaskInstance(taskDef1, child2, LocalDate.now());
        TaskInstance task3 = createTaskInstance(taskDef2, child1, LocalDate.now());
        entityManager.persist(task1);
        entityManager.persist(task2);
        entityManager.persist(task3);
        entityManager.flush();

        assertThat(taskInstanceRepository.findAll()).hasSize(3);

        // When
        taskInstanceRepository.deleteByTaskDefinition_Id(taskDef1.getId());

        // Then
        List<TaskInstance> remainingTasks = (List<TaskInstance>) taskInstanceRepository.findAll();
        assertThat(remainingTasks).hasSize(1);
        assertThat(remainingTasks.get(0).getTaskDefinition()).isEqualTo(taskDef2);
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
        task.setCreatedBy(createdBy);
        return task;
    }

    private TaskInstance createTaskInstance(TaskDefinition taskDefinition, User assignedUser, LocalDate dueDate) {
        TaskInstance task = new TaskInstance();
        task.setTaskDefinition(taskDefinition);
        task.setAssignedUser(assignedUser);
        task.setDueDate(dueDate);
        task.setStatus(TaskStatus.PENDING);
        return task;
    }
}