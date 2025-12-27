package com.family.kidschores.controller;

import java.util.List;

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

import com.family.kidschores.model.Behavior;
import com.family.kidschores.model.BehaviorEvaluation;
import com.family.kidschores.model.Child;
import com.family.kidschores.model.RecurrenceType;
import com.family.kidschores.model.Reward;
import com.family.kidschores.model.RewardRedemption;
import com.family.kidschores.model.Task;
import com.family.kidschores.model.User;
import com.family.kidschores.service.BehaviorEvaluationService;
import com.family.kidschores.service.BehaviorService;
import com.family.kidschores.service.ChildService;
import com.family.kidschores.service.PenaltyService;
import com.family.kidschores.service.RewardRedemptionService;
import com.family.kidschores.service.RewardService;
import com.family.kidschores.service.TaskService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/parent")
@RequiredArgsConstructor
public class ParentController {

    private final ChildService childService;
    private final TaskService taskService;
    private final RewardRedemptionService redemptionService;
    private final BehaviorService behaviorService;
    private final BehaviorEvaluationService behaviorEvaluationService;
    private final PenaltyService penaltyService;
    private final RewardService rewardService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Child> children = childService.findAll();
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
        List<Child> children = childService.findAll();
        model.addAttribute("children", children);
        model.addAttribute("recurrenceTypes", RecurrenceType.values());
        return "parent/task-create";
    }

    @PostMapping("/tasks/create")
    public String createTask(@ModelAttribute TaskForm form) {
        Child child = form.getChildId() != null ? childService.findById(form.getChildId()).orElse(null) : null;

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
        User currentUser = (User) authentication.getPrincipal();
        redemptionService.approveRedemption(id, currentUser, notes);
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
        Child child = childService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));

        List<Task> tasks = taskService.findByChild(child);
        List<Behavior> behaviors = behaviorService.findAllActive();

        model.addAttribute("child", child);
        model.addAttribute("tasks", tasks);
        model.addAttribute("behaviors", behaviors);

        return "parent/child-details";
    }

    @GetMapping("/behaviors")
    public String listBehaviors(Model model) {
        List<Child> children = childService.findAll();
        model.addAttribute("children", children);
        return "parent/behaviors";
    }

    @GetMapping("/behaviors/evaluate/{childId}")
    public String evaluateBehaviors(@PathVariable Long childId, Model model) {
        Child child = childService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));

        List<Behavior> behaviors = behaviorEvaluationService.getActiveBehaviorsForChild(child);
        List<BehaviorEvaluation> evaluations = behaviorEvaluationService.getCurrentWeekEvaluations(child);
        int weeklyTotal = behaviorEvaluationService.calculateWeeklyTotal(child);

        // Erstelle eine Map für einfacheren Zugriff in Thymeleaf
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

        Child child = childService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));

        User currentUser = (User) authentication.getPrincipal();

        return behaviorEvaluationService.updateEvaluation(
                behaviorId, child, currentPoints, remarks, currentUser);
    }

    @PostMapping("/behaviors/evaluate/{childId}/commit")
    public String commitWeeklyEvaluations(@PathVariable Long childId,
            Authentication authentication) {
        Child child = childService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));

        User currentUser = (User) authentication.getPrincipal();
        behaviorEvaluationService.commitWeeklyEvaluations(child, currentUser);

        return "redirect:/parent/behaviors";
    }

    @GetMapping("/behaviors/manage")
    public String manageBehaviors(Model model) {
        List<Behavior> behaviors = behaviorService.findAllActive();
        List<Child> children = childService.findAll();
        model.addAttribute("behaviors", behaviors);
        model.addAttribute("children", children);
        return "parent/behaviors-manage";
    }

    @GetMapping("/behaviors/create")
    public String createBehaviorForm(Model model) {
        List<Child> children = childService.findAll();
        model.addAttribute("children", children);
        return "parent/behavior-create";
    }

    @PostMapping("/behaviors/create")
    public String createBehavior(@RequestParam String title,
            @RequestParam String guideline,
            @RequestParam int points,
            @RequestParam(required = false) Long childId) {
        Child child = childId != null ? childService.findById(childId).orElse(null) : null;

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
        Child child = childService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));

        User currentUser = (User) authentication.getPrincipal();
        behaviorService.recordBehavior(behaviorId, child, currentUser);

        return "redirect:/parent/behaviors";
    }

    @GetMapping("/points")
    public String pointsManagement(Model model) {
        List<Child> children = childService.findAll();
        model.addAttribute("children", children);
        return "parent/points";
    }

    @PostMapping("/points/bonus")
    public String addBonusPoints(@RequestParam Long childId,
            @RequestParam String reason,
            @RequestParam int points,
            Authentication authentication) {
        Child child = childService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));

        User currentUser = (User) authentication.getPrincipal();
        penaltyService.addBonusPoints(child, reason, points, currentUser);

        return "redirect:/parent/dashboard";
    }

    @PostMapping("/points/penalty")
    public String addPenaltyPoints(@RequestParam Long childId,
            @RequestParam String reason,
            @RequestParam int points,
            Authentication authentication) {
        Child child = childService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));

        User currentUser = (User) authentication.getPrincipal();
        penaltyService.createPenalty(child, reason, points, currentUser);

        return "redirect:/parent/dashboard";
    }

    @PostMapping("/penalties/create")
    public String createPenalty(@RequestParam Long childId,
            @RequestParam String reason,
            @RequestParam int points,
            Authentication authentication) {
        Child child = childService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));

        User currentUser = (User) authentication.getPrincipal();
        penaltyService.createPenalty(child, reason, points, currentUser);

        return "redirect:/parent/children/" + childId;
    }
}
