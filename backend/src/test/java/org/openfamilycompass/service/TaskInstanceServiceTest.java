package org.openfamilycompass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openfamilycompass.model.NotificationType;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.TaskDefinition;
import org.openfamilycompass.model.TaskInstance;
import org.openfamilycompass.model.TaskStatus;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.TaskInstanceRepository;

@ExtendWith(MockitoExtension.class)
class TaskInstanceServiceTest {

    @Mock
    private TaskInstanceRepository taskInstanceRepository;

    @Mock
    private PointService pointService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TaskInstanceService taskInstanceService;

    private TaskDefinition taskDefinition;
    private User child;
    private User parent;
    private TaskInstance taskInstance;

    @BeforeEach
    void setUp() {
        taskDefinition = new TaskDefinition();
        taskDefinition.setId(1L);
        taskDefinition.setTitle("Test Task");
        taskDefinition.setDescription("Test Description");
        taskDefinition.setBasePoints(10);

        child = new User();
        child.setId(2L);
        child.setUsername("child");
        child.setRole(UserRole.CHILD);
        child.setFirstName("TestChild");

        parent = new User();
        parent.setId(3L);
        parent.setUsername("parent");
        parent.setRole(UserRole.PARENT);

        taskDefinition.setCreatedBy(parent);

        taskInstance = new TaskInstance();
        taskInstance.setId(1L);
        taskInstance.setTaskDefinition(taskDefinition);
        taskInstance.setAssignedUser(child);
        taskInstance.setDeadline(LocalDate.now().plusDays(1).atTime(23, 59, 59));
        taskInstance.setStatus(TaskStatus.PENDING);
    }

    @Test
    void createTaskInstance_ShouldCreateTaskInstanceWithCorrectDefaults() {
        // Given
        LocalDateTime deadline = LocalDate.now().plusDays(3).atTime(23, 59, 59);
        TaskInstance savedInstance = new TaskInstance();
        savedInstance.setId(2L);
        savedInstance.setTaskDefinition(taskDefinition);
        savedInstance.setAssignedUser(child);
        savedInstance.setDeadline(deadline);
        savedInstance.setStatus(TaskStatus.PENDING);

        when(taskInstanceRepository.save(any(TaskInstance.class))).thenReturn(savedInstance);

        // When
        TaskInstance result = taskInstanceService.createTaskInstance(taskDefinition, child, deadline);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTaskDefinition()).isEqualTo(taskDefinition);
        assertThat(result.getAssignedUser()).isEqualTo(child);
        assertThat(result.getDeadline()).isEqualTo(deadline);
        assertThat(result.getStatus()).isEqualTo(TaskStatus.PENDING);

        ArgumentCaptor<TaskInstance> captor = ArgumentCaptor.forClass(TaskInstance.class);
        verify(taskInstanceRepository).save(captor.capture());
        TaskInstance captured = captor.getValue();
        assertThat(captured.getTaskDefinition()).isEqualTo(taskDefinition);
        assertThat(captured.getAssignedUser()).isEqualTo(child);
        assertThat(captured.getDeadline()).isEqualTo(deadline);
        assertThat(captured.getStatus()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void findOverduePending_ShouldReturnOverduePendingTasks() {
        // Given
        LocalDate today = LocalDate.now();
        List<TaskInstance> expectedTasks = List.of(taskInstance);
        when(taskInstanceRepository.findByStatusAndDeadlineBefore(TaskStatus.PENDING, today.atStartOfDay()))
                .thenReturn(expectedTasks);

        // When
        List<TaskInstance> result = taskInstanceService.findOverduePending(today);

        // Then
        assertThat(result).isEqualTo(expectedTasks);
        verify(taskInstanceRepository).findByStatusAndDeadlineBefore(TaskStatus.PENDING, today.atStartOfDay());
    }

    @Test
    void markAsCompleted_ShouldMarkTaskAsCompletedAndNotifyParents() {
        // Given
        when(taskInstanceRepository.findById(1L)).thenReturn(Optional.of(taskInstance));
        when(taskInstanceRepository.save(any(TaskInstance.class))).thenReturn(taskInstance);

        // When
        TaskInstance result = taskInstanceService.markAsCompleted(1L, child);

        // Then
        assertThat(result).isNotNull();
        assertThat(taskInstance.getStatus()).isEqualTo(TaskStatus.CHILD_COMPLETED);
        assertThat(taskInstance.getCompletedAt()).isNotNull();

        verify(notificationService).createLocalizedNotification(
                eq(parent),
                eq(NotificationType.TASK_COMPLETED),
                eq("notification.task.completed.title"),
                eq("notification.task.completed.message"),
                argThat(args -> args != null && args.length == 2
                        && "TestChild".equals(args[0]) && "Test Task".equals(args[1])),
                eq(1L));
    }

    @Test
    void markAsCompleted_ShouldThrowException_WhenTaskNotFound() {
        // Given
        when(taskInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskInstanceService.markAsCompleted(999L, child))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TaskInstance not found");
    }

    @Test
    void markAsCompleted_ShouldThrowException_WhenTaskNotAssignedToUser() {
        // Given
        User otherChild = new User();
        otherChild.setId(4L);
        otherChild.setUsername("otherChild");

        when(taskInstanceRepository.findById(1L)).thenReturn(Optional.of(taskInstance));

        // When & Then
        assertThatThrownBy(() -> taskInstanceService.markAsCompleted(1L, otherChild))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Task does not belong to this user");
    }

    @Test
    void markAsCompleted_ShouldThrowException_WhenTaskNotInPendingState() {
        // Given
        taskInstance.setStatus(TaskStatus.CHILD_COMPLETED);
        when(taskInstanceRepository.findById(1L)).thenReturn(Optional.of(taskInstance));

        // When & Then
        assertThatThrownBy(() -> taskInstanceService.markAsCompleted(1L, child))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Task is not in pending state");
    }

    @Test
    void approveTask_ShouldApproveTaskAndAwardPoints() {
        // Given
        taskInstance.setStatus(TaskStatus.CHILD_COMPLETED);
        int awardedPoints = 15;
        String notes = "Well done!";

        when(taskInstanceRepository.findById(1L)).thenReturn(Optional.of(taskInstance));
        when(taskInstanceRepository.save(any(TaskInstance.class))).thenReturn(taskInstance);

        // When
        TaskInstance result = taskInstanceService.approveTask(1L, parent, awardedPoints, notes);

        // Then
        assertThat(result).isNotNull();
        assertThat(taskInstance.getStatus()).isEqualTo(TaskStatus.APPROVED);
        assertThat(taskInstance.getApprovedAt()).isNotNull();
        assertThat(taskInstance.getApprovedBy()).isEqualTo(parent);
        assertThat(taskInstance.getAwardedPoints()).isEqualTo(awardedPoints);
        assertThat(taskInstance.getParentNotes()).isEqualTo(notes);

        verify(pointService).completeTransaction(
                eq(1L), eq(PointTransactionType.TASK),
                eq(child), eq(awardedPoints),
                eq("Test Task"), eq(parent));

        verify(notificationService).createLocalizedNotification(
                eq(child),
                eq(NotificationType.TASK_APPROVED),
                eq("notification.task.approved.title"),
                eq("notification.task.approved.message"),
                argThat(args -> args != null && args.length == 2
                        && "Test Task".equals(args[0]) && Integer.valueOf(15).equals(args[1])),
                eq(1L));
    }

    @Test
    void approveTask_ShouldThrowException_WhenTaskNotFound() {
        // Given
        when(taskInstanceRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskInstanceService.approveTask(999L, parent, 10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TaskInstance not found");
    }

    @Test
    void approveTask_ShouldThrowException_WhenTaskNotCompletedByChild() {
        // Given
        taskInstance.setStatus(TaskStatus.PENDING);
        when(taskInstanceRepository.findById(1L)).thenReturn(Optional.of(taskInstance));

        // When & Then
        assertThatThrownBy(() -> taskInstanceService.approveTask(1L, parent, 10, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Task must be completed by child first");
    }

    @Test
    void rejectTask_ShouldRejectTaskWithoutAwardingPoints() {
        // Given
        taskInstance.setStatus(TaskStatus.CHILD_COMPLETED);
        String notes = "Not good enough";

        when(taskInstanceRepository.findById(1L)).thenReturn(Optional.of(taskInstance));
        when(taskInstanceRepository.save(any(TaskInstance.class))).thenReturn(taskInstance);

        // When
        TaskInstance result = taskInstanceService.rejectTask(1L, parent, notes);

        // Then
        assertThat(result).isNotNull();
        assertThat(taskInstance.getStatus()).isEqualTo(TaskStatus.REJECTED);
        assertThat(taskInstance.getApprovedBy()).isEqualTo(parent);
        assertThat(taskInstance.getParentNotes()).isEqualTo(notes);

        verify(pointService, never()).addPoints(any(User.class), any(Integer.class), any(), any(), any(), any());

        verify(notificationService).createLocalizedNotification(
                eq(child),
                eq(NotificationType.TASK_REJECTED),
                eq("notification.task.rejected.title"),
                eq("notification.task.rejected.message"),
                argThat(args -> args != null && args.length == 1 && "Test Task".equals(args[0])),
                eq(notes),
                eq(1L));
    }

    @Test
    void findPendingApproval_ShouldReturnChildCompletedTasks() {
        // Given
        List<TaskInstance> expectedTasks = List.of(taskInstance);
        when(taskInstanceRepository.findByStatus(TaskStatus.CHILD_COMPLETED)).thenReturn(expectedTasks);

        // When
        List<TaskInstance> result = taskInstanceService.findPendingApproval();

        // Then
        assertThat(result).isEqualTo(expectedTasks);
        verify(taskInstanceRepository).findByStatus(TaskStatus.CHILD_COMPLETED);
    }

    @Test
    void findPendingForUser_ShouldReturnPendingAndInProgressTasks() {
        // Given
        List<TaskInstance> expectedTasks = List.of(taskInstance);
        when(taskInstanceRepository.findByAssignedUserAndStatusIn(
                eq(child), eq(List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS))))
                .thenReturn(expectedTasks);

        // When
        List<TaskInstance> result = taskInstanceService.findPendingForUser(child);

        // Then
        assertThat(result).isEqualTo(expectedTasks);
    }

    @Test
    void findByUserAndStatus_ShouldReturnTasksWithSpecificStatus() {
        // Given
        List<TaskInstance> expectedTasks = List.of(taskInstance);
        when(taskInstanceRepository.findByAssignedUserAndStatus(child, TaskStatus.PENDING))
                .thenReturn(expectedTasks);

        // When
        List<TaskInstance> result = taskInstanceService.findByUserAndStatus(child, TaskStatus.PENDING);

        // Then
        assertThat(result).isEqualTo(expectedTasks);
    }

    @Test
    void findByUser_ShouldReturnAllUserTasks() {
        // Given
        List<TaskInstance> expectedTasks = List.of(taskInstance);
        when(taskInstanceRepository.findByAssignedUser(child)).thenReturn(expectedTasks);

        // When
        List<TaskInstance> result = taskInstanceService.findByUser(child);

        // Then
        assertThat(result).isEqualTo(expectedTasks);
    }

    @Test
    void findAll_ShouldReturnAllTaskInstances() {
        // Given
        List<TaskInstance> expectedTasks = List.of(taskInstance);
        when(taskInstanceRepository.findAll()).thenReturn(expectedTasks);

        // When
        List<TaskInstance> result = taskInstanceService.findAll();

        // Then
        assertThat(result).isEqualTo(expectedTasks);
    }

    @Test
    void findById_ShouldReturnOptionalTaskInstance() {
        // Given
        when(taskInstanceRepository.findById(1L)).thenReturn(Optional.of(taskInstance));

        // When
        Optional<TaskInstance> result = taskInstanceService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(taskInstance);
    }

    @Test
    void deleteTaskInstance_ShouldDeleteTaskInstance() {
        // Given
        when(taskInstanceRepository.existsById(1L)).thenReturn(true);

        // When
        taskInstanceService.deleteTaskInstance(1L);

        // Then
        verify(taskInstanceRepository).deleteById(1L);
    }

    @Test
    void deleteTaskInstance_ShouldThrowException_WhenNotExists() {
        // Given
        when(taskInstanceRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> taskInstanceService.deleteTaskInstance(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TaskInstance not found");
    }

    @Test
    void deleteByTaskDefinitionId_ShouldDeleteAllInstancesForDefinition() {
        // Given
        Long taskDefinitionId = 1L;

        // When
        taskInstanceService.deleteByTaskDefinitionId(taskDefinitionId);

        // Then
        verify(taskInstanceRepository).deleteByTaskDefinition_Id(taskDefinitionId);
    }
}