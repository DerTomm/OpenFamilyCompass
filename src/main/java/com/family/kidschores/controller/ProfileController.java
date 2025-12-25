package com.family.kidschores.controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.family.kidschores.model.User;
import com.family.kidschores.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping("/settings")
    public String settings(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("user", user);
        model.addAttribute("currentTheme", user.getTheme() != null ? user.getTheme() : "LIGHT");
        model.addAttribute("avatarType", user.getAvatarType() != null ? user.getAvatarType() : "DEFAULT");
        model.addAttribute("avatarIconName", user.getAvatarIconName());

        return "profile/settings";
    }

    @PostMapping("/settings/theme")
    public String updateTheme(
            @RequestParam String theme,
            Authentication authentication,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("LIGHT".equals(theme) || "DARK".equals(theme)) {
            user.setTheme(theme);
            userService.save(user);

            // Update the user in the security context immediately
            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                    user,
                    authentication.getCredentials(),
                    authentication.getAuthorities());
            newAuth.setDetails(authentication.getDetails());

            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(newAuth);

            // Update the session
            request.getSession().setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    securityContext);

            redirectAttributes.addFlashAttribute("success", "Design-Einstellungen erfolgreich gespeichert!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Ungültiges Theme ausgewählt!");
        }

        return "redirect:/profile/settings";
    }
}
