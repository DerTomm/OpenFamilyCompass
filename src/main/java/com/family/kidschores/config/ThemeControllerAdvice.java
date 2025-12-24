package com.family.kidschores.config;

import com.family.kidschores.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ThemeControllerAdvice {

    @ModelAttribute
    public void addThemeToModel(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            String theme = user.getTheme();
            
            if (theme == null || theme.isEmpty()) {
                theme = "LIGHT";
            }
            
            model.addAttribute("userTheme", theme.toLowerCase());
        } else {
            model.addAttribute("userTheme", "light");
        }
    }
}
