package com.family.kidschores.controller;

import com.family.kidschores.model.*;
import com.family.kidschores.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/parent")
@RequiredArgsConstructor
public class ParentController {

    private final ChildService childService;
    private final TaskService taskService;
    private final RewardRedemptionService redemptionService;
    private final BehaviorService behaviorService;
    private final PenaltyService penaltyService;

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
        List<Behavior> behaviors = behaviorService.findAllActive();
        List<Child> children = childService.findAll();
        model.addAttribute("behaviors", behaviors);
        model.addAttribute("children", children);
        return "parent/behaviors";
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
