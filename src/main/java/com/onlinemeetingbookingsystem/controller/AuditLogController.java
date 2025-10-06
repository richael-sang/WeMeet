package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.entity.AuditLog;
import com.onlinemeetingbookingsystem.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.onlinemeetingbookingsystem.service.ExportService;
import com.onlinemeetingbookingsystem.service.UserService;

@Controller
@RequestMapping("/admin/user-action-logs")
@RequiredArgsConstructor
@Slf4j
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final ExportService exportService;
    private final UserService userService;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 显示审计日志列表页面
     */
    @GetMapping
    @PreAuthorize("hasRole('Admin')")
    public String showAuditLogPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "timestamp") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            Model model) {

        log.info("请求访问审计日志页面，页码: {}, 大小: {}, 排序: {}, 方向: {}", page, size, sort, dir);

        Sort.Direction direction = dir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        
        Page<AuditLog> auditLogPage = auditLogService.findAllPaginated(pageable);
        
        // 为每个日志添加格式化的时间戳
        auditLogPage.getContent().forEach(log -> {
            if (log.getTimestamp() != null) {
                // 在模型中添加格式化日期的属性
                log.setFormattedDate(DATE_FORMATTER.format(log.getTimestamp()));
            } else {
                log.setFormattedDate("N/A");
            }
        });

        int totalPages = auditLogPage.getTotalPages();
        int currentPage = auditLogPage.getNumber();
        int startPage = Math.max(0, currentPage - 2);
        int endPage = Math.min(startPage + 4, totalPages - 1);
        startPage = Math.max(0, endPage - 4);

        model.addAttribute("auditLogPage", auditLogPage);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);

        return "admin/users/user-actions-log";
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('Admin')")
    public void exportAuditLogs(@RequestParam(defaultValue = "csv") String format,
                                @RequestParam(required = false) String fields,
                                HttpServletResponse response,
                                Principal principal) {

        log.info("管理员 '{}' 请求导出审计日志，格式: {}, 指定字段: {}", principal.getName(), format, fields == null ? "默认" : fields);

        try {
            List<AuditLog> logsToExport = auditLogService.findAll();
            
            // 为每个日志添加格式化的时间戳
            logsToExport.forEach(log -> {
                if (log.getTimestamp() != null) {
                    log.setFormattedDate(DATE_FORMATTER.format(log.getTimestamp()));
                } else {
                    log.setFormattedDate("N/A");
                }
            });

            Set<String> requestedFields = null;
            if (fields != null && !fields.trim().isEmpty()) {
                requestedFields = Arrays.stream(fields.split(","))
                                        .map(String::trim)
                                        .filter(s -> !s.isEmpty())
                                        .collect(Collectors.toSet());
            }

            if ("csv".equalsIgnoreCase(format)) {
                exportService.exportAuditLogsToCsv(logsToExport, requestedFields, response);
            } else if ("json".equalsIgnoreCase(format)) {
                exportService.exportAuditLogsToJson(logsToExport, requestedFields, response);
            } else {
                if (!response.isCommitted()) { 
                    response.setContentType("text/plain; charset=utf-8"); 
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST); 
                    response.getWriter().write("不支持的导出格式：" + format + "。请使用 'csv' 或 'json'。"); 
                }
                return;
            }
            
            // 注释：不再记录导出操作的审计日志，只记录用户锁定和解锁操作

        } catch (IOException e) {
            log.error("导出审计日志时发生 IO 错误: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("导出审计日志时发生意外错误: {}", e.getMessage(), e);
            try {
                if (!response.isCommitted()) {
                    response.setContentType("text/plain; charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("导出审计日志时发生内部错误：" + e.getMessage());
                }
            } catch (IOException ioEx) {
                log.error("尝试写入错误信息时发生IO异常", ioEx);
            }
        }
    }
}