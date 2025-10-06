package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.service.UserService;
import com.onlinemeetingbookingsystem.service.AuditLogService;
import com.onlinemeetingbookingsystem.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping({"/admin/users", "/admin/user"})
@Slf4j
public class UserManagementController {
    
    private final UserService userService;
    private final ExportService exportService;
    private final AuditLogService auditLogService;

    @Autowired
    public UserManagementController(UserService userService, ExportService exportService, AuditLogService auditLogService) {
        this.userService = userService;
        this.exportService = exportService;
        this.auditLogService = auditLogService;
    }

    // 添加导出用户的方法
    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportUsers(@RequestParam(defaultValue = "csv") String format,
                            @RequestParam(required = false) String fields,
                            HttpServletResponse response,
                            Principal principal) {

        log.info("管理员 '{}' 请求导出用户数据，格式: {}, 指定字段: {}", principal.getName(), format, fields);

        try {
            // 1. 获取需要导出的用户数据
            List<User> users = userService.findAll();

            // 2. 解析请求的字段 (如果提供了)
            Set<String> requestedFields = null;
            if (fields != null && !fields.trim().isEmpty()) {
                requestedFields = Arrays.stream(fields.split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .collect(Collectors.toSet());
                log.debug("请求导出的字段: {}", requestedFields);
            } else {
                log.debug("未指定导出字段，将使用默认字段。");
            }

            // 3. 调用 ExportService 处理导出
            if ("csv".equalsIgnoreCase(format)) {
                exportService.exportUsersToCsv(users, requestedFields, response);
            } else if ("json".equalsIgnoreCase(format)) {
                exportService.exportUsersToJson(users, requestedFields, response);
            } else {
                log.warn("不支持的导出格式: '{}'，将默认使用 CSV。", format);
                response.setContentType("text/plain; charset=utf-8");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("不支持的导出格式：" + format + "。请使用 'csv' 或 'json'。");
                return;
            }

            // 4. 记录审计日志
            String adminUsername = principal.getName();
            Optional<User> adminUserOpt = userService.findByUsername(adminUsername);
            
            if (adminUserOpt.isPresent()) {
                User adminUser = adminUserOpt.get();
                String details = String.format("导出了 %d 条用户数据，格式为 %s，字段为 %s",
                                            users.size(),
                                            format.toLowerCase(),
                                            (requestedFields != null ? requestedFields.toString() : "默认"));
                auditLogService.logGenericAdminAction(adminUser.getId(), adminUsername, "EXPORT_USERS", details);
            } else {
                log.warn("无法记录导出操作的审计日志，因为找不到管理员用户 '{}'", adminUsername);
            }

        } catch (IOException e) {
            log.error("导出用户数据时发生 IO 错误: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("导出用户数据时发生意外错误: {}", e.getMessage(), e);
            try {
                if (!response.isCommitted()) {
                    response.setContentType("text/plain; charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("导出用户数据时发生内部错误：" + e.getMessage());
                }
            } catch (IOException ioEx) {
                log.error("尝试写入导出错误信息到响应时再次发生 IO 错误", ioEx);
            }
        }
    }

    @GetMapping
    public String listUsers(@RequestParam(required = false) String search,
                           @RequestParam(required = false) String role, // <-- 添加 role 参数
                           @RequestParam(required = false) String status, // <-- 添加 status 参数
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(defaultValue = "id") String sort,
                           @RequestParam(defaultValue = "asc") String dir,
                           Model model) {
        
        Sort.Direction direction = dir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortField = Arrays.asList("id", "username", "email", "role", "locked", "createdAt").contains(sort) ? sort : "id";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        
         // --- 调用新的 Service 方法，传入所有参数 ---
         Page<User> usersPage = userService.findUsersByCriteria(search, role, status, pageable);
         // --- 结束调用 ---
        
        // Calculate pagination values for the template
        int totalPages = usersPage.getTotalPages();
        int currentPage = usersPage.getNumber();
        
        // Calculate start and end page for pagination display
        int startPage = Math.max(0, currentPage - 2);
        int endPage = Math.min(totalPages > 0 ? totalPages - 1 : 0, startPage + 4);
        startPage = Math.max(0, endPage - 4);
        
        model.addAttribute("usersPage", usersPage);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("sort", sortField);
        model.addAttribute("dir", dir);
        model.addAttribute("search", search);
        model.addAttribute("role", role); // <-- 将 role 添加回模型，以便下拉框保持选中状态
        model.addAttribute("status", status); // <-- 将 status 添加回模型
        
        return "admin/users/users-management";
    }
    
    @PostMapping("/{userId}/lock")
    @ResponseBody
    public ResponseEntity<String> lockUser(@PathVariable Long userId, 
                                          @RequestParam(required = false) String reason,
                                          Principal principal) {
        try {
            // Get the admin user from the principal
            Optional<User> adminOpt = userService.findByUsername(principal.getName());
            if (!adminOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Admin user not found");
            }
            
            User admin = adminOpt.get();
            userService.lockUser(userId, reason, admin);
            return ResponseEntity.ok("User locked successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error locking user: " + e.getMessage());
        }
    }
    
    @PostMapping("/{userId}/unlock")
    @ResponseBody
    public ResponseEntity<String> unlockUser(@PathVariable Long userId, 
                                            @RequestParam(required = false) String reason,
                                            Principal principal) {
        try {
            // Get the admin user from the principal
            Optional<User> adminOpt = userService.findByUsername(principal.getName());
            if (!adminOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Admin user not found");
            }
            
            User admin = adminOpt.get();
            userService.unlockUser(userId, reason, admin);
            return ResponseEntity.ok("User unlocked successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error unlocking user: " + e.getMessage());
        }
    }
} 