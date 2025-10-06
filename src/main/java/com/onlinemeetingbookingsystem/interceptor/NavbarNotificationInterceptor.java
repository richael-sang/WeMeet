package com.onlinemeetingbookingsystem.interceptor;

import com.onlinemeetingbookingsystem.entity.User; // Corrected package to entity
import com.onlinemeetingbookingsystem.service.NotificationService;
import com.onlinemeetingbookingsystem.service.UserService; // Assuming UserService exists
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class NavbarNotificationInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(NavbarNotificationInterceptor.class);

    @Autowired
    private NotificationService notificationService;

    @Lazy
    @Autowired
    private UserService userService; // Inject UserService

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler,
                           ModelAndView modelAndView) throws Exception {

        // Ensure ModelAndView exists, it's a view request, and not a redirect
        if (modelAndView != null && modelAndView.hasView() && !modelAndView.getViewName().startsWith("redirect:")) {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Check if user is authenticated and not anonymous
            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                String username = authentication.getName();
                try {
                    // Find user by username via UserService
                    User user = userService.findByUsername(username)
                            .orElse(null); // Adjust if findByUsername returns User directly or different Optional

                    if (user != null) {
                        Long userId = user.getId();
                        // Check for unread notifications using the service method
                        boolean hasUnread = notificationService.hasUnreadNotifications(userId);
                        // Add the flag to the model
                        modelAndView.addObject("hasUnreadNotifications", hasUnread);
                        logger.trace("Added hasUnreadNotifications={} for user ID: {}", hasUnread, userId);
                    } else {
                         // This case might happen if the user exists in security context but not DB (unlikely but possible)
                        logger.warn("User found in SecurityContext but not found in DB via UserService: {}", username);
                        modelAndView.addObject("hasUnreadNotifications", false); // Default to false if user lookup fails
                    }

                } catch (Exception e) {
                    // Log exceptions during user lookup or notification check
                    logger.error("Error adding notification status to model for user {}: {}", username, e.getMessage(), e);
                    // Optionally add a default value to prevent errors in template
                    modelAndView.addObject("hasUnreadNotifications", false);
                }
            } else {
                 // Handle cases like anonymous users or non-UserDetails principals if necessary
                 // For anonymous or non-standard auth, default to no unread notifications
                 if (modelAndView.getModel().get("hasUnreadNotifications") == null) {
                      modelAndView.addObject("hasUnreadNotifications", false);
                 }
            }
        }
    }
} 