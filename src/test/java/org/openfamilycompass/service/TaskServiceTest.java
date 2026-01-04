package org.openfamilycompass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.RecurrenceType;
import org.openfamilycompass.model.Task;
import org.openfamilycompass.model.TaskStatus;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.TaskRepository;
import org.openfamilycompass.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PointService pointService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private Task testTask;
    private User testChildUser;
    private User testUser;

    @BeforeEach
    void setUp() {
        testChildUser = new User();
        testChildUser.setId(1L);
        testChildUser.setUsername("testchild");
        testChildUser.setRole(UserRole.CHILD);
        testChildUser.setFirstName("TestChild");

        testUser = new User();
        testUser.setId(2L);
        testUser.setUsername("parent");
        testUser.setRole(UserRole.PARENT);

        testTask = new Task();
        testTask.setId(1L);
        testTask.setTitle("Test Task");
        testTask.setDescription("Test Description");
        testTask.setBasePoints(10);
        testTask.setAssignedUser(testChildUser);
        testTask.setStatus(TaskStatus.PENDING);
        testTask.setRecurrenceType(RecurrenceType.ONCE);
    }

    @Test
    void createTask_ShouldCreateAndSaveTask() {
        // Given
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        Task result = taskService.createTask(
                "Test Task",
                "Test Description",
                10,
                testChildUser,
                RecurrenceType.ONCE,
                LocalDate.now());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Task");
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void markAsCompleted_ShouldUpdateTaskStatus() {
        // Given
        testTask.setStatus(TaskStatus.PENDING);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        Task result = taskService.markAsCompleted(1L, testUser);

        // Then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.CHILD_COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(taskRepository).save(testTask);
    }

    @Test
    void markAsCompleted_ShouldThrowException_WhenTaskNotFound() {
        // Given
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> taskService.markAsCompleted(999L, testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task not found");
    }

    @Test
    void approveTask_ShouldApproveAndAddPoints() {
        // Given
        testTask.setStatus(TaskStatus.CHILD_COMPLETED);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        Task result = taskService.approveTask(1L, testUser, 10, "Well done!");

        // Then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.APPROVED);
        assertThat(result.getApprovedBy()).isEqualTo(testUser);
        assertThat(result.getAwardedPoints()).isEqualTo(10);
        verify(pointService).addPoints(any(), eq(10), eq(PointTransactionType.TASK), anyString(), anyLong(),
                eq(testUser));
    }

    @Test
    void approveTask_ShouldThrowException_WhenNotCompleted() {
        // Given
        testTask.setStatus(TaskStatus.PENDING);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        // When/Then
        assertThatThrownBy(() -> taskService.approveTask(1L, testUser, 10, "Notes"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be completed by child first");
    }

    @Test
    void rejectTask_ShouldRejectTask() {
        // Given
        testTask.setStatus(TaskStatus.CHILD_COMPLETED);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        Task result = taskService.rejectTask(1L, testUser, "Needs improvement");

        // Then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.REJECTED);
        assertThat(result.getApprovedBy()).isEqualTo(testUser);
        assertThat(result.getParentNotes()).isEqualTo("Needs improvement");
    }

    @Test
    void createNextRecurrence_ShouldNotCreateTask_WhenTypeIsOnce() {
        // Given
        testTask.setRecurrenceType(RecurrenceType.ONCE);

        // When
        taskService.createNextRecurrence(testTask);

        // Then
        verify(taskRepository, never()).save(any());
    }

    @Test
    void calculateNextDueDate_ShouldAddOneDay_ForDaily() {
        // Given
        LocalDate currentDate = LocalDate.of(2025, 1, 1);
        testTask.setDueDate(currentDate);
        testTask.setRecurrenceType(RecurrenceType.DAILY);
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        // When
        taskService.createNextRecurrence(testTask);

        // Then
        verify(taskRepository).save(argThat(task -> task.getDueDate().equals(LocalDate.of(2025, 1, 2))));
    }
}
