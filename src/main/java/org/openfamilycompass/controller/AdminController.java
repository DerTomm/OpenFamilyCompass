package org.openfamilycompass.controller;

import org.openfamilycompass.model.*;
import org.openfamilycompass.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ChildService childService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<User> users = userService.findAllActive();
        model.addAttribute("users", users);
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
}
