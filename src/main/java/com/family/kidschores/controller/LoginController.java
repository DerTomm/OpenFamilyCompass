package com.family.kidschores.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        // Redirect basierend auf Rolle
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            
            if (role.equals("ROLE_ADMIN")) {
                return "redirect:/admin/dashboard";
            } else if (role.equals("ROLE_PARENT")) {
                return "redirect:/parent/dashboard";
            } else if (role.equals("ROLE_CHILD")) {
                return "redirect:/child/dashboard";
            }
        }
        
        return "redirect:/login";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }
}
