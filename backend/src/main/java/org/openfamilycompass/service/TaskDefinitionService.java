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
            @NonNull User createdBy, LocalDate startDate, LocalDate endDate, String weeklyDays) {
        return createTaskDefinition(title, description, basePoints, recurrenceType, assignedUsers, createdBy,
                startDate, endDate, null, weeklyDays);
    }

    @Transactional
    public TaskDefinition createTaskDefinition(@NonNull String title, String description, int basePoints,
            @NonNull RecurrenceType recurrenceType, @NonNull Set<User> assignedUsers,
            @NonNull User createdBy, LocalDate startDate, LocalDate endDate, LocalDateTime endAt, String weeklyDays) {
        TaskDefinition definition = new TaskDefinition();
        definition.setTitle(title);
        definition.setDescription(description);
        definition.setBasePoints(basePoints);
        definition.setRecurrenceType(recurrenceType);
        definition.setAssignedUsers(assignedUsers);
        definition.setCreatedBy(createdBy);
        definition.setStartDate(startDate);
        definition.setEndDate(endDate);
        definition.setEndAt(endAt);
        definition.setWeeklyDays(weeklyDays);

        TaskDefinition savedDefinition = taskDefinitionRepository.save(definition);

        // Für einmalige Aufgaben: Erstelle sofort TaskInstances
        // Due Date = End Date (kann null sein)
        log.info("TaskDefinition created: title='{}', recurrenceType='{}', endDate='{}', assignedUsers.size={}",
                title, recurrenceType, endDate, assignedUsers.size());
        if (recurrenceType == RecurrenceType.ONCE) {
            log.info("Creating TaskInstances for ONCE task '{}' with {} assigned users, dueDate='{}'",
                    title, assignedUsers.size(), endDate);
            for (User user : assignedUsers) {
                log.info("Creating TaskInstance for user: {}", user.getFirstName());
                if (endAt != null) {
                    taskInstanceService.createTaskInstance(savedDefinition, user, endDate, endAt);
                } else {
                    taskInstanceService.createTaskInstance(savedDefinition, user, endDate);
                }
            }
        } else {
            log.info("NOT creating TaskInstances: recurrenceType={}, endDate={}", recurrenceType, endDate);
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
        // Lösche zuerst alle zugehörigen TaskInstances
        taskInstanceService.deleteByTaskDefinitionId(id);
        // Dann lösche die TaskDefinition
        taskDefinitionRepository.deleteById(id);
    }

    @Transactional
    public TaskDefinition updateTaskDefinition(@NonNull Long id, @NonNull String title, String description,
            int basePoints,
            @NonNull RecurrenceType recurrenceType, @NonNull Set<User> assignedUsers,
            LocalDate startDate, LocalDate endDate, String weeklyDays) {
        return updateTaskDefinition(id, title, description, basePoints, recurrenceType, assignedUsers,
                startDate, endDate, null, weeklyDays);
    }

    @Transactional
    public TaskDefinition updateTaskDefinition(@NonNull Long id, @NonNull String title, String description,
            int basePoints,
            @NonNull RecurrenceType recurrenceType, @NonNull Set<User> assignedUsers,
            LocalDate startDate, LocalDate endDate, LocalDateTime endAt, String weeklyDays) {
        TaskDefinition definition = taskDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TaskDefinition not found"));
        definition.setTitle(title);
        definition.setDescription(description);
        definition.setBasePoints(basePoints);
        definition.setRecurrenceType(recurrenceType);
        definition.setAssignedUsers(assignedUsers);
        definition.setStartDate(startDate);
        definition.setEndDate(endDate);
        definition.setEndAt(endAt);
        definition.setWeeklyDays(weeklyDays);
        return taskDefinitionRepository.save(definition);
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

            // Für wiederholende Aufgaben (einmalige werden sofort erstellt)
            if (definition.getRecurrenceType() != RecurrenceType.ONCE) {
                for (User user : definition.getAssignedUsers()) {
                    LocalDate dueDate = calculateNextDueDate(today, definition);
                    if (dueDate != null && (definition.getEndDate() == null || dueDate.isBefore(definition.getEndDate())
                            || dueDate.equals(definition.getEndDate()))) {
                        // Prüfe, ob bereits eine Instanz für diesen Tag existiert
                        boolean exists = taskInstanceService.findByUser(user).stream()
                                .anyMatch(instance -> instance.getTaskDefinition().equals(definition)
                                        && instance.getDueDate().equals(dueDate));
                        if (!exists) {
                            taskInstanceService.createTaskInstance(definition, user, dueDate);
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
            notificationService.createNotification(task.getAssignedUser(), NotificationType.TASK_EXPIRED,
                    "Aufgabe abgelaufen", "Aufgabe '" + task.getTaskDefinition().getTitle() + "' ist abgelaufen.",
                    task.getTaskDefinition().getId());

            if (task.getTaskDefinition().getRecurrenceType() == RecurrenceType.ONCE
                    && task.getTaskDefinition().getCreatedBy() != null) {
                notificationService.createNotification(
                        task.getTaskDefinition().getCreatedBy(),
                        NotificationType.TASK_EXPIRED,
                        "Frist verpasst",
                        "Die Aufgabe '" + task.getTaskDefinition().getTitle() + "' von "
                                + task.getAssignedUser().getFirstName() + " ist abgelaufen.",
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