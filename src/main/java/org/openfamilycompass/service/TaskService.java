package org.openfamilycompass.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.openfamilycompass.model.NotificationType;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.RecurrenceType;
import org.openfamilycompass.model.Task;
import org.openfamilycompass.model.TaskStatus;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.repository.TaskRepository;
import org.openfamilycompass.repository.UserRepository;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final PointService pointService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional
    public Task createTask(@NonNull String title, String description, int basePoints,
            User assignedUser, @NonNull RecurrenceType recurrenceType,
            LocalDate dueDate, boolean isTemplate) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setBasePoints(basePoints);
        task.setAssignedUser(assignedUser);
        task.setRecurrenceType(recurrenceType);
        task.setDueDate(dueDate);
        task.setStatus(TaskStatus.PENDING);
        task.setTemplate(isTemplate);

        return taskRepository.save(task);
    }

    @Transactional
    public Task markAsCompleted(@NonNull Long taskId, @NonNull User child) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("Task cannot be completed in current state");
        }

        task.setStatus(TaskStatus.CHILD_COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        Task savedTask = taskRepository.save(task);

        // Notify all parents about completed task
        List<User> parents = userRepository.findByRole(UserRole.PARENT);
        for (User parent : parents) {
            notificationService.createNotification(
                    parent,
                    NotificationType.TASK_COMPLETED,
                    "Aufgabe abgeschlossen",
                    String.format("'%s' hat die Aufgabe '%s' abgeschlossen und wartet auf Genehmigung.",
                            child.getFirstName(), task.getTitle()),
                    task.getId());
        }

        return savedTask;
    }

    @Transactional
    public Task approveTask(@NonNull Long taskId, @NonNull User approver, int awardedPoints, String notes) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (task.getStatus() != TaskStatus.CHILD_COMPLETED) {
            throw new IllegalStateException("Task must be completed by child first");
        }

        task.setStatus(TaskStatus.APPROVED);
        task.setApprovedAt(LocalDateTime.now());
        task.setApprovedBy(approver);
        task.setAwardedPoints(awardedPoints);
        task.setParentNotes(notes);

        Task savedTask = taskRepository.save(task);

        // Credit points
        Long taskId2 = Objects.requireNonNull(task.getId(), "Task ID must not be null");
        pointService.addPoints(task.getAssignedUser(), awardedPoints,
                PointTransactionType.TASK, "Task completed: " + task.getTitle(),
                taskId2, approver);

        // Notify child about approved task and points
        notificationService.createNotification(
                task.getAssignedUser(),
                NotificationType.TASK_APPROVED,
                "Aufgabe genehmigt",
                String.format("Deine Aufgabe '%s' wurde genehmigt! Du hast %d Punkte erhalten.",
                        task.getTitle(), awardedPoints),
                task.getId());

        // If recurring task, create next instance
        if (task.getRecurrenceType() != RecurrenceType.ONCE) {
            createNextRecurrence(task);
        }

        return savedTask;
    }

    @Transactional
    public Task rejectTask(@NonNull Long taskId, @NonNull User rejector, String notes) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        task.setStatus(TaskStatus.REJECTED);
        task.setApprovedBy(rejector);
        task.setParentNotes(notes);

        Task savedTask = taskRepository.save(task);

        // Notify child about rejected task
        notificationService.createNotification(
                task.getAssignedUser(),
                NotificationType.TASK_REJECTED,
                "Aufgabe abgelehnt",
                String.format("Deine Aufgabe '%s' wurde abgelehnt. %s",
                        task.getTitle(), notes != null ? "Notiz: " + notes : ""),
                task.getId());

        return savedTask;
    }

    @Transactional
    public void createNextRecurrence(Task originalTask) {
        if (originalTask.getRecurrenceType() == RecurrenceType.ONCE) {
            return;
        }

        LocalDate nextDueDate = calculateNextDueDate(originalTask.getDueDate(),
                originalTask.getRecurrenceType());

        Task template = originalTask.isTemplate() ? originalTask : originalTask.getTemplateTask();
        if (template == null) {
            template = originalTask;
        }

        Task nextTask = new Task();
        nextTask.setTitle(template.getTitle());
        nextTask.setDescription(template.getDescription());
        nextTask.setBasePoints(template.getBasePoints());
        nextTask.setAssignedUser(template.getAssignedUser());
        nextTask.setRecurrenceType(template.getRecurrenceType());
        nextTask.setDueDate(nextDueDate);
        nextTask.setStatus(TaskStatus.PENDING);
        nextTask.setTemplate(false);
        nextTask.setTemplateTask(template);

        taskRepository.save(nextTask);
    }

    private LocalDate calculateNextDueDate(LocalDate currentDueDate, RecurrenceType recurrenceType) {
        return switch (recurrenceType) {
            case DAILY -> currentDueDate.plusDays(1);
            case WEEKLY -> currentDueDate.plusWeeks(1);
            case MONTHLY -> currentDueDate.plusMonths(1);
            default -> currentDueDate;
        };
    }

    // Check every day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void generateRecurringTasks() {
        log.info("Checking for recurring tasks to generate...");

        List<Task> templates = taskRepository.findByIsTemplateTrue();
        LocalDate today = LocalDate.now();

        for (Task template : templates) {
            if (template.getRecurrenceType() != RecurrenceType.ONCE) {
                // Check if a task for today already exists
                List<Task> todayTasks = taskRepository.findByDueDate(today);
                boolean exists = todayTasks.stream()
                        .anyMatch(t -> t.getTemplateTask() != null &&
                                t.getTemplateTask().getId().equals(template.getId()));

                if (!exists) {
                    createTaskFromTemplate(template, today);
                }
            }
        }
    }

    @Transactional
    protected void createTaskFromTemplate(Task template, LocalDate dueDate) {
        Task task = new Task();
        task.setTitle(template.getTitle());
        task.setDescription(template.getDescription());
        task.setBasePoints(template.getBasePoints());
        task.setAssignedUser(template.getAssignedUser());
        task.setRecurrenceType(template.getRecurrenceType());
        task.setDueDate(dueDate);
        task.setStatus(TaskStatus.PENDING);
        task.setTemplate(false);
        task.setTemplateTask(template);

        taskRepository.save(task);
        log.info("Created task from template: {}", template.getTitle());
    }

    public List<Task> findByUser(@NonNull User user) {
        return taskRepository.findByAssignedUser(user);
    }

    public List<Task> findByUserAndStatus(@NonNull User user, @NonNull TaskStatus status) {
        return taskRepository.findByAssignedUserAndStatus(user, status);
    }

    public List<Task> findPendingForUser(@NonNull User user) {
        return taskRepository.findByAssignedUserAndStatusIn(user,
                List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS));
    }

    public List<Task> findPendingApproval() {
        return taskRepository.findByStatus(TaskStatus.CHILD_COMPLETED);
    }

    public List<Task> findTemplates() {
        return taskRepository.findByIsTemplateTrue();
    }
}
