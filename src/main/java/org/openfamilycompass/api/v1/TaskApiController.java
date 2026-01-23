package org.openfamilycompass.api.v1;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openfamilycompass.api.v1.dto.TaskDto;
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

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
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

        TaskDefinition definition = new TaskDefinition();
        definition.setTitle(request.getTitle());
        definition.setDescription(request.getDescription());
        definition.setBasePoints(request.getBasePoints());
        definition.setRecurrenceType(request.getRecurrenceType());
        definition.setCreatedBy(currentUser);
        definition.setStartDate(request.getStartDate());
        definition.setEndDate(request.getEndDate());

        if (request.getWeeklyDays() != null && !request.getWeeklyDays().isEmpty()) {
            definition.setWeeklyDays(String.join(",", request.getWeeklyDays()));
        }

        Set<User> assignedUsers = new HashSet<>();
        for (Long userId : request.getAssignedUserIds()) {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found: " + userId));
            assignedUsers.add(user);
        }
        definition.setAssignedUsers(assignedUsers);

        TaskDefinition saved = taskDefinitionService.save(definition);
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
        if (request.getEndDate() != null) {
            definition.setEndDate(request.getEndDate());
        }
        if (request.getWeeklyDays() != null) {
            definition.setWeeklyDays(String.join(",", request.getWeeklyDays()));
        }
        if (request.getAssignedUserIds() != null) {
            Set<User> assignedUsers = new HashSet<>();
            for (Long userId : request.getAssignedUserIds()) {
                User user = userService.findById(userId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found: " + userId));
                assignedUsers.add(user);
            }
            definition.setAssignedUsers(assignedUsers);
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
            @RequestParam(required = false) LocalDate dueDateFrom,
            @RequestParam(required = false) LocalDate dueDateTo) {

        User currentUser = getCurrentUser(jwt);
        List<TaskInstance> instances;

        if (currentUser.getRole() == UserRole.CHILD) {
            instances = taskInstanceService.findByAssignedUser(currentUser);
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
        if (dueDateFrom != null) {
            instances = instances.stream()
                    .filter(i -> i.getDueDate() == null || !i.getDueDate().isBefore(dueDateFrom))
                    .collect(Collectors.toList());
        }
        if (dueDateTo != null) {
            instances = instances.stream()
                    .filter(i -> i.getDueDate() == null || !i.getDueDate().isAfter(dueDateTo))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(
                instances.stream()
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
}
