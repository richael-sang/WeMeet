package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.entity.Notification;
import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.service.NotificationService;
import com.onlinemeetingbookingsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * User Notification Controller
 */
@Controller
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Check the user's notifications list
     */
    @GetMapping("/user/notifications")
    public String viewNotifications(Principal principal, Model model) {
        // 使用当前登录用户名获取用户ID
        Optional<User> userOpt = userService.findByUsername(principal.getName());
        if (!userOpt.isPresent()) {
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        List<Notification> notifications = notificationService.getNotificationsByUserId(user.getId());
        model.addAttribute("notifications", notifications);
        
        return "user/notifications";
    }
    
    /**
     * Mark the notification as read
     */
    @PostMapping("/user/notifications/{id}/mark-read")
    public String markNotificationAsRead(@PathVariable Long id, 
                                       Principal principal, 
                                       RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAsRead(id);
            redirectAttributes.addFlashAttribute("success", "Notification marked as read");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to mark notification as read: " + e.getMessage());
        }
        
        return "redirect:/user/notifications";
    }
}