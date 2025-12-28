package org.openfamilycompass.controller;

import java.util.List;

import org.openfamilycompass.model.User;
import org.openfamilycompass.model.UserRole;
import org.openfamilycompass.service.ChildService;
import org.openfamilycompass.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;

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
            @RequestParam String password,
            @RequestParam UserRole role,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String avatarPath) {
        User user = userService.createUser(username, password, role);

        if (role == UserRole.CHILD && firstName != null) {
            childService.createChild(user, firstName, avatarPath);
        }

        return "redirect:/admin/users";
    }
}
