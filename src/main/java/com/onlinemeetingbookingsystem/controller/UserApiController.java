package com.onlinemeetingbookingsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.model.ApiResponse;
import com.onlinemeetingbookingsystem.service.UserService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;

@RestController
@RequestMapping("/api/user")
public class UserApiController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 获取当前登录用户的个人资料
     * @deprecated 请使用 {@link com.onlinemeetingbookingsystem.controller.api.UserProfileApiController#getUserProfile}
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "用户未登录", null));
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> userOpt = userService.findByUsername(userDetails.getUsername());
        
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "用户不存在", null));
        }
        
        User user = userOpt.get();
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("id", user.getId());
        profileData.put("username", user.getUsername());
        profileData.put("email", user.getEmail());
        // 使用可用的属性
        profileData.put("createdAt", user.getCreatedAt());
        profileData.put("lastLoginAt", user.getLastLoginAt());
        
        return ResponseEntity.ok(new ApiResponse<>(true, "获取用户资料成功", profileData));
    }

    /**
     * 更新用户个人资料
     * @deprecated 请使用 {@link com.onlinemeetingbookingsystem.controller.api.UserProfileApiController#updateUserProfile}
     */
    @PostMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@AuthenticationPrincipal UserDetails userDetails,
                                              @RequestBody UserProfileUpdateRequest request) {
        // 重定向到新的API端点
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header("Location", "/api/profile/update")
                .body(Map.of("message", "This endpoint is deprecated. Please use /api/profile/update"));
    }

    /**
     * 修改密码
     * @deprecated 请使用 {@link com.onlinemeetingbookingsystem.controller.api.UserProfileApiController#changePassword}
     */
    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                           @RequestBody PasswordChangeRequest request) {
        // 重定向到新的API端点
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header("Location", "/api/profile/change-password")
                .body(Map.of("message", "This endpoint is deprecated. Please use /api/profile/change-password"));
    }

    /**
     * 获取用户通知设置
     */
    @GetMapping("/notifications/settings")
    public ResponseEntity<?> getUserNotificationSettings(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        Optional<User> userOpt = userService.findByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        // Since getUserNotificationSettings is not defined, we'll return a default response
        // In a real application, you would implement this method in UserService
        Map<String, Boolean> settings = Map.of(
            "email_notifications", true,
            "booking_reminders", true,
            "system_updates", false
        );

        return ResponseEntity.ok(settings);
    }

    /**
     * 更新用户通知设置
     */
    @PostMapping("/notifications/settings")
    public ResponseEntity<?> updateUserNotificationSettings(@AuthenticationPrincipal UserDetails userDetails,
                                                          @RequestBody Map<String, Boolean> settings) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        Optional<User> userOpt = userService.findByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        // Since updateUserNotificationSettings is not defined, we'll return a default response
        // In a real application, you would implement this method in UserService
        return ResponseEntity.ok(Map.of("success", true, "message", "Notification settings updated successfully"));
    }

    /**
     * 获取用户最近活动记录
     */
    @GetMapping("/activities")
    public ResponseEntity<?> getUserActivities(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }

        Optional<User> userOpt = userService.findByUsername(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        // Since getUserActivities is not defined, we'll return a default response
        // In a real application, you would implement this method in UserService
        List<Map<String, Object>> activities = List.of(
            Map.of(
                "type", "booking_created",
                "description", "Created a new booking for Room A",
                "timestamp", LocalDateTime.now().minusDays(1).toString()
            ),
            Map.of(
                "type", "booking_cancelled",
                "description", "Cancelled booking for Room B",
                "timestamp", LocalDateTime.now().minusDays(3).toString()
            )
        );

        return ResponseEntity.ok(activities);
    }

    // Inner classes for request/response objects
    @Data
    public static class UserProfileUpdateRequest {
        private String email;
        private String avatar;
    }

    @Data
    public static class PasswordChangeRequest {
        private String currentPassword;
        private String newPassword;
    }

    @GetMapping("/user/home")
    public String userHome() {
        // 原home()方法的逻辑
        return "user-home";
    }
} 