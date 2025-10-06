package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class AuthViewController {

    @Autowired
    private UserService userService;

    /**
     * Login page
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Registration page
     */
    @GetMapping("/register")
    public String register() {
        return "register";
    }

    /**
     * Forgot password page
     */
    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }
    
    /**
     * Providing an automatic login function without password for convenience of testing
     * @param username username
     * @param isAdmin whether is admin or not
     * @return redirect to the dashboard
     */
    @GetMapping("/autoLogin")
    public String autoLogin(@RequestParam(defaultValue = "admin") String username, 
                           @RequestParam(defaultValue = "false") boolean isAdmin,
                           RedirectAttributes redirectAttributes) {
        try {
            // try to search for existing users
            Optional<User> userOpt = userService.findByUsername(username);
            User user;
            
            if (userOpt.isPresent()) {
                user = userOpt.get();
            } else {
                // if user is not exist, create a virtual user
                user = new User();
                user.setUsername(username);
                user.setEmail(username + "@example.com");
                
                if (isAdmin || "admin".equals(username)) {
                    user.setRole(User.Role.Admin);
                } else {
                    user.setRole(User.Role.User);
                }
            }
            
            // 创建权限列表
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            
            if (user.getRole() == User.Role.Admin) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
            
            // 创建认证对象并设置到安全上下文
            Authentication auth = 
                new UsernamePasswordAuthenticationToken(username, "", authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            // 添加成功消息
            redirectAttributes.addFlashAttribute("successMessage", "已自动登录为用户: " + username);
            
            return "redirect:/user/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "自动登录失败: " + e.getMessage());
            return "redirect:/login";
        }
    }

    /**
     * 处理登录后直接重定向到仪表盘
     */
    @PostMapping("/dashboard")
    public String redirectToDashboard() {
        return "redirect:/user/dashboard";
    }

    /**
     * 处理登录后直接重定向到管理面板
     */
    @PostMapping("/admin")
    public String redirectToAdmin() {
        return "redirect:/admin/";
    }
} 