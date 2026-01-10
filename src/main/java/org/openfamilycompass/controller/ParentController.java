package org.openfamilycompass.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openfamilycompass.model.Behavior;
import org.openfamilycompass.model.BehaviorEvaluation;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.RecurrenceType;
import org.openfamilycompass.model.Reward;
import org.openfamilycompass.model.RewardRedemption;
import org.openfamilycompass.model.TaskDefinition;
import org.openfamilycompass.model.TaskInstance;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.service.BehaviorEvaluationService;
import org.openfamilycompass.service.BehaviorService;
import org.openfamilycompass.service.PointService;
import org.openfamilycompass.service.PointTransactionWithBalance;
import org.openfamilycompass.service.RewardRedemptionService;
import org.openfamilycompass.service.RewardService;
import org.openfamilycompass.service.TaskDefinitionService;
import org.openfamilycompass.service.TaskInstanceService;
import org.openfamilycompass.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/parent")
@PreAuthorize("hasRole('PARENT')")
@RequiredArgsConstructor
@Slf4j
public class ParentController {

    private final UserService userService;
    private final TaskDefinitionService taskDefinitionService;
    private final TaskInstanceService taskInstanceService;
    private final RewardRedemptionService redemptionService;
    private final BehaviorService behaviorService;
    private final BehaviorEvaluationService behaviorEvaluationService;
    private final RewardService rewardService;
    private final PointService pointService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<User> children = userService.findAllByRole(UserRole.CHILD);

        // Ensure all children have current point totals
        children.forEach(child -> {
            pointService.updateUserPoints(child);
        });

        List<TaskInstance> pendingApprovals = taskInstanceService.findPendingApproval();
        List<RewardRedemption> pendingRedemptions = redemptionService.findPendingApprovals();

        model.addAttribute("children", children);
        model.addAttribute("pendingApprovals", pendingApprovals);
        model.addAttribute("pendingRedemptions", pendingRedemptions);

        return "parent/dashboard";
    }

    @GetMapping("/tasks/pending")
    public String pendingTasks(Model model) {
        List<TaskInstance> tasks = taskInstanceService.findPendingApproval();
        model.addAttribute("tasks", tasks);
        return "parent/tasks-pending";
    }

    // Task Management
    @GetMapping("/tasks")
    public String listTasks(Model model, Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<TaskDefinition> taskDefinitions = taskDefinitionService.findByCreatedBy(currentUser);
        List<User> children = userService.findAllByRole(UserRole.CHILD);
        model.addAttribute("taskDefinitions", taskDefinitions);
        model.addAttribute("children", children);
        return "parent/tasks";
    }

    @GetMapping("/tasks/create")
    public String createTaskForm(Model model) {
        List<User> children = userService.findAllByRole(UserRole.CHILD);
        model.addAttribute("children", children);
        model.addAttribute("recurrenceTypes", RecurrenceType.values());
        return "parent/task-create";
    }

    @PostMapping("/tasks/create")
    public String createTask(@ModelAttribute TaskDefinitionController form,
            Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Set<User> assignedUsers = form.getUserIds() != null ? form.getUserIds().stream()
                .map(id -> userService.findById(id).orElse(null))
                .filter(user -> user != null)
                .collect(java.util.stream.Collectors.toSet()) : Set.of();

        log.info("Creating task '{}' with recurrence '{}' and {} assigned users", form.getTitle(),
                form.getRecurrenceType(), assignedUsers.size());
        log.info("Form data: title='{}', recurrenceType='{}', startDate='{}', userIds={}",
                form.getTitle(), form.getRecurrenceType(), form.getStartDate(), form.getUserIds());

        // Für ONCE: Due Date == Start Date (behandelt als End Date im Service)
        LocalDate effectiveEndDate = form.getRecurrenceType() == RecurrenceType.ONCE ? form.getStartDate()
                : form.getEndDate();

        taskDefinitionService.createTaskDefinition(form.getTitle(), form.getDescription(),
                form.getBasePoints(), form.getRecurrenceType(), assignedUsers,
                currentUser, form.getStartDate(), effectiveEndDate, form.getWeeklyDays());

        return "redirect:/parent/tasks";
    }

    @GetMapping("/tasks/{id}/edit")
    public String editTaskForm(@PathVariable Long id, Model model) {
        TaskDefinition taskDefinition = taskDefinitionService.findById(id);
        List<User> children = userService.findAllByRole(UserRole.CHILD);
        model.addAttribute("task", taskDefinition);
        model.addAttribute("children", children);
        model.addAttribute("recurrenceTypes", RecurrenceType.values());
        return "parent/task-edit";
    }

    @PostMapping("/tasks/{id}/edit")
    public String editTask(@PathVariable Long id, @ModelAttribute TaskDefinitionController form,
            Authentication authentication) {
        userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Set<User> assignedUsers = form.getUserIds() != null ? form.getUserIds().stream()
                .map(userId -> userService.findById(userId).orElse(null))
                .filter(user -> user != null)
                .collect(java.util.stream.Collectors.toSet()) : Set.of();
        taskDefinitionService.updateTaskDefinition(id, form.getTitle(), form.getDescription(),
                form.getBasePoints(), form.getRecurrenceType(), assignedUsers,
                form.getStartDate(), form.getEndDate(), form.getWeeklyDays());
        return "redirect:/parent/tasks";
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id) {
        taskDefinitionService.deleteTaskDefinition(id);
        return "redirect:/parent/tasks";
    }

    @PostMapping("/tasks/{id}/approve")
    public String approveTask(@PathVariable Long id,
            @RequestParam int awardedPoints,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        taskInstanceService.approveTask(id, currentUser, awardedPoints, notes);
        return "redirect:/parent/tasks/pending";
    }

    @PostMapping("/tasks/{id}/reject")
    public String rejectTask(@PathVariable Long id,
            @RequestParam String notes,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        taskInstanceService.rejectTask(id, currentUser, notes);
        return "redirect:/parent/tasks/pending";
    }

    @GetMapping("/redemptions/pending")
    public String pendingRedemptions(Model model) {
        List<RewardRedemption> redemptions = redemptionService.findPendingApprovals();
        model.addAttribute("redemptions", redemptions);
        return "parent/redemptions-pending";
    }

    @PostMapping("/redemptions/{id}/approve")
    public String approveRedemption(@PathVariable Long id,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        redemptionService.approveRedemption(id, currentUser, notes);
        return "redirect:/parent/redemptions/pending";
    }

    @PostMapping("/redemptions/{id}/reject")
    public String rejectRedemption(@PathVariable Long id,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        User currentUser = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        redemptionService.cancelRedemption(id, currentUser);
        return "redirect:/parent/redemptions/pending";
    }

    @PostMapping("/redemptions/{id}/deliver")
    public String deliverRedemption(@PathVariable Long id) {
        redemptionService.markAsDelivered(id);
        return "redirect:/parent/dashboard";
    }

    // Reward Management
    @GetMapping("/rewards")
    public String listRewards(Model model) {
        List<Reward> rewards = rewardService.findAll();
        model.addAttribute("rewards", rewards);
        return "parent/rewards";
    }

    @GetMapping("/rewards/create")
    public String createRewardForm() {
        return "parent/reward-create";
    }

    @PostMapping("/rewards/create")
    public String createReward(@RequestParam String title,
            @RequestParam String description,
            @RequestParam int pointsCost,
            @RequestParam(required = false) MultipartFile image) {
        // TODO: Handle image upload
        rewardService.createReward(title, description, pointsCost, null);
        return "redirect:/parent/rewards";
    }

    @GetMapping("/children/{id}")
    public String childDetails(@PathVariable Long id, @RequestParam(defaultValue = "ALL") String period, Model model) {
        User child = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Calculate start date based on period
        LocalDateTime startDate = calculateStartDate(period);

        // Get transactions with balance for the period
        List<PointTransactionWithBalance> transactionsWithBalance = pointService
                .getTransactionHistoryWithBalance(child);

        // Filter by date if not ALL
        if (!"ALL".equals(period)) {
            transactionsWithBalance = transactionsWithBalance.stream()
                    .filter(t -> t.getTransaction().getCreatedAt().isAfter(startDate))
                    .toList();
        }

        // Prepare chart data
        List<Map<String, Object>> chartData = new ArrayList<>();
        for (PointTransactionWithBalance t : transactionsWithBalance) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("date", t.getTransaction().getCreatedAt().toLocalDate().toString());
            dataPoint.put("balance", t.getBalance());
            chartData.add(dataPoint);
        }

        // Reverse to show chronologically in chart
        java.util.Collections.reverse(chartData);

        List<TaskInstance> tasks = taskInstanceService.findByUser(child);

        model.addAttribute("child", child);
        model.addAttribute("transactionsWithBalance", transactionsWithBalance);
        model.addAttribute("chartData", chartData);
        model.addAttribute("tasks", tasks);
        model.addAttribute("selectedPeriod", period);

        return "parent/child-details";
    }

    private LocalDateTime calculateStartDate(String period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period.toUpperCase()) {
            case "TODAY" -> now.toLocalDate().atStartOfDay();
            case "7DAYS" -> now.minusDays(7);
            case "30DAYS" -> now.minusDays(30);
            case "180DAYS" -> now.minusDays(180);
            case "365DAYS" -> now.minusDays(365);
            default -> LocalDateTime.MIN; // ALL
        };
    }

    @GetMapping("/behaviors")
    public String listBehaviors(Model model) {
        List<User> children = userService.findAllByRole(UserRole.CHILD);
        model.addAttribute("children", children);
        return "parent/behaviors";
    }

    @GetMapping("/behaviors/evaluate/{childId}")
    public String evaluateBehaviors(@PathVariable Long childId, Model model) {
        User child = userService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Behavior> behaviors = behaviorEvaluationService.getActiveBehaviorsForUser(child);
        List<BehaviorEvaluation> evaluations = behaviorEvaluationService.getCurrentWeekEvaluations(child);
        int weeklyTotal = behaviorEvaluationService.calculateWeeklyTotal(child);

        // Create a map for easier access in Thymeleaf
        java.util.Map<Long, BehaviorEvaluation> evaluationMap = new java.util.HashMap<>();
        for (BehaviorEvaluation eval : evaluations) {
            evaluationMap.put(eval.getBehavior().getId(), eval);
        }

        model.addAttribute("child", child);
        model.addAttribute("behaviors", behaviors);
        model.addAttribute("evaluations", evaluations);
        model.addAttribute("evaluationMap", evaluationMap);
        model.addAttribute("weeklyTotal", weeklyTotal);

        return "parent/behaviors-evaluate";
    }

    @PostMapping("/behaviors/evaluate/update")
    @ResponseBody
    public BehaviorEvaluation updateBehaviorEvaluation(
            @RequestParam Long behaviorId,
            @RequestParam Long childId,
            @RequestParam int currentPoints,
            @RequestParam(required = false) String remarks,
            Authentication authentication) {

        User child = userService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User currentUser = (User) authentication.getPrincipal();

        return behaviorEvaluationService.updateEvaluation(
                behaviorId, child, currentPoints, remarks, currentUser);
    }

    @GetMapping("/behaviors/evaluate/weekly-total")
    @ResponseBody
    public Map<String, Integer> getWeeklyTotal(@RequestParam Long childId) {
        User child = userService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        int weeklyTotal = behaviorEvaluationService.calculateWeeklyTotal(child);

        Map<String, Integer> response = new HashMap<>();
        response.put("weeklyTotal", weeklyTotal);
        return response;
    }

    @PostMapping("/behaviors/evaluate/{childId}/commit")
    public String commitWeeklyEvaluations(@PathVariable Long childId,
            Authentication authentication) {
        User child = userService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User currentUser = (User) authentication.getPrincipal();
        behaviorEvaluationService.commitWeeklyEvaluations(child, currentUser);

        return "redirect:/parent/behaviors";
    }

    @GetMapping("/behaviors/manage")
    public String manageBehaviors(Model model) {
        List<Behavior> behaviors = behaviorService.findAllActive();
        List<User> children = userService.findAllByRole(UserRole.CHILD);
        model.addAttribute("behaviors", behaviors);
        model.addAttribute("children", children);
        return "parent/behaviors-manage";
    }

    @GetMapping("/behaviors/create")
    public String createBehaviorForm(Model model) {
        List<User> children = userService.findAllByRole(UserRole.CHILD);
        model.addAttribute("children", children);
        return "parent/behavior-create";
    }

    @PostMapping("/behaviors/create")
    public String createBehavior(@RequestParam String title,
            @RequestParam String guideline,
            @RequestParam int points,
            @RequestParam(required = false) Long childId,
            Authentication authentication) {
        User child = childId != null ? userService.findById(childId).orElse(null) : null;
        User currentUser = (User) authentication.getPrincipal();

        Behavior behavior = behaviorService.createBehavior(title, guideline, points, child);

        // Create initial evaluations with maximum points for applicable children
        List<User> applicableChildren;
        if (child != null) {
            applicableChildren = List.of(child);
        } else {
            applicableChildren = userService.findAllByRole(UserRole.CHILD);
        }

        for (User applicableChild : applicableChildren) {
            behaviorEvaluationService.updateEvaluation(behavior.getId(), applicableChild, points, null, currentUser);
        }

        return "redirect:/parent/behaviors/manage";
    }

    @GetMapping("/behaviors/{id}/edit")
    public String editBehaviorForm(@PathVariable Long id, Model model) {
        Behavior behavior = behaviorService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Behavior not found"));
        List<User> children = userService.findAllByRole(UserRole.CHILD);
        model.addAttribute("behavior", behavior);
        model.addAttribute("children", children);
        return "parent/behavior-edit";
    }

    @PostMapping("/behaviors/{id}/edit")
    public String editBehavior(@PathVariable Long id,
            @RequestParam String title,
            @RequestParam String guideline,
            @RequestParam int points,
            @RequestParam(required = false) Long childId) {
        User child = childId != null ? userService.findById(childId).orElse(null) : null;

        behaviorService.editBehavior(id, title, guideline, points, child);
        return "redirect:/parent/behaviors/manage";
    }

    @PostMapping("/behaviors/{id}/delete")
    public String deleteBehavior(@PathVariable Long id) {
        behaviorService.deleteBehavior(id);
        return "redirect:/parent/behaviors/manage";
    }

    @PostMapping("/behaviors/{behaviorId}/record")
    public String recordBehavior(@PathVariable Long behaviorId,
            @RequestParam Long childId,
            Authentication authentication) {
        User child = userService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User currentUser = (User) authentication.getPrincipal();
        behaviorService.recordBehavior(behaviorId, child, currentUser);

        return "redirect:/parent/behaviors";
    }

    @GetMapping("/points")
    public String pointsManagement(Model model) {
        List<User> children = userService.findAllByRole(UserRole.CHILD);
        model.addAttribute("children", children);
        return "parent/points";
    }

    @GetMapping("/points/manage/{childId}")
    public String managePoints(@PathVariable Long childId, Model model) {
        User child = userService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("child", child);
        return "parent/points-manage";
    }

    @PostMapping("/points/manage/{childId}")
    public String assignPoints(@PathVariable Long childId,
            @RequestParam String reason,
            @RequestParam String type,
            @RequestParam int points,
            Authentication authentication) {
        User child = userService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User currentUser = (User) authentication.getPrincipal();

        if ("bonus".equals(type)) {
            pointService.addPoints(child, points, PointTransactionType.BONUS, reason, null, currentUser);
        } else if ("penalty".equals(type)) {
            pointService.deductPoints(child, points, PointTransactionType.PENALTY, reason, null, currentUser);
        }

        return "redirect:/parent/points/manage/" + childId + "?success=true";
    }

    @PostMapping("/behaviors/update-ranks")
    @ResponseBody
    public ResponseEntity<String> updateBehaviorRanks(@RequestBody List<Long> behaviorIds) {
        try {
            behaviorService.updateBehaviorRanks(behaviorIds);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}
