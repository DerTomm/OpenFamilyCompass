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
            LocalDate dueDate) {
        TaskInstance instance = new TaskInstance();
        instance.setTaskDefinition(definition);
        instance.setAssignedUser(assignedUser);
        instance.setDueDate(dueDate);
        instance.setStatus(TaskStatus.PENDING);

        return taskInstanceRepository.save(instance);
    }

    @Transactional(readOnly = true)
    public List<TaskInstance> findOverduePending(@NonNull LocalDate today) {
        return taskInstanceRepository.findByStatusAndDueDateBefore(TaskStatus.PENDING, today);
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

        // Notify parents about completed task
        notificationService.createNotification(
                instance.getAssignedUser(),
                NotificationType.TASK_COMPLETED,
                "Aufgabe erledigt",
                String.format("Deine Aufgabe '%s' wurde als erledigt markiert.",
                        instance.getTaskDefinition().getTitle()),
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
        notificationService.createNotification(
                instance.getAssignedUser(),
                NotificationType.TASK_APPROVED,
                "Aufgabe genehmigt",
                String.format("Deine Aufgabe '%s' wurde genehmigt. Du erhältst %d Punkte.",
                        instance.getTaskDefinition().getTitle(), awardedPoints),
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
        notificationService.createNotification(
                instance.getAssignedUser(),
                NotificationType.TASK_REJECTED,
                "Aufgabe abgelehnt",
                String.format("Deine Aufgabe '%s' wurde abgelehnt. %s",
                        instance.getTaskDefinition().getTitle(), notes != null ? "Notiz: " + notes : ""),
                instance.getId());

        return savedInstance;
    }

    public List<TaskInstance> findPendingApproval() {
        return taskInstanceRepository.findByStatus(TaskStatus.CHILD_COMPLETED);
    }

    public List<TaskInstance> findPendingForUser(@NonNull User user) {
        return taskInstanceRepository.findByAssignedUserAndStatusIn(user,
                List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS));
    }

    public List<TaskInstance> findByUserAndStatus(@NonNull User user, @NonNull TaskStatus status) {
        return taskInstanceRepository.findByAssignedUserAndStatus(user, status);
    }

    public List<TaskInstance> findByUser(@NonNull User user) {
        return taskInstanceRepository.findByAssignedUser(user);
    }

    public List<TaskInstance> findByAssignedUser(@NonNull User user) {
        return taskInstanceRepository.findByAssignedUser(user);
    }

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
    public TaskInstance approve(@NonNull TaskInstance instance, @NonNull User approver, int awardedPoints, String notes) {
        return approveTask(instance.getId(), approver, awardedPoints, notes);
    }

    @Transactional
    public TaskInstance reject(@NonNull TaskInstance instance, @NonNull User rejector, String notes) {
        return rejectTask(instance.getId(), rejector, notes);
    }

    public List<TaskInstance> findAll() {
        return taskInstanceRepository.findAll();
    }

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