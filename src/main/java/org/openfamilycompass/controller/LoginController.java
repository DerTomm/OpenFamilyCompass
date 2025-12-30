package org.openfamilycompass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(value = "refreshToken", required = false) String refreshToken) {
        // If no refresh token available, show login page
        if (refreshToken == null || refreshToken.isEmpty()) {
            return "login";
        }

        // For OIDC, we don't handle refresh tokens here anymore
        // The Authorization Server handles authentication
        return "login";
    }
}