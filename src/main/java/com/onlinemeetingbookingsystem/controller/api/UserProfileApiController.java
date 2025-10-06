package com.onlinemeetingbookingsystem.controller.api;

import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.service.FileService;
import com.onlinemeetingbookingsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/profile")
public class UserProfileApiController {

    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp"
    };
    
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Autowired
    private UserService userService;
    
    @Autowired
    private FileService fileService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        Optional<User> userOpt = userService.findByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        User user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("avatar", user.getAvatar());
        response.put("createdAt", user.getCreatedAt());
        response.put("lastLoginAt", user.getLastLoginAt());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/update")
    public ResponseEntity<?> updateUserProfile(
            @RequestBody UserProfileRequest request,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        
        Optional<User> userOpt = userService.findByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
        
        User user = userOpt.get();
        
        boolean usernameChanged = false;
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty() 
            && !request.getUsername().equals(user.getUsername())) {
            Optional<User> existingUserWithSameUsername = userService.findByUsername(request.getUsername());
            if (existingUserWithSameUsername.isPresent() && !existingUserWithSameUsername.get().getId().equals(user.getId())) {
                 return ResponseEntity.badRequest().body(Map.of("error", "Username already taken"));
            }
            user.setUsername(request.getUsername());
            usernameChanged = true;
        }
        
        if (request.getAvatar() != null ) { 
             user.setAvatar(request.getAvatar().isEmpty() ? null : request.getAvatar()); 
        }
        
        try {
            userService.saveUser(user);
            
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "Profile updated successfully");

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("email", user.getEmail());
            userMap.put("avatar", user.getAvatar());

            responseBody.put("user", userMap);

            if (usernameChanged) {
                responseBody.put("usernameChanged", true); 
                responseBody.put("message", "Username updated successfully. Please log in again.");
            }

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
             System.err.println("Error updating user profile for user " + authentication.getName() + ": " + e.getMessage()); 
             e.printStackTrace();

             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body(Map.of("error", "An unexpected error occurred while updating the profile. Please check server logs or contact support."));
        }
    }
    
    @PostMapping(value = "/upload-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("code", "0", "message", "Not authenticated"));
        }
        
        Optional<User> userOpt = userService.findByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("code", "0", "message", "User not found"));
        }
        
        try {
            // Check if file is empty
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "Please select an image to upload"));
            }

            // Check file size
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "Image size cannot exceed 10MB"));
            }

            // Get file content type
            String contentType = file.getContentType();

            // Check if it's an allowed image type
            if (contentType == null || !Arrays.asList(ALLOWED_IMAGE_TYPES).contains(contentType.toLowerCase())) {
                return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "Only JPG, PNG, GIF, BMP, and WEBP formats are allowed"));
            }

            // Validate file extension
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !isValidImageExtension(originalFilename)) {
                return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "Invalid file format"));
            }

            // Further validate that the file is an image
            try {
                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image == null) {
                    return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "Invalid image file"));
                }
            } catch (IOException e) {
                return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "Invalid image file"));
            }

            // Upload file
            String fileUrl = fileService.uploadFile(file);
            
            // Update user's avatar
            User user = userOpt.get();
            
            // Store the previous avatar URL if it's not the default one
            String previousAvatar = user.getAvatar();
            if (previousAvatar != null && !previousAvatar.equals("/images/default-avatar.png") && previousAvatar.contains("://")) {
                // Delete the previous avatar asynchronously
                try {
                    fileService.deleteFile(previousAvatar);
                } catch (Exception e) {
                    // Log but continue, this isn't critical
                    System.err.println("Failed to delete previous avatar: " + e.getMessage());
                }
            }
            
            user.setAvatar(fileUrl);
            userService.saveUser(user);
            
            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);
            response.put("message", "Avatar uploaded successfully");
            response.put("code", "1");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "Failed to upload image: " + e.getMessage()));
        }
    }
    
    /**
     * Validate file extension
     */
    private boolean isValidImageExtension(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp").contains(extension);
    }
    
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody PasswordChangeRequest request,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        
        Optional<User> userOpt = userService.findByUsername(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        User user = userOpt.get();
        
        // Validate current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
        }
        
        // Validate new password
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password cannot be empty"));
        }
        
        // Password strength validation
        if (!request.getNewPassword().matches("^(?=.*[A-Z])(?=.*\\d).{6,}$")) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Password must contain at least one uppercase letter and one digit, and be at least 6 characters long"
            ));
        }
        
        // Set new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setLastPasswordChangeAt(LocalDateTime.now());
        userService.saveUser(user);
        
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
    
    // Request classes
    public static class UserProfileRequest {
        private String email;
        private String avatar;
        private String username;
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getAvatar() {
            return avatar;
        }
        
        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }
        
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
    
    public static class PasswordChangeRequest {
        private String currentPassword;
        private String newPassword;
        
        public String getCurrentPassword() {
            return currentPassword;
        }
        
        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }
        
        public String getNewPassword() {
            return newPassword;
        }
        
        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
} 