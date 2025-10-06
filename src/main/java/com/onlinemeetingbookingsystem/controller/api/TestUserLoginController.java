package com.onlinemeetingbookingsystem.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.onlinemeetingbookingsystem.entity.User;

@RestController
@RequestMapping("/api/test-user")
public class TestUserLoginController {

    /**
     * 测试用户登录接口 - 用于前端用户切换功能
     */
    @PostMapping("/login/{userId}")
    public ResponseEntity<?> loginTestUser(@PathVariable String userId, HttpServletRequest request) {
        try {
            // 创建虚拟测试用户
            User testUser = new User();
            testUser.setId(Long.parseLong(userId));
            testUser.setUsername("测试用户 " + userId);
            testUser.setEmail("test" + userId + "@example.com");
            testUser.setRole(User.Role.User);
            
            // 创建权限列表
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            
            // 创建认证对象并设置到安全上下文
            WebAuthenticationDetails details = new WebAuthenticationDetails(request);
            UsernamePasswordAuthenticationToken auth = 
                new UsernamePasswordAuthenticationToken(testUser.getUsername(), "", authorities);
            auth.setDetails(details);
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            return ResponseEntity.ok(Map.of(
                "success", true, 
                "message", "已切换到测试用户 " + userId,
                "user", Map.of(
                    "id", testUser.getId(),
                    "username", testUser.getUsername()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "切换测试用户失败: " + e.getMessage()
            ));
        }
    }
} 