package org.openfamilycompass.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.openfamilycompass.model.NotificationType;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.TaskDefinition;
import org.openfamilycompass.model.TaskInstance;
import org.openfamilycompass.model.TaskStatus;
import org.openfamilycompass.model.User;
import org.openfamilycompass.repository.TaskInstanceRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskInstanceService {

    private final TaskInstanceRepository taskInstanceRepository;
    private final PointService pointService;
    private final NotificationService notificationService;

    @Transactional
    public TaskInstance createTaskInstance(@NonNull TaskDefinition definition, @NonNull User assignedUser,
            LocalDateTime deadline) {
        TaskInstance instance = new TaskInstance();
        instance.setTaskDefinition(definition);
        instance.setAssignedUser(assignedUser);
        instance.setDeadline(deadline);
        instance.setStatus(TaskStatus.PENDING);

        TaskInstance savedInstance = taskInstanceRepository.save(instance);

        notificationService.createLocalizedNotification(
                assignedUser,
                NotificationType.TASK_ASSIGNED,
                "notification.task.assigned.title",
                "notification.task.assigned.message",
                new Object[] { definition.getTitle() },
                savedInstance.getId());

        return savedInstance;
    }

    @Transactional(readOnly = true)
    public List<TaskInstance> findOverduePending(@NonNull LocalDate today) {
        return findOverduePending(today.atStartOfDay());
    }

    @Transactional(readOnly = true)
    public List<TaskInstance> findOverduePending(@NonNull LocalDateTime now) {
        return taskInstanceRepository.findByStatusAndDeadlineBefore(TaskStatus.PENDING, now);
    }

    @Transactional
    public TaskInstance markAsCompleted(@NonNull Long taskInstanceId, @NonNull User child) {
        TaskInstance instance = taskInstanceRepository.findById(taskInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("TaskInstance not found"));

        if (!instance.getAssignedUser().equals(child)) {
            throw new IllegalArgumentException("Task does not belong to this user");
        }

        if (instance.getStatus() != TaskStatus.PENDING) {
            throw new IllegalStateException("Task is not in pending state");
        }

        instance.setStatus(TaskStatus.CHILD_COMPLETED);
        instance.setCompletedAt(LocalDateTime.now());

        TaskInstance savedInstance = taskInstanceRepository.save(instance);

        User parentToNotify = instance.getTaskDefinition().getCreatedBy() != null
                ? instance.getTaskDefinition().getCreatedBy()
                : instance.getAssignedUser();

        notificationService.createLocalizedNotification(
                parentToNotify,
                NotificationType.TASK_COMPLETED,
                "notification.task.completed.title",
                "notification.task.completed.message",
                new Object[] { instance.getAssignedUser().getFirstName(), instance.getTaskDefinition().getTitle() },
                instance.getId());

        return savedInstance;
    }

    @Transactional
    public TaskInstance approveTask(@NonNull Long taskInstanceId, @NonNull User approver, int awardedPoints,
            String notes) {
        TaskInstance instance = taskInstanceRepository.findById(taskInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("TaskInstance not found"));

        if (instance.getStatus() != TaskStatus.CHILD_COMPLETED) {
            throw new IllegalStateException("Task must be completed by child first");
        }

        instance.setStatus(TaskStatus.APPROVED);
        instance.setApprovedAt(LocalDateTime.now());
        instance.setApprovedBy(approver);
        instance.setAwardedPoints(awardedPoints);
        instance.setParentNotes(notes);

        TaskInstance savedInstance = taskInstanceRepository.save(instance);

        // Credit points
        Long instanceId = Objects.requireNonNull(instance.getId(), "TaskInstance ID must not be null");
        pointService.addPoints(instance.getAssignedUser(), awardedPoints,
                PointTransactionType.TASK, "Task completed: " + instance.getTaskDefinition().getTitle(),
                instanceId, approver);

        // Notify child about approved task
        notificationService.createLocalizedNotification(
                instance.getAssignedUser(),
                NotificationType.TASK_APPROVED,
                "notification.task.approved.title",
                "notification.task.approved.message",
                new Object[] { instance.getTaskDefinition().getTitle(), awardedPoints },
                instance.getId());

        return savedInstance;
    }

    @Transactional
    public TaskInstance rejectTask(@NonNull Long taskInstanceId, @NonNull User rejector, String notes) {
        TaskInstance instance = taskInstanceRepository.findById(taskInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("TaskInstance not found"));

        instance.setStatus(TaskStatus.REJECTED);
        instance.setApprovedBy(rejector);
        instance.setParentNotes(notes);

        TaskInstance savedInstance = taskInstanceRepository.save(instance);

        // Notify child about rejected task
        notificationService.createLocalizedNotification(
                instance.getAssignedUser(),
                NotificationType.TASK_REJECTED,
                "notification.task.rejected.title",
                "notification.task.rejected.message",
                new Object[] { instance.getTaskDefinition().getTitle() },
                notes,
                instance.getId());

        return savedInstance;
    }

    public List<TaskInstance> findPendingApproval() {
        return taskInstanceRepository.findByStatus(TaskStatus.CHILD_COMPLETED);
    }

    @Transactional(readOnly = true)
    public List<TaskInstance> findPendingForUser(@NonNull User user) {
        return taskInstanceRepository.findByAssignedUserAndStatusIn(user,
                List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS));
    }

    @Transactional(readOnly = true)
    public List<TaskInstance> findByUserAndStatus(@NonNull User user, @NonNull TaskStatus status) {
        return taskInstanceRepository.findByAssignedUserAndStatus(user, status);
    }

    @Transactional(readOnly = true)
    public List<TaskInstance> findByUser(@NonNull User user) {
        return taskInstanceRepository.findByAssignedUser(user);
    }

    @Transactional(readOnly = true)
    public List<TaskInstance> findByAssignedUser(@NonNull User user) {
        return taskInstanceRepository.findByAssignedUser(user);
    }

    @Transactional(readOnly = true)
    public List<TaskInstance> findByStatus(@NonNull TaskStatus status) {
        return taskInstanceRepository.findByStatus(status);
    }

    @Transactional
    public TaskInstance markAsCompleted(@NonNull TaskInstance instance) {
        instance.setStatus(TaskStatus.CHILD_COMPLETED);
        instance.setCompletedAt(LocalDateTime.now());
        return taskInstanceRepository.save(instance);
    }

    @Transactional
    public TaskInstance approve(@NonNull TaskInstance instance, @NonNull User approver, int awardedPoints,
            String notes) {
        return approveTask(instance.getId(), approver, awardedPoints, notes);
    }

    @Transactional
    public TaskInstance reject(@NonNull TaskInstance instance, @NonNull User rejector, String notes) {
        return rejectTask(instance.getId(), rejector, notes);
    }

    @Transactional(readOnly = true)
    public List<TaskInstance> findAll() {
        return taskInstanceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<TaskInstance> findById(@NonNull Long id) {
        return taskInstanceRepository.findById(id);
    }

    @Transactional
    public void deleteTaskInstance(@NonNull Long id) {
        if (!taskInstanceRepository.existsById(id)) {
            throw new IllegalArgumentException("TaskInstance not found");
        }
        taskInstanceRepository.deleteById(id);
    }

    @Transactional
    public void deleteByTaskDefinitionId(@NonNull Long taskDefinitionId) {
        taskInstanceRepository.deleteByTaskDefinition_Id(taskDefinitionId);
    }
}