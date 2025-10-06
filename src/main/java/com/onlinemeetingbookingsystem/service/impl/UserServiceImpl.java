package com.onlinemeetingbookingsystem.service.impl;

import com.onlinemeetingbookingsystem.dto.RegistrationRequest;
import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.entity.Notification;
import com.onlinemeetingbookingsystem.repository.UserRepository;
import com.onlinemeetingbookingsystem.service.AuditLogService;
import com.onlinemeetingbookingsystem.service.FileService;
import com.onlinemeetingbookingsystem.service.NotificationService;
import com.onlinemeetingbookingsystem.service.UserService;
import com.onlinemeetingbookingsystem.util.JwtUtils;
import com.onlinemeetingbookingsystem.util.VerificationCodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private VerificationCodeUtil verificationCodeUtil;

    @Autowired
    private FileService fileService;
    
    private static final String ACTION_LOCK_USER = "LOCK_USER";
    private static final String ACTION_UNLOCK_USER = "UNLOCK_USER";
    private static final String ADMIN_KEY = "Cpt202Group50"; // Admin registration key
    private static final String DEFAULT_AVATAR = "/images/default_avatar.jpeg";

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Override
    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return userRepository.findAll(pageable);
        }
        
        // Simple implementation since we don't have JpaSpecificationExecutor
        String searchTermLower = searchTerm.toLowerCase();
        return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            searchTermLower, searchTermLower, pageable);
    }

    @Override
    @Transactional
    public void lockUser(Long userId, String reason, User admin) {
        userRepository.findById(userId).ifPresent(user -> {
            if (!user.isLocked()) {
                user.setLocked(true);
                user.setLockUntil(LocalDateTime.now().plusYears(100));
                userRepository.save(user);
                auditLogService.logAdminAction(admin, ACTION_LOCK_USER, user, reason);
                System.out.println("User " + userId + " locked by admin " + admin.getUsername());

                // Create notification for the user
                Notification notification = new Notification();
                notification.setUserId(userId);
                notification.setMessage("Your account has been locked by an administrator. Reason: " + reason);
                notification.setIsRead(false);
                notificationService.save(notification);
            }
        });
    }

    @Override
    @Transactional
    public void unlockUser(Long userId, String reason, User admin) {
        userRepository.findById(userId).ifPresent(user -> {
            if (user.isLocked()) {
                user.setLocked(false);
                user.setLockUntil(null);
                user.setFailedAttempts(0);
                userRepository.save(user);
                auditLogService.logAdminAction(admin, ACTION_UNLOCK_USER, user, reason);
                System.out.println("User " + userId + " unlocked by admin " + admin.getUsername());

                // Create notification for the user
                Notification notification = new Notification();
                notification.setUserId(userId);
                notification.setMessage("Your account has been unlocked by an administrator. Reason: " + reason);
                notification.setIsRead(false);
                notificationService.save(notification);
            }
        });
    }

    @Override
    public boolean isUserLocked(Long userId) {
        return userRepository.findById(userId).map(User::isLocked).orElse(false);
    }

    @Override
    @Transactional
    public void incrementFailedAttempts(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public void resetFailedAttempts(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFailedAttempts(0);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public void updateLastLoginTime(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
    
    @Override
    public Page<User> findUsersByCriteria(String searchTerm, String roleString, String status, Pageable pageable) {
        if ((searchTerm == null || searchTerm.trim().isEmpty()) && 
            (roleString == null || roleString.trim().isEmpty()) && 
            (status == null || status.trim().isEmpty())) {
            // 如果没有任何筛选条件，直接返回所有用户
            return userRepository.findAll(pageable);
        }
        
        List<User> filteredUsers = userRepository.findAll();
        
        // 1. 按搜索词过滤（用户名或邮箱）
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String searchTermLower = searchTerm.toLowerCase();
            filteredUsers = filteredUsers.stream()
                .filter(user -> 
                    (user.getUsername() != null && user.getUsername().toLowerCase().contains(searchTermLower)) || 
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchTermLower)))
                .collect(Collectors.toList());
        }
        
        // 2. 按角色过滤
        if (roleString != null && !roleString.trim().isEmpty()) {
            filteredUsers = filteredUsers.stream()
                .filter(user -> user.getRole() != null && 
                               user.getRole().toString().equalsIgnoreCase(roleString))
                .collect(Collectors.toList());
        }
        
        // 3. 按状态过滤（活跃/锁定）
        if (status != null && !status.trim().isEmpty()) {
            filteredUsers = filteredUsers.stream()
                .filter(user -> {
                    if ("active".equalsIgnoreCase(status)) {
                        return !user.isLocked();
                    } else if ("locked".equalsIgnoreCase(status)) {
                        return user.isLocked();
                    }
                    return true;
                })
                .collect(Collectors.toList());
        }
        
        // 对过滤后的结果应用分页
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredUsers.size());
        
        // 应用排序
        String sortProperty = pageable.getSort().iterator().next().getProperty();
        boolean ascending = pageable.getSort().iterator().next().isAscending();
        
        // 根据排序字段对列表进行排序
        filteredUsers.sort((u1, u2) -> {
            int result = 0;
            switch (sortProperty) {
                case "id":
                    result = u1.getId().compareTo(u2.getId());
                    break;
                case "username":
                    result = (u1.getUsername() != null && u2.getUsername() != null) ? 
                            u1.getUsername().compareTo(u2.getUsername()) : 0;
                    break;
                case "email":
                    result = (u1.getEmail() != null && u2.getEmail() != null) ? 
                            u1.getEmail().compareTo(u2.getEmail()) : 0;
                    break;
                case "role":
                    result = (u1.getRole() != null && u2.getRole() != null) ? 
                            u1.getRole().compareTo(u2.getRole()) : 0;
                    break;
                case "locked":
                    result = Boolean.compare(u1.isLocked(), u2.isLocked());
                    break;
                case "createdAt":
                    result = (u1.getCreatedAt() != null && u2.getCreatedAt() != null) ? 
                            u1.getCreatedAt().compareTo(u2.getCreatedAt()) : 0;
                    break;
                default:
                    result = u1.getId().compareTo(u2.getId());
            }
            return ascending ? result : -result;
        });
        
        // 只取分页范围内的数据
        List<User> pageContent = start < end ? filteredUsers.subList(start, end) : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, filteredUsers.size());
    }
    
    // New authentication methods
    
    @Override
    @Transactional
    public ResponseEntity<?> register(RegistrationRequest request) {
        // Validate username
        if (!StringUtils.hasText(request.getUsername())) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "Please enter a username"));
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "Username already registered"));
        }
        
        // Validate password
        String password = request.getPassword();
        if (!StringUtils.hasText(password)) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "Please enter a password"));
        }
        if (!password.matches("^(?=.*[A-Z])(?=.*\\d).{6,}$")) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "Password must contain at least one uppercase letter, one digit, and be at least 6 characters long"));
        }
        
        // Validate email
        String email = request.getEmail();
        if (!StringUtils.hasText(email)) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "Please enter an email"));
        }
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "Email already registered"));
        }
        
        // Validate register code
        String registerCode = request.getRegisterCode();
        if (!StringUtils.hasText(registerCode)) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "Please enter the email verification code"));
        }
        StringBuffer msgBuffer = new StringBuffer();
        if (!verificationCodeUtil.verifyCode(email, registerCode, "register:", msgBuffer)) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", msgBuffer.toString()));
        }
        
        // Validate admin role
        User.Role role = request.getRole();
        String adminKey = request.getAdminKey();
        if (role == User.Role.Admin && !ADMIN_KEY.equals(adminKey)) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "Incorrect administrator key, please try again."));
        }
        
        // --- Process Avatar ---
        String avatarUrl = DEFAULT_AVATAR; // Start with default
        if (StringUtils.hasText(request.getAvatarDataUrl())) {
            try {
                // Decode Base64 Data URL (e.g., "data:image/png;base64,iVBOR...")
                String base64Data = request.getAvatarDataUrl().split(",")[1];
                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                
                // Determine file extension (simple check, adjust if needed)
                String mimeType = request.getAvatarDataUrl().split(",")[0].split(":")[1].split(";")[0];
                String extension = mimeType.split("/")[1]; // e.g., "png"
                String filename = "avatar_" + request.getUsername() + "_" + System.currentTimeMillis() + "." + extension;

                // Upload using FileService
                avatarUrl = fileService.uploadFile(imageBytes, filename, mimeType); // Assuming FileService has an overload for byte[]
                System.out.println("Successfully uploaded avatar, URL: " + avatarUrl);

            } catch (Exception e) {
                // Log the error but proceed with default avatar
                System.err.println("Failed to process/upload avatar from Base64 data: " + e.getMessage());
                // Optionally return an error if avatar upload is critical
                // return ResponseEntity.ok().body(Map.of("code", "0", "message", "Avatar processing failed: " + e.getMessage()));
                avatarUrl = DEFAULT_AVATAR; // Fallback to default
            }
        }
        // --- End Avatar Processing ---

        // Create new User entity
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setEmail(email);
        newUser.setRole(role);
        newUser.setAvatar(avatarUrl); // Set the processed or default URL
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setEmailVerified(true); // Since we verified with code
        newUser.setLocked(false);
        newUser.setFailedAttempts(0);
        
        // Save the user
        User savedUser = userRepository.save(newUser);
        
        System.out.println("User registered successfully: " + savedUser.getUsername() + " with Avatar: " + savedUser.getAvatar());
        return ResponseEntity.ok().body(Map.of("code", "1", "message", "Registration successful", "userId", savedUser.getId()));
    }
    
    @Override
    public Map<String, Object> login(String username, String password) {
        try {
            System.out.println("Attempting login for username: " + username);
            
            // Authenticate with Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            
            // Get user details
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));
            
            System.out.println("Authentication successful, User ID: " + user.getId() + ", Role: " + user.getRole());
            
            // Generate JWT token
            String token = jwtUtils.generateToken(username);
            
            // Store token in Redis for expiration/revocation management
            String redisKey = "token:" + username;
            try {
                redisTemplate.opsForValue().set(redisKey, token, jwtUtils.getExpiration(), TimeUnit.SECONDS);
                System.out.println("Token stored in Redis");
            } catch (Exception e) {
                System.err.println("Failed to store token in Redis: " + e.getMessage());
                // Continue login process even if Redis fails
            }
            
            // Update last login time
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            
            // Reset failed login attempts
            resetFailedAttempts(user.getId());
            
            // Return user info and token
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("userId", user.getId());
            result.put("username", user.getUsername());
            result.put("email", user.getEmail());
            result.put("avatar", user.getAvatar());
            result.put("role", user.getRole().toString());
            
            
            System.out.println("Login successful, returning data: " + result);
            return result;
            
        } catch (BadCredentialsException e) {
            System.err.println("Login failed: " + e.getMessage());
            
            // Increment failed attempts if user exists
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                incrementFailedAttempts(user.getId());
                
                // Lock account after 5 failed attempts
                if (user.getFailedAttempts() >= 5) {
                    user.setLocked(true);
                    user.setLockUntil(LocalDateTime.now().plusHours(24)); // Lock for 24 hours
                    userRepository.save(user);
                    throw new BadCredentialsException("Account is locked. Please try again in 24 hours or contact an administrator");
                }
            }
            
            throw new BadCredentialsException("Invalid username or password");
        } catch (Exception e) {
            System.err.println("Unknown error during login process: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @Override
    public ResponseEntity<?> sendRegisterCode(String email) {
        // Check if email is already registered
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "This email is already registered"));
        }
        
        try {
            // Send verification code
            return verificationCodeUtil.sendRegisterCode(email);
        } catch (Exception e) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "Failed to send verification code: " + e.getMessage()));
        }
    }
    
    @Override
    public ResponseEntity<?> sendForgetPasswordCode(String email) {
        // Check if email exists
        if (userRepository.findByEmail(email).isEmpty()) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "This email is not registered"));
        }
        
        try {
            // Send verification code
            return verificationCodeUtil.sendForgetPasswordCode(email);
        } catch (Exception e) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "Failed to send verification code: " + e.getMessage()));
        }
    }
    
    @Override
    @Transactional
    public ResponseEntity<?> resetPassword(User requestUser) {
        String email = requestUser.getEmail();
        String password = requestUser.getPassword();
        String forgetPwdCode = requestUser.getForgetPwdCode();
        
        // Validate email
        if (!StringUtils.hasText(email)) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "Please enter an email"));
        }
        
        // Validate password
        if (!StringUtils.hasText(password)) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "Please enter a password"));
        }
        
        // Password must contain at least one uppercase letter and one digit, and be at least 6 characters
        if (!password.matches("^(?=.*[A-Z])(?=.*\\d).{6,}$")) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "Password must contain at least one uppercase letter, one digit, and be at least 6 characters long"));
        }
        
        // Validate verification code
        if (!StringUtils.hasText(forgetPwdCode)) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "Please enter the verification code"));
        }
        
        // 查找用户
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "This email is not registered"));
        }
        
        // 验证验证码
        StringBuffer msgBuffer = new StringBuffer();
        boolean isCodeValid = verificationCodeUtil.verifyCode(email, forgetPwdCode, "forget:", msgBuffer);
        
        if (!isCodeValid) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", msgBuffer.toString()));
        }
        
        // 更新用户密码
        User user = userOpt.get();
        
        // Update password
        user.setPassword(passwordEncoder.encode(password));
        user.setLastPasswordChangeAt(LocalDateTime.now());
        
        // If account was locked due to failed attempts, unlock it
        if (user.isLocked() && user.getLockUntil() != null) {
            user.setLocked(false);
            user.setLockUntil(null);
            user.setFailedAttempts(0);
        }
        
        userRepository.save(user);
        
        return ResponseEntity.ok().body(Map.of("code", "1", "message", "Password reset successful"));
    }
} 