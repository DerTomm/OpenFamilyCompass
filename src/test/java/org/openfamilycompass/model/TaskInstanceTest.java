package org.openfamilycompass.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskInstanceTest {

    private TaskInstance taskInstance;
    private TaskDefinition taskDefinition;
    private User assignedUser;
    private User approvedBy;

    @BeforeEach
    void setUp() {
        taskDefinition = new TaskDefinition();
        taskDefinition.setId(1L);
        taskDefinition.setTitle("Test Task Definition");

        assignedUser = new User();
        assignedUser.setId(2L);
        assignedUser.setUsername("child");

        approvedBy = new User();
        approvedBy.setId(3L);
        approvedBy.setUsername("parent");

        taskInstance = new TaskInstance();
        taskInstance.setId(1L);
        taskInstance.setTaskDefinition(taskDefinition);
        taskInstance.setAssignedUser(assignedUser);
        taskInstance.setDueDate(LocalDate.now().plusDays(1));
        taskInstance.setStatus(TaskStatus.PENDING);
        taskInstance.setCompletedAt(null);
        taskInstance.setApprovedAt(null);
        taskInstance.setAwardedPoints(null);
        taskInstance.setApprovedBy(null);
        taskInstance.setParentNotes(null);
    }

    @Test
    void constructor_ShouldCreateTaskInstance() {
        // Given
        TaskInstance newTask = new TaskInstance();

        // Then
        assertThat(newTask).isNotNull();
        assertThat(newTask.getStatus()).isEqualTo(TaskStatus.PENDING); // Default value
    }

    @Test
    void noArgsConstructor_ShouldCreateEmptyTaskInstance() {
        // When
        TaskInstance newTask = new TaskInstance();

        // Then
        assertThat(newTask.getId()).isNull();
        assertThat(newTask.getTaskDefinition()).isNull();
        assertThat(newTask.getAssignedUser()).isNull();
        assertThat(newTask.getDueDate()).isNull();
        assertThat(newTask.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(newTask.getCompletedAt()).isNull();
        assertThat(newTask.getApprovedAt()).isNull();
        assertThat(newTask.getAwardedPoints()).isNull();
        assertThat(newTask.getApprovedBy()).isNull();
        assertThat(newTask.getParentNotes()).isNull();
        assertThat(newTask.getCreatedAt()).isNull();
    }

    @Test
    void allArgsConstructor_ShouldCreateFullyPopulatedTaskInstance() {
        // Given
        Long id = 1L;
        TaskDefinition definition = taskDefinition;
        User user = assignedUser;
        LocalDate dueDate = LocalDate.now().plusDays(3);
        TaskStatus status = TaskStatus.APPROVED;
        LocalDateTime completedAt = LocalDateTime.now().minusHours(2);
        LocalDateTime approvedAt = LocalDateTime.now().minusHours(1);
        Integer awardedPoints = 15;
        User approver = approvedBy;
        String parentNotes = "Well done!";
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);

        // When
        TaskInstance task = new TaskInstance(id, definition, user, dueDate, status,
                completedAt, approvedAt, awardedPoints, approver, parentNotes, createdAt);

        // Then
        assertThat(task.getId()).isEqualTo(id);
        assertThat(task.getTaskDefinition()).isEqualTo(definition);
        assertThat(task.getAssignedUser()).isEqualTo(user);
        assertThat(task.getDueDate()).isEqualTo(dueDate);
        assertThat(task.getStatus()).isEqualTo(status);
        assertThat(task.getCompletedAt()).isEqualTo(completedAt);
        assertThat(task.getApprovedAt()).isEqualTo(approvedAt);
        assertThat(task.getAwardedPoints()).isEqualTo(awardedPoints);
        assertThat(task.getApprovedBy()).isEqualTo(approver);
        assertThat(task.getParentNotes()).isEqualTo(parentNotes);
        assertThat(task.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void onCreate_ShouldSetCreatedAt() {
        // Given
        TaskInstance newTask = new TaskInstance();
        newTask.setTaskDefinition(taskDefinition);
        newTask.setAssignedUser(assignedUser);

        // When
        newTask.onCreate();

        // Then
        assertThat(newTask.getCreatedAt()).isNotNull();
        assertThat(newTask.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        // Given
        LocalDate newDueDate = LocalDate.of(2024, 12, 31);
        TaskStatus newStatus = TaskStatus.APPROVED;
        LocalDateTime completedAt = LocalDateTime.now();
        LocalDateTime approvedAt = LocalDateTime.now().plusHours(1);
        Integer awardedPoints = 25;
        String parentNotes = "Excellent work!";

        // When
        taskInstance.setDueDate(newDueDate);
        taskInstance.setStatus(newStatus);
        taskInstance.setCompletedAt(completedAt);
        taskInstance.setApprovedAt(approvedAt);
        taskInstance.setAwardedPoints(awardedPoints);
        taskInstance.setApprovedBy(approvedBy);
        taskInstance.setParentNotes(parentNotes);

        // Then
        assertThat(taskInstance.getDueDate()).isEqualTo(newDueDate);
        assertThat(taskInstance.getStatus()).isEqualTo(newStatus);
        assertThat(taskInstance.getCompletedAt()).isEqualTo(completedAt);
        assertThat(taskInstance.getApprovedAt()).isEqualTo(approvedAt);
        assertThat(taskInstance.getAwardedPoints()).isEqualTo(awardedPoints);
        assertThat(taskInstance.getApprovedBy()).isEqualTo(approvedBy);
        assertThat(taskInstance.getParentNotes()).isEqualTo(parentNotes);
    }

    @Test
    void status_ShouldDefaultToPending() {
        // When
        TaskInstance newTask = new TaskInstance();

        // Then
        assertThat(newTask.getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void taskInstanceLifecycle_ShouldHandleStatusChanges() {
        // Given - Initial state
        assertThat(taskInstance.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(taskInstance.getCompletedAt()).isNull();
        assertThat(taskInstance.getApprovedAt()).isNull();
        assertThat(taskInstance.getAwardedPoints()).isNull();

        // When - Child completes task
        taskInstance.setStatus(TaskStatus.CHILD_COMPLETED);
        taskInstance.setCompletedAt(LocalDateTime.now());

        // Then
        assertThat(taskInstance.getStatus()).isEqualTo(TaskStatus.CHILD_COMPLETED);
        assertThat(taskInstance.getCompletedAt()).isNotNull();

        // When - Parent approves task
        taskInstance.setStatus(TaskStatus.APPROVED);
        taskInstance.setApprovedAt(LocalDateTime.now());
        taskInstance.setAwardedPoints(20);
        taskInstance.setApprovedBy(approvedBy);
        taskInstance.setParentNotes("Great job!");

        // Then
        assertThat(taskInstance.getStatus()).isEqualTo(TaskStatus.APPROVED);
        assertThat(taskInstance.getApprovedAt()).isNotNull();
        assertThat(taskInstance.getAwardedPoints()).isEqualTo(20);
        assertThat(taskInstance.getApprovedBy()).isEqualTo(approvedBy);
        assertThat(taskInstance.getParentNotes()).isEqualTo("Great job!");
    }

    @Test
    void equalsAndHashCode_ShouldWorkBasedOnId() {
        // Given
        TaskInstance task1 = new TaskInstance();
        task1.setId(1L);

        TaskInstance task2 = new TaskInstance();
        task2.setId(1L);

        TaskInstance task3 = new TaskInstance();
        task3.setId(2L);

        // Then
        assertThat(task1).isEqualTo(task2);
        assertThat(task1).isNotEqualTo(task3);
        assertThat(task1.hashCode()).isEqualTo(task2.hashCode());
        assertThat(task1.hashCode()).isNotEqualTo(task3.hashCode());
    }

    @Test
    void toString_ShouldContainKeyFields() {
        // When
        String toString = taskInstance.toString();

        // Then
        assertThat(toString).contains("TaskInstance");
        assertThat(toString).contains("id=1");
        assertThat(toString).contains("status=PENDING");
    }
}