package org.openfamilycompass.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskDefinitionTest {

    private TaskDefinition taskDefinition;
    private User createdBy;
    private User assignedUser;

    @BeforeEach
    void setUp() {
        createdBy = new User();
        createdBy.setId(1L);
        createdBy.setUsername("parent");

        assignedUser = new User();
        assignedUser.setId(2L);
        assignedUser.setUsername("child");

        taskDefinition = new TaskDefinition();
        taskDefinition.setId(1L);
        taskDefinition.setTitle("Test Task");
        taskDefinition.setDescription("Test Description");
        taskDefinition.setBasePoints(10);
        taskDefinition.setRecurrenceType(RecurrenceType.ONCE);
        taskDefinition.setCreatedBy(createdBy);
        taskDefinition.setStartDate(LocalDate.now());
        taskDefinition.setEndDate(LocalDate.now().plusDays(7));
        taskDefinition.setWeeklyDays("MONDAY,TUESDAY");

        Set<User> assignedUsers = new HashSet<>();
        assignedUsers.add(assignedUser);
        taskDefinition.setAssignedUsers(assignedUsers);
    }

    @Test
    void constructor_ShouldCreateTaskDefinition() {
        // Given
        TaskDefinition newTask = new TaskDefinition();

        // Then
        assertThat(newTask).isNotNull();
        assertThat(newTask.getRecurrenceType()).isEqualTo(RecurrenceType.ONCE); // Default value
    }

    @Test
    void noArgsConstructor_ShouldCreateEmptyTaskDefinition() {
        // When
        TaskDefinition newTask = new TaskDefinition();

        // Then
        assertThat(newTask.getId()).isNull();
        assertThat(newTask.getTitle()).isNull();
        assertThat(newTask.getDescription()).isNull();
        assertThat(newTask.getBasePoints()).isEqualTo(0);
        assertThat(newTask.getRecurrenceType()).isEqualTo(RecurrenceType.ONCE);
        assertThat(newTask.getAssignedUsers()).isNull();
        assertThat(newTask.getCreatedBy()).isNull();
        assertThat(newTask.getStartDate()).isNull();
        assertThat(newTask.getEndDate()).isNull();
        assertThat(newTask.getWeeklyDays()).isNull();
        assertThat(newTask.getCreatedAt()).isNull();
    }

    @Test
    void allArgsConstructor_ShouldCreateFullyPopulatedTaskDefinition() {
        // Given
        Long id = 1L;
        String title = "Test Task";
        String description = "Test Description";
        int basePoints = 10;
        RecurrenceType recurrenceType = RecurrenceType.WEEKLY;
        Set<User> assignedUsers = Set.of(assignedUser);
        User creator = createdBy;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(7);
        String weeklyDays = "MONDAY";

        // When
        TaskDefinition task = new TaskDefinition(id, title, description, basePoints, recurrenceType,
                assignedUsers, creator, startDate, endDate, weeklyDays, LocalDateTime.now());

        // Then
        assertThat(task.getId()).isEqualTo(id);
        assertThat(task.getTitle()).isEqualTo(title);
        assertThat(task.getDescription()).isEqualTo(description);
        assertThat(task.getBasePoints()).isEqualTo(basePoints);
        assertThat(task.getRecurrenceType()).isEqualTo(recurrenceType);
        assertThat(task.getAssignedUsers()).isEqualTo(assignedUsers);
        assertThat(task.getCreatedBy()).isEqualTo(creator);
        assertThat(task.getStartDate()).isEqualTo(startDate);
        assertThat(task.getEndDate()).isEqualTo(endDate);
        assertThat(task.getWeeklyDays()).isEqualTo(weeklyDays);
        assertThat(task.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreate_ShouldSetCreatedAt() {
        // Given
        TaskDefinition newTask = new TaskDefinition();
        newTask.setTitle("Test Task");

        // When
        newTask.onCreate();

        // Then
        assertThat(newTask.getCreatedAt()).isNotNull();
        assertThat(newTask.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        // Given
        Long id = 100L;
        String title = "Updated Task";
        String description = "Updated Description";
        int basePoints = 20;
        RecurrenceType recurrenceType = RecurrenceType.WEEKLY;
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        String weeklyDays = "MONDAY,WEDNESDAY,FRIDAY";

        // When
        taskDefinition.setId(id);
        taskDefinition.setTitle(title);
        taskDefinition.setDescription(description);
        taskDefinition.setBasePoints(basePoints);
        taskDefinition.setRecurrenceType(recurrenceType);
        taskDefinition.setStartDate(startDate);
        taskDefinition.setEndDate(endDate);
        taskDefinition.setWeeklyDays(weeklyDays);

        // Then
        assertThat(taskDefinition.getId()).isEqualTo(id);
        assertThat(taskDefinition.getTitle()).isEqualTo(title);
        assertThat(taskDefinition.getDescription()).isEqualTo(description);
        assertThat(taskDefinition.getBasePoints()).isEqualTo(basePoints);
        assertThat(taskDefinition.getRecurrenceType()).isEqualTo(recurrenceType);
        assertThat(taskDefinition.getStartDate()).isEqualTo(startDate);
        assertThat(taskDefinition.getEndDate()).isEqualTo(endDate);
        assertThat(taskDefinition.getWeeklyDays()).isEqualTo(weeklyDays);
    }

    @Test
    void assignedUsers_ShouldHandleMultipleUsers() {
        // Given
        User child2 = new User();
        child2.setId(3L);
        child2.setUsername("child2");

        Set<User> users = new HashSet<>();
        users.add(assignedUser);
        users.add(child2);

        // When
        taskDefinition.setAssignedUsers(users);

        // Then
        assertThat(taskDefinition.getAssignedUsers()).hasSize(2);
        assertThat(taskDefinition.getAssignedUsers()).contains(assignedUser, child2);
    }

    @Test
    void equalsAndHashCode_ShouldWorkBasedOnId() {
        // Given
        TaskDefinition task1 = new TaskDefinition();
        task1.setId(1L);
        task1.setTitle("Task 1");

        TaskDefinition task2 = new TaskDefinition();
        task2.setId(1L);
        task2.setTitle("Task 1");

        TaskDefinition task3 = new TaskDefinition();
        task3.setId(2L);
        task3.setTitle("Task 1");

        // Then
        assertThat(task1).isEqualTo(task2);
        assertThat(task1).isNotEqualTo(task3);
        assertThat(task1.hashCode()).isEqualTo(task2.hashCode());
        assertThat(task1.hashCode()).isNotEqualTo(task3.hashCode());
    }

    @Test
    void toString_ShouldContainKeyFields() {
        // When
        String toString = taskDefinition.toString();

        // Then
        assertThat(toString).contains("TaskDefinition");
        assertThat(toString).contains("id=1");
        assertThat(toString).contains("title=Test Task");
        assertThat(toString).contains("description=Test Description");
        assertThat(toString).contains("basePoints=10");
    }
}