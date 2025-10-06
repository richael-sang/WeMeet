package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class HomeController {

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home() {
        // Check if user is logged in and redirect based on role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            Optional<User> userOpt = userService.findByUsername(auth.getName());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (user.getRole() == User.Role.Admin) {
                    return "redirect:/admin";
                } else {
                    return "redirect:/user/portal";
                }
            }
        }
        
        // Default to login page if not authenticated
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }
        
        Optional<User> userOpt = userService.findByUsername(auth.getName());
        if (!userOpt.isPresent()) {
            return "redirect:/login";
        }
        
        User user = userOpt.get();
        
        // 根据用户角色返回不同的页面
        if (user.getRole() == User.Role.Admin) {
            return "redirect:/admin";
        } else if (user.getRole() == User.Role.User) {
            return "redirect:/user/dashboard";
        } else {
            return "redirect:/login"; // 默认重定向到登录页面
        }
    }
    
    // 原login方法已移至AuthViewController
    // 原autoLogin方法已移至AuthViewController
} 