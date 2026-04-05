package org.openfamilycompass.api.v1;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openfamilycompass.api.v1.dto.TaskDto;
import org.openfamilycompass.model.RecurrenceType;
import org.openfamilycompass.model.TaskDefinition;
import org.openfamilycompass.model.TaskInstance;
import org.openfamilycompass.model.TaskStatus;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.service.TaskDefinitionService;
import org.openfamilycompass.service.TaskInstanceService;
import org.openfamilycompass.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tasks", description = "Task definitions and instances")
public class TaskApiController {

    private final TaskDefinitionService taskDefinitionService;
    private final TaskInstanceService taskInstanceService;
    private final UserService userService;

    // ============ TASK DEFINITIONS ============

    @GetMapping("/definitions")
    @Operation(summary = "List task definitions")
    public ResponseEntity<List<TaskDto.DefinitionResponse>> listTaskDefinitions(
            @RequestParam(required = false) Long assignedUserId) {

        List<TaskDefinition> definitions;
        if (assignedUserId != null) {
            definitions = taskDefinitionService.findByAssignedUserId(assignedUserId);
        } else {
            definitions = taskDefinitionService.findAll();
        }

        return ResponseEntity.ok(
                definitions.stream()
                        .map(TaskDto.DefinitionResponse::fromEntity)
                        .collect(Collectors.toList()));
    }

    @PostMapping("/definitions")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Create a new task definition")
    public ResponseEntity<TaskDto.DefinitionResponse> createTaskDefinition(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TaskDto.CreateDefinitionRequest request) {

        User currentUser = getCurrentUser(jwt);

        Set<User> assignedUsers = new HashSet<>();
        for (Long userId : request.getAssignedUserIds()) {
            User user = userService.findById(userId)
                    .orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found: " + userId));
            assignedUsers.add(user);
        }

        validateRecurrenceConfiguration(
                request.getRecurrenceType(),
                request.getWeeklyDays(),
                request.getMonthlyMode(),
                request.getMonthlyWeekNumber(),
                request.getMonthlyDayOfMonth());

        String weeklyDaysPayload = buildSchedulePayload(
                request.getRecurrenceType(),
                request.getWeeklyDays(),
                request.getMonthlyMode(),
                request.getMonthlyWeekNumber(),
                request.getMonthlyDayOfMonth(),
                request.getMonthlyAdjustToLastDay());

        LocalDateTime deadline = request.getRecurrenceType() == RecurrenceType.ONCE ? request.getDeadline() : null;
        LocalDate seriesEndDate = request.getRecurrenceType() != RecurrenceType.ONCE ? request.getSeriesEndDate()
                : null;

        TaskDefinition saved = taskDefinitionService.createTaskDefinition(
                request.getTitle(),
                request.getDescription(),
                request.getBasePoints(),
                request.getRecurrenceType(),
                assignedUsers,
                currentUser,
                request.getStartDate(),
                seriesEndDate,
                deadline,
                weeklyDaysPayload);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskDto.DefinitionResponse.fromEntity(saved));
    }

    @GetMapping("/definitions/{id}")
    @Operation(summary = "Get task definition by ID")
    public ResponseEntity<TaskDto.DefinitionResponse> getTaskDefinitionById(@PathVariable Long id) {
        TaskDefinition definition = taskDefinitionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task definition not found"));
        return ResponseEntity.ok(TaskDto.DefinitionResponse.fromEntity(definition));
    }

    @PutMapping("/definitions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Update task definition")
    public ResponseEntity<TaskDto.DefinitionResponse> updateTaskDefinition(
            @PathVariable Long id,
            @Valid @RequestBody TaskDto.UpdateDefinitionRequest request) {

        TaskDefinition definition = taskDefinitionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task definition not found"));

        if (request.getTitle() != null) {
            definition.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            definition.setDescription(request.getDescription());
        }
        if (request.getBasePoints() != null) {
            definition.setBasePoints(request.getBasePoints());
        }
        if (request.getRecurrenceType() != null) {
            definition.setRecurrenceType(request.getRecurrenceType());
        }
        if (request.getStartDate() != null) {
            definition.setStartDate(request.getStartDate());
        }
        if (request.getSeriesEndDate() != null) {
            definition.setSeriesEndDate(request.getSeriesEndDate());
        }
        if (request.getDeadline() != null) {
            definition.setDeadline(request.getDeadline());
        }
        RecurrenceType effectiveRecurrence = definition.getRecurrenceType();
        if (request.getWeeklyDays() != null || request.getMonthlyWeekNumber() != null
                || request.getMonthlyMode() != null
                || request.getMonthlyDayOfMonth() != null || request.getMonthlyAdjustToLastDay() != null
                || request.getRecurrenceType() != null) {
            String currentSchedule = definition.getWeeklyDays();
            List<String> effectiveDays = request.getWeeklyDays() != null
                    ? request.getWeeklyDays()
                    : extractDaysFromStoredSchedule(currentSchedule, effectiveRecurrence);
            String effectiveMonthlyMode = request.getMonthlyMode() != null
                    ? request.getMonthlyMode()
                    : extractMonthlyMode(currentSchedule);
            Integer effectiveMonthlyWeekNumber = request.getMonthlyWeekNumber() != null
                    ? request.getMonthlyWeekNumber()
                    : extractMonthlyWeekNumber(currentSchedule);
            Integer effectiveMonthlyDayOfMonth = request.getMonthlyDayOfMonth() != null
                    ? request.getMonthlyDayOfMonth()
                    : extractMonthlyDayOfMonth(currentSchedule);
            Boolean effectiveMonthlyAdjustToLastDay = request.getMonthlyAdjustToLastDay() != null
                    ? request.getMonthlyAdjustToLastDay()
                    : extractMonthlyAdjustToLastDay(currentSchedule);

            validateRecurrenceConfiguration(
                    effectiveRecurrence,
                    effectiveDays,
                    effectiveMonthlyMode,
                    effectiveMonthlyWeekNumber,
                    effectiveMonthlyDayOfMonth);
            definition.setWeeklyDays(buildSchedulePayload(
                    effectiveRecurrence,
                    effectiveDays,
                    effectiveMonthlyMode,
                    effectiveMonthlyWeekNumber,
                    effectiveMonthlyDayOfMonth,
                    effectiveMonthlyAdjustToLastDay));
        }
        if (request.getAssignedUserIds() != null) {
            Set<User> previousUsers = new HashSet<>(definition.getAssignedUsers());
            Set<User> assignedUsers = new HashSet<>();
            for (Long userId : request.getAssignedUserIds()) {
                User user = userService.findById(userId)
                        .orElseThrow(
                                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found: " + userId));
                assignedUsers.add(user);
            }
            Set<User> newlyAssigned = assignedUsers.stream()
                    .filter(u -> previousUsers.stream().noneMatch(p -> p.getId().equals(u.getId())))
                    .collect(java.util.stream.Collectors.toSet());
            definition.setAssignedUsers(assignedUsers);

            if (definition.getRecurrenceType() != RecurrenceType.ONCE) {
                definition.setDeadline(null);
            }

            TaskDefinition saved = taskDefinitionService.save(definition);

            // For ONCE tasks: create TaskInstances for newly assigned users (after
            // the save!)
            // For recurring tasks: create immediately if today is a due date
            if (saved.getRecurrenceType() == RecurrenceType.ONCE) {
                // Only create immediately if no future start date is set
                LocalDate today = LocalDate.now();
                if (saved.getStartDate() == null || !saved.getStartDate().isAfter(today)) {
                    for (User newUser : newlyAssigned) {
                        taskInstanceService.createTaskInstance(saved, newUser, saved.getDeadline());
                        log.info("Created TaskInstance for newly assigned user '{}' on ONCE task '{}'",
                                newUser.getUsername(), saved.getTitle());
                    }
                }
            } else {
                taskDefinitionService.createInstancesForNewUsersIfDueToday(saved, newlyAssigned);
            }

            return ResponseEntity.ok(TaskDto.DefinitionResponse.fromEntity(saved));
        }

        if (definition.getRecurrenceType() != RecurrenceType.ONCE) {
            definition.setDeadline(null);
        }

        TaskDefinition saved = taskDefinitionService.save(definition);
        return ResponseEntity.ok(TaskDto.DefinitionResponse.fromEntity(saved));
    }

    @DeleteMapping("/definitions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Delete task definition")
    public ResponseEntity<Void> deleteTaskDefinition(@PathVariable Long id) {
        TaskDefinition definition = taskDefinitionService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task definition not found"));
        taskDefinitionService.delete(definition);
        return ResponseEntity.noContent().build();
    }

    // ============ TASK INSTANCES ============

    @GetMapping("/instances")
    @Operation(summary = "List task instances")
    public ResponseEntity<List<TaskDto.InstanceResponse>> listTaskInstances(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) LocalDate deadlineFrom,
            @RequestParam(required = false) LocalDate deadlineTo) {

        User currentUser = getCurrentUser(jwt);
        List<TaskInstance> instances;

        if (currentUser.getRole() == UserRole.CHILD) {
            instances = taskInstanceService.findByAssignedUser(currentUser);
            log.debug("[listTaskInstances] CHILD user '{}' (id={}) → {} instances found",
                    currentUser.getUsername(), currentUser.getId(), instances.size());
        } else if (assignedUserId != null) {
            User assignedUser = userService.findById(assignedUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
            instances = taskInstanceService.findByAssignedUser(assignedUser);
        } else {
            instances = taskInstanceService.findAll();
        }

        if (status != null) {
            instances = instances.stream()
                    .filter(i -> i.getStatus() == status)
                    .collect(Collectors.toList());
        }
        if (deadlineFrom != null) {
            instances = instances.stream()
                    .filter(i -> i.getDeadline() == null || !i.getDeadline().toLocalDate().isBefore(deadlineFrom))
                    .collect(Collectors.toList());
        }
        if (deadlineTo != null) {
            instances = instances.stream()
                    .filter(i -> i.getDeadline() == null || !i.getDeadline().toLocalDate().isAfter(deadlineTo))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(
                instances.stream()
                        .sorted((a, b) -> {
                            // Tasks without deadline first (due immediately), then by deadline ascending
                            if (a.getDeadline() == null && b.getDeadline() == null) {
                                // Same deadline situation: newest first
                                if (a.getCreatedAt() == null)
                                    return 1;
                                if (b.getCreatedAt() == null)
                                    return -1;
                                return b.getCreatedAt().compareTo(a.getCreatedAt());
                            }
                            if (a.getDeadline() == null)
                                return -1;
                            if (b.getDeadline() == null)
                                return 1;
                            return a.getDeadline().compareTo(b.getDeadline());
                        })
                        .map(TaskDto.InstanceResponse::fromEntity)
                        .collect(Collectors.toList()));
    }

    @GetMapping("/instances/{id}")
    @Operation(summary = "Get task instance by ID")
    public ResponseEntity<TaskDto.InstanceResponse> getTaskInstanceById(@PathVariable Long id) {
        TaskInstance instance = taskInstanceService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task instance not found"));
        return ResponseEntity.ok(TaskDto.InstanceResponse.fromEntity(instance));
    }

    @PostMapping("/instances/{id}/complete")
    @PreAuthorize("hasRole('CHILD')")
    @Operation(summary = "Mark task as completed (Child)")
    public ResponseEntity<TaskDto.InstanceResponse> completeTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        User currentUser = getCurrentUser(jwt);
        TaskInstance instance = taskInstanceService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task instance not found"));

        if (!instance.getAssignedUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not assigned to this task");
        }

        if (instance.getStatus() != TaskStatus.PENDING && instance.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task cannot be completed in current status");
        }

        TaskInstance completed = taskInstanceService.markAsCompleted(instance.getId(), currentUser);
        return ResponseEntity.ok(TaskDto.InstanceResponse.fromEntity(completed));
    }

    @PostMapping("/instances/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Approve completed task (Parent)")
    public ResponseEntity<TaskDto.InstanceResponse> approveTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody(required = false) TaskDto.ApproveRequest request) {

        User currentUser = getCurrentUser(jwt);
        TaskInstance instance = taskInstanceService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task instance not found"));

        if (instance.getStatus() != TaskStatus.CHILD_COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task cannot be approved in current status");
        }

        Integer points = request != null && request.getAwardedPoints() != null
                ? request.getAwardedPoints()
                : instance.getTaskDefinition().getBasePoints();
        String notes = request != null ? request.getNotes() : null;

        TaskInstance approved = taskInstanceService.approveTask(instance.getId(), currentUser, points, notes);
        return ResponseEntity.ok(TaskDto.InstanceResponse.fromEntity(approved));
    }

    @PostMapping("/instances/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Reject completed task (Parent)")
    public ResponseEntity<TaskDto.InstanceResponse> rejectTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody(required = false) TaskDto.RejectRequest request) {

        User currentUser = getCurrentUser(jwt);
        TaskInstance instance = taskInstanceService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task instance not found"));

        if (instance.getStatus() != TaskStatus.CHILD_COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task cannot be rejected in current status");
        }

        String notes = request != null ? request.getNotes() : null;
        TaskInstance rejected = taskInstanceService.rejectTask(instance.getId(), currentUser, notes);
        return ResponseEntity.ok(TaskDto.InstanceResponse.fromEntity(rejected));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARENT')")
    @Operation(summary = "Get pending tasks for approval")
    public ResponseEntity<List<TaskDto.InstanceResponse>> getPendingTasks() {
        List<TaskInstance> pending = taskInstanceService.findByStatus(TaskStatus.CHILD_COMPLETED);
        return ResponseEntity.ok(
                pending.stream()
                        .map(TaskDto.InstanceResponse::fromEntity)
                        .collect(Collectors.toList()));
    }

    private User getCurrentUser(Jwt jwt) {
        String username = jwt.getSubject();
        return userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private void validateRecurrenceConfiguration(RecurrenceType recurrenceType, List<String> weeklyDays,
            String monthlyMode, Integer monthlyWeekNumber, Integer monthlyDayOfMonth) {
        if (recurrenceType == RecurrenceType.WEEKLY) {
            if (weeklyDays == null || weeklyDays.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "weeklyDays is required for WEEKLY tasks");
            }
        }

        if (recurrenceType == RecurrenceType.MONTHLY) {
            if (monthlyMode == null || monthlyMode.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "monthlyMode is required for MONTHLY tasks");
            }

            if ("WEEKDAY_PATTERN".equalsIgnoreCase(monthlyMode)) {
                if (weeklyDays == null || weeklyDays.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "weeklyDays is required for MONTHLY WEEKDAY_PATTERN tasks");
                }
                if (monthlyWeekNumber == null || monthlyWeekNumber < 1 || monthlyWeekNumber > 5) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "monthlyWeekNumber must be between 1 and 5 for MONTHLY WEEKDAY_PATTERN tasks");
                }
                return;
            }

            if ("DAY_OF_MONTH".equalsIgnoreCase(monthlyMode)) {
                if (monthlyDayOfMonth == null || monthlyDayOfMonth < 1 || monthlyDayOfMonth > 31) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "monthlyDayOfMonth must be between 1 and 31 for MONTHLY DAY_OF_MONTH tasks");
                }
                return;
            }

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "monthlyMode must be DAY_OF_MONTH or WEEKDAY_PATTERN for MONTHLY tasks");
        }
    }

    private String buildSchedulePayload(RecurrenceType recurrenceType, List<String> weeklyDays, String monthlyMode,
            Integer monthlyWeekNumber, Integer monthlyDayOfMonth, Boolean monthlyAdjustToLastDay) {
        if (recurrenceType == RecurrenceType.ONCE) {
            return null;
        }

        if (recurrenceType == RecurrenceType.WEEKLY) {
            if (weeklyDays == null || weeklyDays.isEmpty()) {
                return null;
            }
            return String.join(",", weeklyDays);
        }

        if ("DAY_OF_MONTH".equalsIgnoreCase(monthlyMode)) {
            return "MD:" + monthlyDayOfMonth + ":" + (Boolean.TRUE.equals(monthlyAdjustToLastDay) ? "LAST" : "SKIP");
        }

        if (weeklyDays == null || weeklyDays.isEmpty()) {
            return null;
        }
        return "MW:" + monthlyWeekNumber + ":" + String.join(",", weeklyDays);
    }

    private List<String> extractDaysFromStoredSchedule(String storedValue, RecurrenceType recurrenceType) {
        if (storedValue == null || storedValue.isBlank()) {
            return List.of();
        }

        if (recurrenceType == RecurrenceType.MONTHLY && storedValue.startsWith("MW:") && storedValue.contains(":")) {
            String[] parts = storedValue.split(":", 3);
            if (parts.length < 3 || parts[2].isBlank()) {
                return List.of();
            }
            return Arrays.asList(parts[2].split(","));
        }

        if (recurrenceType == RecurrenceType.MONTHLY && storedValue.startsWith("W") && storedValue.contains(":")) {
            // Backward compatibility for old W2:MONDAY format
            String[] parts = storedValue.split(":", 2);
            if (parts.length < 2 || parts[1].isBlank()) {
                return List.of();
            }
            return Arrays.asList(parts[1].split(","));
        }

        return Arrays.asList(storedValue.split(","));
    }

    private Integer extractMonthlyWeekNumber(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }

        String prefix;
        if (storedValue.startsWith("MW:")) {
            String[] parts = storedValue.split(":", 3);
            if (parts.length < 2) {
                return null;
            }
            prefix = parts[1];
        } else if (storedValue.startsWith("W") && storedValue.contains(":")) {
            prefix = storedValue.split(":", 2)[0].substring(1);
        } else {
            return null;
        }

        try {
            return Integer.parseInt(prefix);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return null;
        }
    }

    private String extractMonthlyMode(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }
        if (storedValue.startsWith("MD:")) {
            return "DAY_OF_MONTH";
        }
        if (storedValue.startsWith("MW:") || (storedValue.startsWith("W") && storedValue.contains(":"))) {
            return "WEEKDAY_PATTERN";
        }
        return null;
    }

    private Integer extractMonthlyDayOfMonth(String storedValue) {
        if (storedValue == null || !storedValue.startsWith("MD:")) {
            return null;
        }
        String[] parts = storedValue.split(":", 3);
        if (parts.length < 2) {
            return null;
        }
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean extractMonthlyAdjustToLastDay(String storedValue) {
        if (storedValue == null || !storedValue.startsWith("MD:")) {
            return null;
        }
        String[] parts = storedValue.split(":", 3);
        if (parts.length < 3) {
            return null;
        }
        return "LAST".equalsIgnoreCase(parts[2]);
    }
}
