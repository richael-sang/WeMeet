package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.dto.LoginRequest;
import com.onlinemeetingbookingsystem.dto.RegistrationRequest;
import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.service.FileService;
import com.onlinemeetingbookingsystem.service.UserService;
import com.onlinemeetingbookingsystem.util.CaptchaUtils;
import com.onlinemeetingbookingsystem.util.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private FileService fileService;

    @Autowired
    private CaptchaUtils captchaUtils;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 允许的图片格式
    private static final String[] ALLOWED_IMAGE_TYPES = {
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/webp"
    };

    // 最大文件大小（10MB）
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 用户注册接口
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        return userService.register(request);
    }

    /**
     * 重新发送验证码接口
     */
    @PostMapping("/sendRegisterCode")
    public ResponseEntity<?> sendRegisterCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "缺少邮箱"));
        }
        return userService.sendRegisterCode(email);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            if (!captchaUtils.validateCaptcha(loginRequest.getCaptchaKey(), loginRequest.getCaptchaCode())) {
                return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "验证码错误或已过期"));
            }

            Map<String, Object> result = userService.login(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
            );
            
            // Extract token from result
            String token = (String) result.get("token");

            if (token != null) {
                // write into redis
                String username = loginRequest.getUsername();
                stringRedisTemplate.opsForValue().set(
                        "token:" + username,
                        token,
                        jwtUtils.getExpiration(),
                        TimeUnit.SECONDS
                );

                // Create cookie
                Cookie cookie = new Cookie("AUTH-TOKEN", token);
                cookie.setHttpOnly(true); // Make it inaccessible to JavaScript
                cookie.setPath("/"); // Available for all paths
                // Set Max-Age based on token expiration (e.g., from JwtUtils)
                 try {
                     long expirationSeconds = jwtUtils.getExpiration(); // Assuming this returns seconds
                     cookie.setMaxAge((int) expirationSeconds);
                 } catch (Exception e) {
                     System.err.println("Failed to get token expiration for cookie Max-Age: " + e.getMessage());
                     // Set a default reasonable Max-Age if expiration cannot be determined, e.g., 1 day
                     cookie.setMaxAge(24 * 60 * 60); 
                 }
                // cookie.setSecure(true); // Uncomment this in production if using HTTPS
                // cookie.setSameSite("Strict"); // Or "Lax", consider based on your needs
                
                // Add cookie to the response
                response.addCookie(cookie);
                 System.out.println("AUTH-TOKEN cookie set.");
            }
            
            // Return original result in the body
            return ResponseEntity.ok().body(Map.of("code", "1", "data", result));
        } catch (Exception e) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", e.getMessage()));
        }
    }

    /**
     * 获取图形验证码
     */
    @GetMapping("/captchaImage")
    public ResponseEntity<?> getCaptchaImage() {
        try {
            Map<String, Object> captchaMap = captchaUtils.generateCaptcha();
            BufferedImage image = (BufferedImage) captchaMap.get("image");
            String captchaKey = (String) captchaMap.get("key");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

            Map<String, String> response = new HashMap<>();
            response.put("captchaKey", captchaKey);
            response.put("captchaImage", "data:image/png;base64," + base64Image);
            response.put("code", "1");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "获取图形验证码失败"));
        }
    }

    /**
     * 发送忘记密码验证码
     */
    @PostMapping("/sendForgetPwdCode")
    public ResponseEntity<?> sendForgetPwdCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.ok().body(Map.of("code", "0", "message", "缺少邮箱"));
        }
        return userService.sendForgetPasswordCode(email);
    }

    /**
     * 忘记密码
     */
    @PostMapping("/forgetPwd")
    public ResponseEntity<?> forgetPwd(@RequestBody User user) {
        return userService.resetPassword(user);
    }

    /**
     * 上传头像
     */
    @PostMapping(value = "/uploadAvatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(@RequestParam("avatar") MultipartFile file) {
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "请选择要上传的图片"));
            }

            // 检查文件大小
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "图片大小不能超过10MB"));
            }

            // 获取文件的ContentType
            String contentType = file.getContentType();

            // 检查是否是允许的图片类型
            if (contentType == null || !Arrays.asList(ALLOWED_IMAGE_TYPES).contains(contentType.toLowerCase())) {
                return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "只能上传 JPG、PNG、GIF、BMP、WEBP 格式的图片"));
            }

            // 检查文件扩展名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !isValidImageExtension(originalFilename)) {
                return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "文件格式不正确"));
            }

            // 进一步验证文件内容是否为图片
            try {
                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image == null) {
                    return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "无效的图片文件"));
                }
            } catch (IOException e) {
                return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "无效的图片文件"));
            }

            // 上传文件
            String fileUrl = fileService.uploadFile(file);
            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);
            response.put("message", "上传成功");
            response.put("code", "1");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("code", "0", "message", "图片上传失败: " + e.getMessage()));
        }
    }

    /**
     * 验证文件扩展名
     */
    private boolean isValidImageExtension(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp").contains(extension);
    }
} 