package org.openfamilycompass.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            // Redirect to role-based dashboard
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                switch (role) {
                    case "ROLE_ADMIN":
                        return "redirect:/admin/dashboard";
                    case "ROLE_PARENT":
                        return "redirect:/parent/dashboard";
                    case "ROLE_CHILD":
                        return "redirect:/child/dashboard";
                }
            }
            // Default fallback
            return "redirect:/profile/settings";
        }
        // Not authenticated, but SecurityConfig will redirect to /login
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        // Same logic as home
        return home(authentication);
    }
}