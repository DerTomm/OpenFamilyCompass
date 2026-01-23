package org.openfamilycompass.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
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
        TaskDefinition definition = new TaskDefinition();
        definition.setTitle(title);
        definition.setDescription(description);
        definition.setBasePoints(basePoints);
        definition.setRecurrenceType(recurrenceType);
        definition.setAssignedUsers(assignedUsers);
        definition.setCreatedBy(createdBy);
        definition.setStartDate(startDate);
        definition.setEndDate(endDate);
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
                taskInstanceService.createTaskInstance(savedDefinition, user, endDate);
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
        TaskDefinition definition = taskDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TaskDefinition not found"));
        definition.setTitle(title);
        definition.setDescription(description);
        definition.setBasePoints(basePoints);
        definition.setRecurrenceType(recurrenceType);
        definition.setAssignedUsers(assignedUsers);
        definition.setStartDate(startDate);
        definition.setEndDate(endDate);
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
        }
    }

    private LocalDate calculateNextDueDate(LocalDate today, TaskDefinition definition) {
        switch (definition.getRecurrenceType()) {
            case WEEKLY:
                if (definition.getWeeklyDays() != null && !definition.getWeeklyDays().isEmpty()) {
                    Set<DayOfWeek> days = Arrays.stream(definition.getWeeklyDays().split(","))
                            .map(String::trim)
                            .map(DayOfWeek::valueOf)
                            .collect(java.util.stream.Collectors.toSet());
                    DayOfWeek todayDay = today.getDayOfWeek();
                    if (days.contains(todayDay)) {
                        return today;
                    }
                }
                return null;
            default:
                return null;
        }
    }
}