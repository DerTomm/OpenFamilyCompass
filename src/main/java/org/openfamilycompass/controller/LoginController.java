package org.openfamilycompass.controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.openfamilycompass.model.User;
import org.openfamilycompass.security.JwtTokenService;
import org.openfamilycompass.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final JwtTokenService jwtTokenService;
    private final UserService userService;

    @GetMapping("/login")
    public String login(@RequestParam(value = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request) {
        // If no refresh token available, show login page
        if (refreshToken == null || refreshToken.isEmpty()) {
            return "login";
        }

        // Perform auto-login with refresh token
        try {
            // Validate it's a refresh token
            if (!jwtTokenService.isRefreshToken(refreshToken)) {
                return "redirect:/login?error";
            }

            // Extract username and load user
            String username = jwtTokenService.extractUsername(refreshToken);
            User user = (User) userService.loadUserByUsername(username);

            // Validate refresh token
            if (!jwtTokenService.validateToken(refreshToken, user)) {
                return "redirect:/login?error";
            }

            // Create authentication and set in SecurityContext
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null,
                    user.getAuthorities());

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            // Store SecurityContext in session
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            return "redirect:/dashboard";

        } catch (Exception e) {
            return "redirect:/login?error";
        }
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
