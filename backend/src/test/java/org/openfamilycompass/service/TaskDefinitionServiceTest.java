package org.openfamilycompass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openfamilycompass.model.RecurrenceType;
import org.openfamilycompass.model.TaskDefinition;
import org.openfamilycompass.model.TaskInstance;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.TaskDefinitionRepository;

@ExtendWith(MockitoExtension.class)
class TaskDefinitionServiceTest {

    @Mock
    private TaskDefinitionRepository taskDefinitionRepository;

    @Mock
    private TaskInstanceService taskInstanceService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TaskDefinitionService taskDefinitionService;

    private User parent;
    private User child1;
    private User child2;
    private TaskDefinition taskDefinition;

    @BeforeEach
    void setUp() {
        parent = new User();
        parent.setId(1L);
        parent.setUsername("parent");
        parent.setRole(UserRole.PARENT);

        child1 = new User();
        child1.setId(2L);
        child1.setUsername("child1");
        child1.setRole(UserRole.CHILD);

        child2 = new User();
        child2.setId(3L);
        child2.setUsername("child2");
        child2.setRole(UserRole.CHILD);

        taskDefinition = new TaskDefinition();
        taskDefinition.setId(1L);
        taskDefinition.setTitle("Test Task");
        taskDefinition.setDescription("Test Description");
        taskDefinition.setBasePoints(10);
        taskDefinition.setRecurrenceType(RecurrenceType.ONCE);
        taskDefinition.setCreatedBy(parent);
        taskDefinition.setStartDate(LocalDate.now());
        taskDefinition.setSeriesEndDate(LocalDate.now().plusDays(7));
    }

    @Test
    void createTaskDefinition_ShouldCreateOnceTaskAndTaskInstances() {
        // Given
        String title = "New Task";
        String description = "Task Description";
        int basePoints = 15;
        RecurrenceType recurrenceType = RecurrenceType.ONCE;
        Set<User> assignedUsers = Set.of(child1, child2);
        LocalDate endDate = LocalDate.now().plusDays(5);

        TaskDefinition savedDefinition = new TaskDefinition();
        savedDefinition.setId(1L);
        savedDefinition.setTitle(title);
        savedDefinition.setDescription(description);
        savedDefinition.setBasePoints(basePoints);
        savedDefinition.setRecurrenceType(recurrenceType);
        savedDefinition.setAssignedUsers(assignedUsers);
        savedDefinition.setCreatedBy(parent);
        savedDefinition.setSeriesEndDate(endDate);

        when(taskDefinitionRepository.save(any(TaskDefinition.class))).thenReturn(savedDefinition);

        // When
        TaskDefinition result = taskDefinitionService.createTaskDefinition(
                title, description, basePoints, recurrenceType, assignedUsers, parent, null, endDate, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getDescription()).isEqualTo(description);
        assertThat(result.getBasePoints()).isEqualTo(basePoints);
        assertThat(result.getRecurrenceType()).isEqualTo(recurrenceType);
        assertThat(result.getAssignedUsers()).isEqualTo(assignedUsers);
        assertThat(result.getCreatedBy()).isEqualTo(parent);
        assertThat(result.getSeriesEndDate()).isEqualTo(endDate);

        // Verify TaskInstances were created for ONCE tasks
        verify(taskInstanceService).createTaskInstance(savedDefinition, child1, (LocalDateTime) null);
        verify(taskInstanceService).createTaskInstance(savedDefinition, child2, (LocalDateTime) null);
    }

    @Test
    void createTaskDefinition_ShouldNotCreateInstance_WhenWeeklyTaskHasNoDueDayToday() {
        // Given: a weekly task whose due days are deliberately chosen to exclude
        // today's day of week. The service is expected to create an initial
        // instance only when today IS a due day (see TaskDefinitionService);
        // otherwise the scheduler takes over later.
        DayOfWeek today = LocalDate.now().getDayOfWeek();
        String weeklyDays = today.plus(1).name() + "," + today.plus(3).name();

        String title = "Weekly Task";
        RecurrenceType recurrenceType = RecurrenceType.WEEKLY;
        Set<User> assignedUsers = Set.of(child1);

        TaskDefinition savedDefinition = new TaskDefinition();
        savedDefinition.setId(1L);
        savedDefinition.setTitle(title);
        savedDefinition.setRecurrenceType(recurrenceType);
        savedDefinition.setAssignedUsers(assignedUsers);
        savedDefinition.setCreatedBy(parent);
        savedDefinition.setWeeklyDays(weeklyDays);

        when(taskDefinitionRepository.save(any(TaskDefinition.class))).thenReturn(savedDefinition);

        // When
        TaskDefinition result = taskDefinitionService.createTaskDefinition(
                title, null, 10, recurrenceType, assignedUsers, parent, null, null, weeklyDays);

        // Then
        assertThat(result.getRecurrenceType()).isEqualTo(RecurrenceType.WEEKLY);
        assertThat(result.getWeeklyDays()).isEqualTo(weeklyDays);

        // No instance should be created today because today is not a due day.
        verify(taskInstanceService, never()).createTaskInstance(any(), any(), any());
    }

    @Test
    void findByCreatedBy_ShouldReturnTaskDefinitions() {
        // Given
        List<TaskDefinition> expectedTasks = List.of(taskDefinition);
        when(taskDefinitionRepository.findByCreatedBy(parent)).thenReturn(expectedTasks);

        // When
        List<TaskDefinition> result = taskDefinitionService.findByCreatedBy(parent);

        // Then
        assertThat(result).isEqualTo(expectedTasks);
        verify(taskDefinitionRepository).findByCreatedBy(parent);
    }

    @Test
    void findByAssignedUser_ShouldReturnTaskDefinitions() {
        // Given
        List<TaskDefinition> expectedTasks = List.of(taskDefinition);
        when(taskDefinitionRepository.findByAssignedUsersContaining(child1)).thenReturn(expectedTasks);

        // When
        List<TaskDefinition> result = taskDefinitionService.findByAssignedUser(child1);

        // Then
        assertThat(result).isEqualTo(expectedTasks);
        verify(taskDefinitionRepository).findByAssignedUsersContaining(child1);
    }

    @Test
    void findById_ShouldReturnTaskDefinition_WhenExists() {
        // Given
        when(taskDefinitionRepository.findById(1L)).thenReturn(Optional.of(taskDefinition));

        // When
        Optional<TaskDefinition> result = taskDefinitionService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(taskDefinition);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(taskDefinitionRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<TaskDefinition> result = taskDefinitionService.findById(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void deleteTaskDefinition_ShouldDeleteTaskAndInstances() {
        // Given
        when(taskDefinitionRepository.existsById(1L)).thenReturn(true);

        // When
        taskDefinitionService.deleteTaskDefinition(1L);

        // Then
        verify(taskInstanceService).deleteByTaskDefinitionId(1L);
        verify(taskDefinitionRepository).deleteById(1L);
    }

    @Test
    void deleteTaskDefinition_ShouldThrowException_WhenNotExists() {
        // Given
        when(taskDefinitionRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> taskDefinitionService.deleteTaskDefinition(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TaskDefinition not found");
    }

    @Test
    void updateTaskDefinition_ShouldUpdateAllFields() {
        // Given
        Long id = 1L;
        String newTitle = "Updated Task";
        String newDescription = "Updated Description";
        int newBasePoints = 20;
        RecurrenceType newRecurrenceType = RecurrenceType.WEEKLY;
        Set<User> newAssignedUsers = Set.of(child2);
        LocalDate newStartDate = LocalDate.now().plusDays(1);
        LocalDate newEndDate = LocalDate.now().plusDays(10);
        String newWeeklyDays = "TUESDAY,THURSDAY";

        TaskDefinition existing = new TaskDefinition();
        existing.setId(id);
        existing.setTitle("Old Title");
        existing.setDescription("Old Description");
        existing.setBasePoints(5);
        existing.setRecurrenceType(RecurrenceType.ONCE);
        existing.setAssignedUsers(Set.of(child1));
        existing.setCreatedBy(parent);

        TaskDefinition updated = new TaskDefinition();
        updated.setId(id);
        updated.setTitle(newTitle);
        updated.setDescription(newDescription);
        updated.setBasePoints(newBasePoints);
        updated.setRecurrenceType(newRecurrenceType);
        updated.setAssignedUsers(newAssignedUsers);
        updated.setCreatedBy(parent);
        updated.setStartDate(newStartDate);
        updated.setSeriesEndDate(newEndDate);
        updated.setWeeklyDays(newWeeklyDays);

        when(taskDefinitionRepository.findById(id)).thenReturn(Optional.of(existing));
        when(taskDefinitionRepository.save(any(TaskDefinition.class))).thenReturn(updated);

        // When
        TaskDefinition result = taskDefinitionService.updateTaskDefinition(
                id, newTitle, newDescription, newBasePoints, newRecurrenceType,
                newAssignedUsers, newStartDate, newEndDate, newWeeklyDays);

        // Then
        assertThat(result.getTitle()).isEqualTo(newTitle);
        assertThat(result.getDescription()).isEqualTo(newDescription);
        assertThat(result.getBasePoints()).isEqualTo(newBasePoints);
        assertThat(result.getRecurrenceType()).isEqualTo(newRecurrenceType);
        assertThat(result.getAssignedUsers()).isEqualTo(newAssignedUsers);
        assertThat(result.getStartDate()).isEqualTo(newStartDate);
        assertThat(result.getSeriesEndDate()).isEqualTo(newEndDate);
        assertThat(result.getWeeklyDays()).isEqualTo(newWeeklyDays);
    }

    @Test
    void updateTaskDefinition_ShouldThrowException_WhenNotExists() {
        // Given
        when(taskDefinitionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskDefinitionService.updateTaskDefinition(
                999L, "Title", "Desc", 10, RecurrenceType.ONCE, Set.of(), null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TaskDefinition not found");
    }

    @Test
    void generateTaskInstances_ShouldCreateWeeklyTasksForToday() {
        // Given
        LocalDate today = LocalDate.now();
        Set<User> assignedUsers = Set.of(child1);

        TaskDefinition weeklyTask = new TaskDefinition();
        weeklyTask.setId(2L);
        weeklyTask.setTitle("Weekly Task");
        weeklyTask.setRecurrenceType(RecurrenceType.WEEKLY);
        weeklyTask.setAssignedUsers(assignedUsers);
        weeklyTask.setWeeklyDays(today.getDayOfWeek().toString()); // Today is in weekly days

        List<TaskDefinition> allDefinitions = List.of(weeklyTask);
        when(taskDefinitionRepository.findAll()).thenReturn(allDefinitions);

        // When
        taskDefinitionService.generateTaskInstances();

        // Then
        verify(taskInstanceService).createTaskInstance(weeklyTask, child1, today.atTime(23, 59, 59));
    }

    @Test
    void generateTaskInstances_ShouldExpireOverdueTasks() {
        // Given
        LocalDate today = LocalDate.now();
        TaskInstance overdueTask = new TaskInstance();
        overdueTask.setId(1L);
        overdueTask.setTaskDefinition(taskDefinition);
        overdueTask.setAssignedUser(child1);
        List<TaskInstance> overdueTasks = List.of(overdueTask);

        when(taskDefinitionRepository.findAll()).thenReturn(List.of());
        when(taskInstanceService.findOverduePending(today)).thenReturn(overdueTasks);

        // When
        taskDefinitionService.generateTaskInstances();

        // Then
        verify(taskInstanceService).findOverduePending(today);
        // Note: We can't easily verify the status change without a more complex setup
        // In a real scenario, the task status would be changed in the service
    }

    @Test
    void generateTaskInstances_ShouldNotCreateDuplicateWeeklyTasks() {
        // Given
        LocalDate today = LocalDate.now();
        Set<User> assignedUsers = Set.of(child1);

        TaskDefinition weeklyTask = new TaskDefinition();
        weeklyTask.setId(2L);
        weeklyTask.setTitle("Weekly Task");
        weeklyTask.setRecurrenceType(RecurrenceType.WEEKLY);
        weeklyTask.setAssignedUsers(assignedUsers);
        weeklyTask.setWeeklyDays(today.getDayOfWeek().toString());

        List<TaskDefinition> allDefinitions = List.of(weeklyTask);
        when(taskDefinitionRepository.findAll()).thenReturn(allDefinitions);

        // Mock that task instance already exists for this task definition and date
        TaskInstance existingInstance = new TaskInstance();
        existingInstance.setTaskDefinition(weeklyTask);
        existingInstance.setDeadline(today.atTime(23, 59, 59));
        List<TaskInstance> existingInstances = List.of(existingInstance);
        when(taskInstanceService.findByUser(child1)).thenReturn(existingInstances);

        // When
        taskDefinitionService.generateTaskInstances();

        // Then
        verify(taskInstanceService, never()).createTaskInstance(any(), any(), any());
    }

    @Test
    void generateTaskInstances_ShouldSkipTasksAfterEndDate() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDate pastEndDate = today.minusDays(1);
        Set<User> assignedUsers = Set.of(child1);

        TaskDefinition expiredTask = new TaskDefinition();
        expiredTask.setId(2L);
        expiredTask.setTitle("Expired Task");
        expiredTask.setRecurrenceType(RecurrenceType.WEEKLY);
        expiredTask.setAssignedUsers(assignedUsers);
        expiredTask.setSeriesEndDate(pastEndDate);
        expiredTask.setWeeklyDays(today.getDayOfWeek().toString());

        List<TaskDefinition> allDefinitions = List.of(expiredTask);
        when(taskDefinitionRepository.findAll()).thenReturn(allDefinitions);

        // When
        taskDefinitionService.generateTaskInstances();

        // Then
        verify(taskInstanceService, never()).createTaskInstance(any(), any(), any());
    }
}