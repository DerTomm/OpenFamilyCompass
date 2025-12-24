package com.family.kidschores.controller;

import com.family.kidschores.model.*;
import com.family.kidschores.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ChildService childService;
    private final TaskService taskService;
    private final RewardService rewardService;
    private final BehaviorService behaviorService;
    private final PenaltyService penaltyService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Child> children = childService.findAll();
        List<Task> pendingApprovals = taskService.findPendingApproval();
        
        model.addAttribute("children", children);
        model.addAttribute("pendingApprovals", pendingApprovals);
        
        return "admin/dashboard";
    }

    // User Management
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userService.findAllActive();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/users/create")
    public String createUserForm(Model model) {
        model.addAttribute("roles", UserRole.values());
        return "admin/user-create";
    }

    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
                           @RequestParam String pin,
                           @RequestParam UserRole role,
                           @RequestParam(required = false) String firstName,
                           @RequestParam(required = false) String avatarPath) {
        User user = userService.createUser(username, pin, role);
        
        if (role == UserRole.CHILD && firstName != null) {
            childService.createChild(user, firstName, avatarPath);
        }
        
        return "redirect:/admin/users";
    }

    // Task Management
    @GetMapping("/tasks")
    public String listTasks(Model model) {
        List<Task> templates = taskService.findTemplates();
        model.addAttribute("templates", templates);
        return "admin/tasks";
    }

    @GetMapping("/tasks/create")
    public String createTaskForm(Model model) {
        List<Child> children = childService.findAll();
        model.addAttribute("children", children);
        model.addAttribute("recurrenceTypes", RecurrenceType.values());
        return "admin/task-create";
    }

    @PostMapping("/tasks/create")
    public String createTask(@ModelAttribute TaskForm form) {
        Child child = form.getChildId() != null ? 
                     childService.findById(form.getChildId()).orElse(null) : null;
        
        taskService.createTask(form.getTitle(), form.getDescription(), 
                              form.getBasePoints(), child, 
                              form.getRecurrenceType(), form.getDueDate(), 
                              form.isTemplate());
        
        return "redirect:/admin/tasks";
    }

    // Reward Management
    @GetMapping("/rewards")
    public String listRewards(Model model) {
        List<Reward> rewards = rewardService.findAll();
        model.addAttribute("rewards", rewards);
        return "admin/rewards";
    }

    @GetMapping("/rewards/create")
    public String createRewardForm() {
        return "admin/reward-create";
    }

    @PostMapping("/rewards/create")
    public String createReward(@RequestParam String title,
                              @RequestParam String description,
                              @RequestParam int pointsCost,
                              @RequestParam(required = false) MultipartFile image) {
        // TODO: Handle image upload
        rewardService.createReward(title, description, pointsCost, null);
        return "redirect:/admin/rewards";
    }

    // Behavior Management
    @GetMapping("/behaviors")
    public String listBehaviors(Model model) {
        List<Behavior> behaviors = behaviorService.findAllActive();
        List<Child> children = childService.findAll();
        model.addAttribute("behaviors", behaviors);
        model.addAttribute("children", children);
        return "admin/behaviors";
    }

    @GetMapping("/behaviors/create")
    public String createBehaviorForm(Model model) {
        List<Child> children = childService.findAll();
        model.addAttribute("children", children);
        return "admin/behavior-create";
    }

    @PostMapping("/behaviors/create")
    public String createBehavior(@RequestParam String title,
                                @RequestParam String guideline,
                                @RequestParam int points,
                                @RequestParam(required = false) Long childId) {
        Child child = childId != null ? 
                     childService.findById(childId).orElse(null) : null;
        
        behaviorService.createBehavior(title, guideline, points, child);
        return "redirect:/admin/behaviors";
    }

    @PostMapping("/behaviors/{id}/deactivate")
    public String deactivateBehavior(@PathVariable Long id) {
        behaviorService.deactivateBehavior(id);
        return "redirect:/admin/behaviors";
    }

    // Penalty Management
    @PostMapping("/penalties/create")
    public String createPenalty(@RequestParam Long childId,
                               @RequestParam String reason,
                               @RequestParam int points,
                               Authentication authentication) {
        Child child = childService.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));
        
        User currentUser = (User) authentication.getPrincipal();
        penaltyService.createPenalty(child, reason, points, currentUser);
        
        return "redirect:/admin/children/" + childId;
    }

    @GetMapping("/children/{id}")
    public String childDetails(@PathVariable Long id, Model model) {
        Child child = childService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Child not found"));
        
        model.addAttribute("child", child);
        return "admin/child-details";
    }
}
