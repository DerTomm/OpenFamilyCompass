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
@RequestMapping("/child")
@RequiredArgsConstructor
public class ChildController {

    private final ChildService childService;
    private final TaskService taskService;
    private final RewardService rewardService;
    private final RewardRedemptionService redemptionService;
    private final PointService pointService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User currentUser = (User) authentication.getPrincipal();
        Child child = childService.findByUser(currentUser)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));

        List<Task> pendingTasks = taskService.findPendingForChild(child);
        List<Task> completedTasks = taskService.findByChildAndStatus(child, TaskStatus.CHILD_COMPLETED);
        
        model.addAttribute("child", child);
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("totalPoints", child.getTotalPoints());
        
        return "child/dashboard";
    }

    @GetMapping("/tasks")
    public String tasks(Authentication authentication, Model model) {
        User currentUser = (User) authentication.getPrincipal();
        Child child = childService.findByUser(currentUser)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));

        List<Task> tasks = taskService.findPendingForChild(child);
        
        model.addAttribute("child", child);
        model.addAttribute("tasks", tasks);
        
        return "child/tasks";
    }

    @PostMapping("/tasks/{id}/complete")
    public String completeTask(@PathVariable Long id, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        taskService.markAsCompleted(id, currentUser);
        return "redirect:/child/tasks";
    }

    @GetMapping("/shop")
    public String shop(Authentication authentication, Model model) {
        User currentUser = (User) authentication.getPrincipal();
        Child child = childService.findByUser(currentUser)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));

        List<Reward> rewards = rewardService.findAllActive();
        List<RewardRedemption> myRedemptions = redemptionService.findByChild(child);
        
        model.addAttribute("child", child);
        model.addAttribute("rewards", rewards);
        model.addAttribute("myRedemptions", myRedemptions);
        model.addAttribute("totalPoints", child.getTotalPoints());
        
        return "child/shop";
    }

    @PostMapping("/shop/redeem/{rewardId}")
    public String redeemReward(@PathVariable Long rewardId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        Child child = childService.findByUser(currentUser)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));

        Reward reward = rewardService.findById(rewardId)
                .orElseThrow(() -> new IllegalArgumentException("Reward not found"));

        try {
            redemptionService.requestReward(child, reward);
        } catch (IllegalStateException e) {
            // Nicht genug Punkte
            return "redirect:/child/shop?error=notenough";
        }
        
        return "redirect:/child/shop?success=true";
    }

    @GetMapping("/history")
    public String history(Authentication authentication, Model model) {
        User currentUser = (User) authentication.getPrincipal();
        Child child = childService.findByUser(currentUser)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));

        List<PointTransaction> transactions = pointService.getTransactionHistory(child);
        
        model.addAttribute("child", child);
        model.addAttribute("transactions", transactions);
        
        return "child/history";
    }
}
