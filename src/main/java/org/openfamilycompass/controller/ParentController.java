package org.openfamilycompass.controller;

import java.util.List;

import org.openfamilycompass.model.Behavior;
import org.openfamilycompass.model.BehaviorEvaluation;
import org.openfamilycompass.model.PointTransactionType;
import org.openfamilycompass.model.RecurrenceType;
import org.openfamilycompass.model.Reward;
import org.openfamilycompass.model.RewardRedemption;
import org.openfamilycompass.model.Task;
import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.service.BehaviorEvaluationService;
import org.openfamilycompass.service.BehaviorService;
import org.openfamilycompass.service.PointService;
import org.openfamilycompass.service.RewardRedemptionService;
import org.openfamilycompass.service.RewardService;
import org.openfamilycompass.service.TaskService;
import org.openfamilycompass.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/parent")
@PreAuthorize("hasRole('PARENT')")
@RequiredArgsConstructor
public class ParentController {

    private final UserService userService;
    private final TaskService taskService;
    private final RewardRedemptionService redemptionService;
    private final BehaviorService behaviorService;
    private final BehaviorEvaluationService behaviorEvaluationService;
    private final RewardService rewardService;
    private final PointService pointService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<User> children = userService.findAllByRole(UserRole.CHILD);
        List<Task> pendingApprovals = taskService.findPendingApproval();
        List<RewardRedemption> pendingRedemptions = redemptionService.findPendingApprovals();

        model.addAttribute("children", children);
        model.addAttribute("pendingApprovals", pendingApprovals);
        model.addAttribute("pendingRedemptions", pendingRedemptions);

        return "parent/dashboard";
    }

    @GetMapping("/tasks/pending")
    public String pendingTasks(Model model) {
        List<Task> tasks = taskService.findPendingApproval();
        model.addAttribute("tasks", tasks);
        return "parent/tasks-pending";
    }

    // Task Management
    @GetMapping("/tasks")
    public String listTasks(Model model) {
        List<Task> templates = taskService.findTemplates();
        model.addAttribute("templates", templates);
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
    public String createTask(@ModelAttribute TaskForm form) {
        User child = form.getUserId() != null ? userService.findById(form.getUserId()).orElse(null) : null;

        taskService.createTask(form.getTitle(), form.getDescription(),
                form.getBasePoints(), child,
                form.getRecurrenceType(), form.getDueDate(),
                form.isTemplate());

        return "redirect:/parent/tasks";
    }

    @PostMapping("/tasks/{id}/approve")
    public String approveTask(@PathVariable Long id,
            @RequestParam int awardedPoints,
            @RequestParam(required = false) String notes,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        taskService.approveTask(id, currentUser, awardedPoints, notes);
        return "redirect:/parent/tasks/pending";
    }

    @PostMapping("/tasks/{id}/reject")
    public String rejectTask(@PathVariable Long id,
            @RequestParam String notes,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        taskService.rejectTask(id, currentUser, notes);
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
    public String childDetails(@PathVariable Long id, Model model) {
        User child = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Task> tasks = taskService.findByUser(child);
        List<Behavior> behaviors = behaviorService.findAllActive();

        model.addAttribute("child", child);
        model.addAttribute("tasks", tasks);
        model.addAttribute("behaviors", behaviors);

        return "parent/child-details";
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
            @RequestParam(required = false) Long childId) {
        User child = childId != null ? userService.findById(childId).orElse(null) : null;

        behaviorService.createBehavior(title, guideline, points, child);
        return "redirect:/parent/behaviors/manage";
    }

    @PostMapping("/behaviors/{id}/deactivate")
    public String deactivateBehavior(@PathVariable Long id) {
        behaviorService.deactivateBehavior(id);
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
}
