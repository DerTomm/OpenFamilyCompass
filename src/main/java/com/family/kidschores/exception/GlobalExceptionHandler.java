package com.family.kidschores.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(IllegalArgumentException ex, RedirectAttributes redirectAttributes) {
        log.error("IllegalArgumentException: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/dashboard";
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalStateException(IllegalStateException ex, RedirectAttributes redirectAttributes) {
        log.error("IllegalStateException: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/dashboard";
    }

    @ExceptionHandler(NullPointerException.class)
    public String handleNullPointerException(NullPointerException ex, RedirectAttributes redirectAttributes) {
        log.error("NullPointerException: {}", ex.getMessage(), ex);
        redirectAttributes.addFlashAttribute("error", "Ein unerwarteter Fehler ist aufgetreten. Bitte versuchen Sie es erneut.");
        return "redirect:/dashboard";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model, RedirectAttributes redirectAttributes) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        redirectAttributes.addFlashAttribute("error", "Ein unerwarteter Fehler ist aufgetreten. Bitte wenden Sie sich an den Administrator.");
        return "redirect:/dashboard";
    }
}
