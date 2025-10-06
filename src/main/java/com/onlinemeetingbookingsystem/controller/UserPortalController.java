package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.entity.MeetingRoom;
import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.service.BookingService;
import com.onlinemeetingbookingsystem.service.MeetingRoomService;
import com.onlinemeetingbookingsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/user/portal")
public class UserPortalController {

    @Autowired
    private MeetingRoomService meetingRoomService;
    
    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserService userService;

    @GetMapping
    public String userPortal(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Try to get user info if authenticated and not anonymous
        Optional<User> userOpt = Optional.ofNullable(auth)
                                       .filter(Authentication::isAuthenticated)
                                       .map(Authentication::getName)
                                       .filter(name -> !"anonymousUser".equals(name))
                                       .flatMap(userService::findByUsername);

        // Load common data regardless of login state
        List<MeetingRoom> rooms = meetingRoomService.findAll();
        model.addAttribute("rooms", rooms);
        System.out.println("加载会议室数量: " + rooms.size());

        if (userOpt.isPresent()) {
            User currentUser = userOpt.get();
             // Only add user object if the user is actually found and authenticated
            if (currentUser.getRole() == User.Role.User) { 
                 model.addAttribute("user", currentUser);
                 System.out.println("用户 " + currentUser.getUsername() + " 访问门户，已添加用户信息到 Model");
                 // You might load user-specific bookings here if needed
                 // List<Booking> userBookings = bookingService.getBookingsByUserId(currentUser.getId());
                 // model.addAttribute("userBookings", userBookings);
            } else {
                 // Logged in user is not a regular USER (e.g., Admin trying to access user portal directly)
                 // Decide if they should be redirected or shown a specific message/view
                 System.out.println("用户 " + currentUser.getUsername() + " 角色为 "+ currentUser.getRole() +" 尝试访问用户门户");
                 // Option 1: Redirect admin back to admin dashboard (or login if preferred)
                  return "redirect:/admin"; // Or "redirect:/login";
                 // Option 2: Show the portal but maybe without certain features (if applicable)
                 // model.addAttribute("user", currentUser); // Could still add user for header display?
            }
        } else {
            // User is not authenticated or anonymous
            System.out.println("匿名用户访问门户");
            // No user object added to the model
        }

        return "user/portal"; // Always return the template view
    }
    
    /**
     * POST mapping for login redirection (Handles the POST from login.js before redirecting to GET)
     * This might not be strictly necessary anymore if login.js uses GET directly, but kept for safety.
     */
    @PostMapping
    public String userPortalPost() {
        System.out.println("收到 POST 请求 /user/portal, 重定向到 GET /user/portal");
        return "redirect:/user/portal";
    }
    
    /**
     * 测试用户切换功能 - 仅用于模拟不同用户
     */
    @PostMapping("/switch-user")
    public ResponseEntity<Map<String, Object>> switchUser(@RequestParam String userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Optional<User> userOpt = Optional.ofNullable(auth)
                                           .filter(Authentication::isAuthenticated)
                                           .map(Authentication::getName)
                                           .filter(name -> !"anonymousUser".equals(name))
                                           .flatMap(userService::findByUsername);
            
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "当前用户认证信息不存在");
                return ResponseEntity.status(401).body(response); // Use 401 for not authenticated
            }
            
            // --- Test user switching logic (remains complex, consider simplifying if not needed) ---
            if (userId.startsWith("test-user")) {
                String testUserNumber = userId.split("-")[2];
                User testUser = new User();
                testUser.setId(Long.parseLong(testUserNumber));
                testUser.setUsername("测试用户" + testUserNumber);
                testUser.setEmail("test" + testUserNumber + "@example.com");
                testUser.setRole(User.Role.User); 
                response.put("success", true);
                response.put("userId", testUser.getId());
                response.put("username", testUser.getUsername());
                return ResponseEntity.ok(response);
            }
            // --- End test user switching logic ---
            
            Long id = Long.parseLong(userId);
            Optional<User> targetUserOpt = userService.findById(id);
            
            if (!targetUserOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "目标用户不存在");
                return ResponseEntity.status(404).body(response);
            }
            
            User targetUser = targetUserOpt.get();
            response.put("success", true);
            response.put("userId", targetUser.getId());
            response.put("username", targetUser.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
             response.put("success", false);
             response.put("message", "无效的用户 ID 格式");
             return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "切换用户时发生错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}