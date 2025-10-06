package com.onlinemeetingbookingsystem.config;

import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

/**
 * Controller Advice to add global attributes to the model, specifically for the navigation bars.
 */
@ControllerAdvice(annotations = Controller.class) // Apply to all @Controller annotated classes
public class GlobalControllerAdvice {

    @Autowired
    private UserService userService;

    /**
     * Adds common attributes needed across multiple views, like the current user's display name.
     * This method runs before any @RequestMapping handler method in any @Controller.
     *
     * @param model The Spring Model object.
     */
    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String username = auth.getName();
            Optional<User> userOpt = userService.findByUsername(username);

            userOpt.ifPresent(user -> {
                // Check the user's role and add the appropriate attribute
                if (user.getRole() == User.Role.Admin) {
                    // For admin users, add 'adminName'
                    model.addAttribute("adminName", user.getUsername()); // Or use getFullName() if available
                    // Log for debugging
                    // System.out.println("Added adminName to model: " + user.getUsername());
                } else if (user.getRole() == User.Role.User) {
                    // For regular users, add 'userName' (matching UserViewController logic)
                    // This might consolidate logic if UserViewController doesn't need to do it anymore.
                    model.addAttribute("userName", user.getUsername());
                    // Log for debugging
                    // System.out.println("Added userName to model: " + user.getUsername());
                }
                 // Add the full user object if needed globally (optional)
                 // model.addAttribute("currentUser", user); 
            });
        } else {
             // Log for debugging if needed
             // System.out.println("User not authenticated or is anonymous. No name added to model.");
        }
    }
} 