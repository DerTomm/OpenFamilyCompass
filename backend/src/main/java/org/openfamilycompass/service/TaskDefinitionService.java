package org.openfamilycompass.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.openfamilycompass.model.NotificationType;
import org.openfamilycompass.model.RecurrenceType;
import org.openfamilycompass.model.TaskDefinition;
import org.openfamilycompass.model.TaskInstance;
import org.openfamilycompass.model.TaskStatus;
import org.openfamilycompass.model.User;
import org.openfamilycompass.repository.TaskDefinitionRepository;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskDefinitionService {

    private final TaskDefinitionRepository taskDefinitionRepository;
    private final TaskInstanceService taskInstanceService;
    private final NotificationService notificationService;

    @Transactional
    public TaskDefinition createTaskDefinition(@NonNull String title, String description, int basePoints,
            @NonNull RecurrenceType recurrenceType, @NonNull Set<User> assignedUsers,
            @NonNull User createdBy, LocalDate startDate, LocalDate seriesEndDate, String weeklyDays) {
        return createTaskDefinition(title, description, basePoints, recurrenceType, assignedUsers, createdBy,
                startDate, seriesEndDate, null, weeklyDays);
    }

    @Transactional
    public TaskDefinition createTaskDefinition(@NonNull String title, String description, int basePoints,
            @NonNull RecurrenceType recurrenceType, @NonNull Set<User> assignedUsers,
            @NonNull User createdBy, LocalDate startDate, LocalDate seriesEndDate, LocalDateTime deadline,
            String weeklyDays) {
        TaskDefinition definition = new TaskDefinition();
        definition.setTitle(title);
        definition.setDescription(description);
        definition.setBasePoints(basePoints);
        definition.setRecurrenceType(recurrenceType);
        definition.setAssignedUsers(assignedUsers);
        definition.setCreatedBy(createdBy);
        definition.setStartDate(startDate);
        definition.setSeriesEndDate(seriesEndDate);
        definition.setDeadline(deadline);
        definition.setWeeklyDays(weeklyDays);

        TaskDefinition savedDefinition = taskDefinitionRepository.save(definition);

        // For one-time tasks: create TaskInstances immediately (if no
        // future start date)
        log.info(
                "TaskDefinition created: title='{}', recurrenceType='{}', startDate='{}', deadline='{}', assignedUsers.size={}",
                title, recurrenceType, startDate, deadline, assignedUsers.size());
        if (recurrenceType == RecurrenceType.ONCE) {
            LocalDate today = LocalDate.now();
            if (startDate == null || !startDate.isAfter(today)) {
                log.info("Creating TaskInstances for ONCE task '{}' with {} assigned users, deadline='{}'",
                        title, assignedUsers.size(), deadline);
                for (User user : assignedUsers) {
                    log.info("Creating TaskInstance for user: {}", user.getFirstName());
                    taskInstanceService.createTaskInstance(savedDefinition, user, deadline);
                }
            } else {
                log.info(
                        "ONCE task '{}' has a future start date ({}), instances will be created on the start date",
                        title, startDate);
            }
        } else {
            // For recurring tasks: if today is a valid due date and the start date is not
            // in the future, create an instance immediately so the user gets notified right
            // away.
            LocalDate today = LocalDate.now();
            if (startDate == null || !startDate.isAfter(today)) {
                LocalDate dueDate = calculateNextDueDate(today, savedDefinition);
                if (dueDate != null
                        && (seriesEndDate == null || !dueDate.isAfter(seriesEndDate))) {
                    log.info("Creating initial TaskInstances for recurring task '{}' (due today: {})", title, dueDate);
                    for (User user : assignedUsers) {
                        taskInstanceService.createTaskInstance(savedDefinition, user, dueDate.atTime(23, 59, 59));
                    }
                } else {
                    log.info(
                            "Recurring task '{}': today is not a due date (recurrenceType={}), first instance will be created by scheduler",
                            title, recurrenceType);
                }
            } else {
                log.info("Recurring task '{}' has a future start date ({}), instances will be created by scheduler",
                        title, startDate);
            }
        }

        return savedDefinition;
    }

    @Transactional(readOnly = true)
    public List<TaskDefinition> findByCreatedBy(@NonNull User createdBy) {
        return taskDefinitionRepository.findByCreatedBy(createdBy);
    }

    @Transactional(readOnly = true)
    public List<TaskDefinition> findByAssignedUser(@NonNull User user) {
        return taskDefinitionRepository.findByAssignedUsersContaining(user);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<TaskDefinition> findById(@NonNull Long id) {
        return taskDefinitionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<TaskDefinition> findAll() {
        return taskDefinitionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TaskDefinition> findByAssignedUserId(@NonNull Long userId) {
        return taskDefinitionRepository.findAll().stream()
                .filter(def -> def.getAssignedUsers().stream()
                        .anyMatch(user -> user.getId().equals(userId)))
                .toList();
    }

    @Transactional
    public TaskDefinition save(@NonNull TaskDefinition definition) {
        return taskDefinitionRepository.save(definition);
    }

    @Transactional
    public void delete(@NonNull TaskDefinition definition) {
        taskInstanceService.deleteByTaskDefinitionId(definition.getId());
        taskDefinitionRepository.delete(definition);
    }

    @Transactional
    public void deleteTaskDefinition(@NonNull Long id) {
        if (!taskDefinitionRepository.existsById(id)) {
            throw new IllegalArgumentException("TaskDefinition not found");
        }
        // First delete all associated TaskInstances
        taskInstanceService.deleteByTaskDefinitionId(id);
        // Then delete the TaskDefinition
        taskDefinitionRepository.deleteById(id);
    }

    @Transactional
    public TaskDefinition updateTaskDefinition(@NonNull Long id, @NonNull String title, String description,
            int basePoints,
            @NonNull RecurrenceType recurrenceType, @NonNull Set<User> assignedUsers,
            LocalDate startDate, LocalDate seriesEndDate, String weeklyDays) {
        return updateTaskDefinition(id, title, description, basePoints, recurrenceType, assignedUsers,
                startDate, seriesEndDate, null, weeklyDays);
    }

    @Transactional
    public TaskDefinition updateTaskDefinition(@NonNull Long id, @NonNull String title, String description,
            int basePoints,
            @NonNull RecurrenceType recurrenceType, @NonNull Set<User> assignedUsers,
            LocalDate startDate, LocalDate seriesEndDate, LocalDateTime deadline, String weeklyDays) {
        TaskDefinition definition = taskDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TaskDefinition not found"));

        // Remember newly assigned users before overwriting
        Set<User> previousUsers = definition.getAssignedUsers() != null
                ? new java.util.HashSet<>(definition.getAssignedUsers())
                : new java.util.HashSet<>();

        definition.setTitle(title);
        definition.setDescription(description);
        definition.setBasePoints(basePoints);
        definition.setRecurrenceType(recurrenceType);
        definition.setAssignedUsers(assignedUsers);
        definition.setStartDate(startDate);
        definition.setSeriesEndDate(seriesEndDate);
        definition.setDeadline(deadline);
        definition.setWeeklyDays(weeklyDays);
        TaskDefinition saved = taskDefinitionRepository.save(definition);

        // --- 1. Remove open instances for users who are no longer assigned ---
        for (User removedUser : previousUsers) {
            boolean stillAssigned = assignedUsers.stream().anyMatch(u -> u.getId().equals(removedUser.getId()));
            if (!stillAssigned) {
                log.info("User '{}' removed from task '{}': deleting open instances", removedUser.getFirstName(),
                        title);
                taskInstanceService.deleteOpenInstancesForUser(saved.getId(), removedUser);
            }
        }

        // --- 2. Handle instances for currently assigned users ---
        LocalDate today = LocalDate.now();

        if (recurrenceType == RecurrenceType.ONCE) {
            if (startDate == null || !startDate.isAfter(today)) {
                for (User user : assignedUsers) {
                    boolean isNew = previousUsers.stream().noneMatch(p -> p.getId().equals(user.getId()));
                    if (isNew) {
                        log.info("Creating TaskInstance for newly assigned user '{}' on ONCE task '{}'",
                                user.getFirstName(), title);
                        taskInstanceService.createTaskInstance(saved, user, deadline);
                    } else if (startDate != null) {
                        // Start date was set/changed: remove instances before the new start date
                        taskInstanceService.deleteOpenInstancesBeforeDate(saved.getId(), user, startDate);
                    }
                }
            } else {
                // New start date is in the future: remove all open instances for everyone
                for (User user : assignedUsers) {
                    taskInstanceService.deleteOpenInstancesBeforeDate(saved.getId(), user, startDate);
                }
            }
        } else {
            // --- Recurring tasks ---
            // 2a. If start date moved into the future: remove instances before the new
            // start date
            if (startDate != null && startDate.isAfter(today)) {
                for (User user : assignedUsers) {
                    taskInstanceService.deleteOpenInstancesBeforeDate(saved.getId(), user, startDate);
                }
            } else {
                // 2b. Check if today is still a valid due date
                LocalDate dueDate = calculateNextDueDate(today, saved);
                boolean todayIsValid = dueDate != null
                        && (saved.getSeriesEndDate() == null || !dueDate.isAfter(saved.getSeriesEndDate()));

                for (User user : assignedUsers) {
                    boolean isNew = previousUsers.stream().noneMatch(p -> p.getId().equals(user.getId()));
                    if (!todayIsValid) {
                        // Today no longer matches the schedule: remove today's open instances
                        taskInstanceService.deleteOpenInstancesBeforeDate(saved.getId(), user, today.plusDays(1));
                        log.debug("Today is no longer a due date for task '{}': removed today's open instance for '{}'",
                                title, user.getFirstName());
                    } else if (isNew) {
                        // New user and today is valid: create instance if none exists yet
                        boolean exists = taskInstanceService.findByUser(user).stream()
                                .anyMatch(inst -> inst.getTaskDefinition().getId().equals(saved.getId())
                                        && inst.getDeadline() != null
                                        && inst.getDeadline().toLocalDate().equals(dueDate));
                        if (!exists) {
                            log.info(
                                    "Creating TaskInstance for newly assigned user '{}' on recurring task '{}' (due today: {})",
                                    user.getFirstName(), title, dueDate);
                            taskInstanceService.createTaskInstance(saved, user, dueDate.atTime(23, 59, 59));
                        }
                    }
                }
            }
        }

        return saved;
    }

    /**
     * For a set of newly assigned users, immediately creates a TaskInstance
     * if the recurring task is due today and no instance exists yet.
     * Called by the API controller after a save().
     */
    @Transactional
    public void createInstancesForNewUsersIfDueToday(@NonNull TaskDefinition definition,
            @NonNull Set<User> newlyAssigned) {
        if (newlyAssigned.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (definition.getStartDate() != null && definition.getStartDate().isAfter(today)) {
            return;
        }
        LocalDate dueDate = calculateNextDueDate(today, definition);
        if (dueDate == null) {
            return;
        }
        if (definition.getSeriesEndDate() != null && dueDate.isAfter(definition.getSeriesEndDate())) {
            return;
        }
        for (User user : newlyAssigned) {
            boolean exists = taskInstanceService.findByUser(user).stream()
                    .anyMatch(inst -> inst.getTaskDefinition().getId().equals(definition.getId())
                            && inst.getDeadline() != null
                            && inst.getDeadline().toLocalDate().equals(dueDate));
            if (!exists) {
                log.info("Creating TaskInstance for newly assigned user '{}' on recurring task '{}' (due today: {})",
                        user.getFirstName(), definition.getTitle(), dueDate);
                taskInstanceService.createTaskInstance(definition, user, dueDate.atTime(23, 59, 59));
            }
        }
    }

    // Check every day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void generateTaskInstances() {
        LocalDate today = LocalDate.now();
        List<TaskDefinition> definitions = taskDefinitionRepository.findAll();

        for (TaskDefinition definition : definitions) {
            if (definition.getStartDate() != null && definition.getStartDate().isAfter(today)) {
                continue;
            }

            if (definition.getRecurrenceType() == RecurrenceType.ONCE) {
                // ONCE tasks with start date: create instances only on the start date
                // (without start date they were already created immediately upon creation)
                if (definition.getStartDate() != null && definition.getStartDate().equals(today)) {
                    for (User user : definition.getAssignedUsers()) {
                        boolean exists = taskInstanceService.findByUser(user).stream()
                                .anyMatch(inst -> inst.getTaskDefinition().getId().equals(definition.getId()));
                        if (!exists) {
                            log.info("Creating delayed ONCE TaskInstance: task='{}', user='{}'",
                                    definition.getTitle(), user.getFirstName());
                            taskInstanceService.createTaskInstance(definition, user, definition.getDeadline());
                        }
                    }
                }
            } else {
                // Recurring tasks
                for (User user : definition.getAssignedUsers()) {
                    LocalDate dueDate = calculateNextDueDate(today, definition);
                    if (dueDate != null
                            && (definition.getSeriesEndDate() == null || dueDate.isBefore(definition.getSeriesEndDate())
                                    || dueDate.equals(definition.getSeriesEndDate()))) {
                        // Check if an instance for this day already exists
                        boolean exists = taskInstanceService.findByUser(user).stream()
                                .anyMatch(instance -> instance.getTaskDefinition().equals(definition)
                                        && instance.getDeadline() != null
                                        && instance.getDeadline().toLocalDate().equals(dueDate));
                        if (!exists) {
                            taskInstanceService.createTaskInstance(definition, user, dueDate.atTime(23, 59, 59));
                        }
                    }
                }
            }
        }

        // Expire overdue tasks
        expireOverdueTasks(today);
    }

    private void expireOverdueTasks(LocalDate today) {
        List<TaskInstance> overdueTasks = taskInstanceService.findOverduePending(today);
        for (TaskInstance task : overdueTasks) {
            task.setStatus(TaskStatus.EXPIRED);
            log.info("Expiring overdue task: {} for user {}", task.getTaskDefinition().getTitle(),
                    task.getAssignedUser().getFirstName());
            // Send notification
            notificationService.createLocalizedNotification(task.getAssignedUser(), NotificationType.TASK_EXPIRED,
                    "notification.task.expired.title",
                    "notification.task.expired.message",
                    new Object[] { task.getTaskDefinition().getTitle() },
                    task.getTaskDefinition().getId());

            if (task.getTaskDefinition().getRecurrenceType() == RecurrenceType.ONCE
                    && task.getTaskDefinition().getCreatedBy() != null) {
                notificationService.createLocalizedNotification(
                        task.getTaskDefinition().getCreatedBy(),
                        NotificationType.TASK_EXPIRED,
                        "notification.task.deadline_missed.title",
                        "notification.task.deadline_missed.message",
                        new Object[] { task.getTaskDefinition().getTitle(), task.getAssignedUser().getFirstName() },
                        task.getId());
            }
        }
    }

    private LocalDate calculateNextDueDate(LocalDate today, TaskDefinition definition) {
        switch (definition.getRecurrenceType()) {
            case WEEKLY:
                if (matchesWeeklySchedule(today, definition.getWeeklyDays())) {
                    return today;
                }
                return null;
            case MONTHLY:
                if (definition.getWeeklyDays() == null || definition.getWeeklyDays().isBlank()) {
                    return null;
                }

                String schedule = definition.getWeeklyDays();
                if (schedule.startsWith("MD:")) {
                    String[] parts = schedule.split(":", 3);
                    if (parts.length < 2) {
                        return null;
                    }

                    int dayOfMonth;
                    try {
                        dayOfMonth = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                    if (dayOfMonth < 1 || dayOfMonth > 31) {
                        return null;
                    }

                    boolean adjustToLast = parts.length >= 3 && "LAST".equalsIgnoreCase(parts[2]);
                    int lastDayOfMonth = today.lengthOfMonth();
                    int effectiveDay = dayOfMonth;
                    if (dayOfMonth > lastDayOfMonth) {
                        if (!adjustToLast) {
                            return null;
                        }
                        effectiveDay = lastDayOfMonth;
                    }

                    if (today.getDayOfMonth() == effectiveDay) {
                        return today;
                    }
                    return null;
                }

                if (schedule.startsWith("MW:")) {
                    String[] parts = schedule.split(":", 3);
                    if (parts.length < 3 || parts[2].isBlank()) {
                        return null;
                    }

                    int monthlyWeekNumber;
                    try {
                        monthlyWeekNumber = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        return null;
                    }

                    if (monthlyWeekNumber < 1 || monthlyWeekNumber > 5) {
                        return null;
                    }

                    Set<DayOfWeek> monthlyDays = Arrays.stream(parts[2].split(","))
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .map(this::parseDayOfWeek)
                            .filter(java.util.Objects::nonNull)
                            .collect(java.util.stream.Collectors.toSet());

                    int weekOfMonth = ((today.getDayOfMonth() - 1) / 7) + 1;
                    if (monthlyDays.contains(today.getDayOfWeek()) && weekOfMonth == monthlyWeekNumber) {
                        return today;
                    }
                    return null;
                }

                // Backward compatibility for legacy format W2:MONDAY
                if (schedule.startsWith("W") && schedule.contains(":")) {
                    String[] parts = schedule.split(":", 2);
                    int monthlyWeekNumber;
                    try {
                        monthlyWeekNumber = Integer.parseInt(parts[0].substring(1));
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        return null;
                    }

                    if (monthlyWeekNumber < 1 || monthlyWeekNumber > 5 || parts.length < 2 || parts[1].isBlank()) {
                        return null;
                    }

                    Set<DayOfWeek> monthlyDays = Arrays.stream(parts[1].split(","))
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .map(this::parseDayOfWeek)
                            .filter(java.util.Objects::nonNull)
                            .collect(java.util.stream.Collectors.toSet());

                    int weekOfMonth = ((today.getDayOfMonth() - 1) / 7) + 1;
                    if (monthlyDays.contains(today.getDayOfWeek()) && weekOfMonth == monthlyWeekNumber) {
                        return today;
                    }
                    return null;
                }

                return null;
            default:
                return null;
        }
    }

    private boolean matchesWeeklySchedule(LocalDate today, String weeklyDaysRaw) {
        if (weeklyDaysRaw == null || weeklyDaysRaw.isBlank()) {
            return false;
        }

        Set<DayOfWeek> days = Arrays.stream(weeklyDaysRaw.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .map(this::parseDayOfWeek)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        return days.contains(today.getDayOfWeek());
    }

    private DayOfWeek parseDayOfWeek(String value) {
        try {
            return DayOfWeek.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}