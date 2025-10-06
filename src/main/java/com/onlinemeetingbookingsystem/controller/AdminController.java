package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.dto.BookingStatisticsDTO;
import com.onlinemeetingbookingsystem.entity.BookingRejectLog;
import com.onlinemeetingbookingsystem.service.BookingRejectLogService;
import com.onlinemeetingbookingsystem.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * Admin Controller Dashboard
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private BookingRejectLogService bookingRejectLogService;

    /**
     * 管理员面板首页
     */
    @GetMapping({"/dashboard", "", "/"})
    public String adminIndex() {
        return "admin/dashboard/dashboard";
    }

    /**
     * 用户管理页面 - 重定向到正确的URL
     */
    @GetMapping("/users-dashboard")
    public String userManagement() {
        return "redirect:/admin/users";
    }

    /**
     * 会议室管理页面 - 已有对应的控制器方法
     */
    @GetMapping("/rooms-dashboard")
    public String roomManagement() {
        return "redirect:/admin/rooms";
    }

    /**
     * 预约管理页面 - 已有对应的控制器方法
     */
    @GetMapping("/bookings-dashboard")
    public String bookingManagement() {
        return "redirect:/admin/bookings";
    }

    /**
     * 拒绝记录页面 - 重定向到拒绝日志页面
     */
    @GetMapping("/reject-logs")
    public String rejectLogs() {
        return "redirect:/admin/booking-reject-logs";
    }
    
    /**
     * 系统日志页面 - 重定向到统计页面
     */
    @GetMapping("/logs")
    public String systemLogs() {
        return "redirect:/admin/statistics";
    }
    
    /**
     * 系统设置页面 - 新增
     */
    @GetMapping("/settings")
    public String systemSettings() {
        return "admin/settings/settings";
    }
    
    /**
     * 预约统计数据页面
     */
    @GetMapping("/statistics")
    public String getBookingStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        // 设置默认日期范围为最近30天
        LocalDateTime start, end;
        
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        start = startDate.atStartOfDay();
        end = endDate.atTime(LocalTime.MAX);

        // 获取综合统计数据
        BookingStatisticsDTO statistics = bookingService.getComprehensiveStatistics(start, end);
        
        // 格式化最活跃的日期
        if (statistics.getMostActiveDay() != null) {
            statistics.setFormattedMostActiveDay(statistics.getMostActiveDay().toString());
        }
        
        // 添加数据到模型
        model.addAttribute("statistics", statistics);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        
        return "admin/statistics/statistics";
    }
    
    /**
     * 查看预约拒绝日志页面
     */
    @GetMapping("/booking-reject-logs")
    public String viewRejectLogs(
            @RequestParam(defaultValue = "rejectedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Model model) {
        
        List<BookingRejectLog> logs = bookingRejectLogService.findAll();
        
        // 确保空值处理
        for (BookingRejectLog log : logs) {
            if (log.getRejectedAt() == null) {
                log.setRejectedAt(LocalDateTime.now());
            }
        }
        
        // 简单的内存排序，可以根据需求改为数据库排序
        if ("rejectedAt".equals(sortBy)) {
            if ("asc".equals(direction)) {
                logs.sort(Comparator.comparing(BookingRejectLog::getRejectedAt, 
                    Comparator.nullsLast(Comparator.naturalOrder())));
            } else {
                logs.sort(Comparator.comparing(BookingRejectLog::getRejectedAt, 
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            }
        } else if ("booking".equals(sortBy)) {
            if ("asc".equals(direction)) {
                logs.sort(Comparator.comparing(log -> log.getBooking() != null ? log.getBooking().getId() : 0L));
            } else {
                logs.sort(Comparator.comparing((BookingRejectLog log) -> log.getBooking() != null ? log.getBooking().getId() : 0L).reversed());
            }
        } else if ("admin".equals(sortBy)) {
            if ("asc".equals(direction)) {
                logs.sort(Comparator.comparing(log -> log.getAdmin() != null ? log.getAdmin().getUsername() : ""));
            } else {
                logs.sort(Comparator.comparing((BookingRejectLog log) -> log.getAdmin() != null ? log.getAdmin().getUsername() : "").reversed());
            }
        }
        
        model.addAttribute("rejectLogs", logs);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        
        return "admin/bookings/booking-rejection-log";
    }
} 