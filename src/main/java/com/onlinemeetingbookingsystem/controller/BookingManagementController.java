package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.dto.BookingDTO;
import com.onlinemeetingbookingsystem.entity.Booking;
import com.onlinemeetingbookingsystem.entity.BookingRejectLog;
import com.onlinemeetingbookingsystem.entity.MeetingRoom;
import com.onlinemeetingbookingsystem.entity.Notification;
import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.repository.MeetingRoomRepository;
import com.onlinemeetingbookingsystem.repository.UserRepository;
import com.onlinemeetingbookingsystem.service.BookingService;
import com.onlinemeetingbookingsystem.service.BookingRejectLogService;
import com.onlinemeetingbookingsystem.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.security.Principal;

@Controller
@RequestMapping("/admin/bookings")
public class BookingManagementController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;

    @Autowired
    private UserRepository userRepository;

    private final BookingRejectLogService bookingRejectLogService;
    private final NotificationService notificationService;

    public BookingManagementController(BookingService bookingService, 
                                     BookingRejectLogService bookingRejectLogService,
                                     NotificationService notificationService) {
        this.bookingService = bookingService;
        this.bookingRejectLogService = bookingRejectLogService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String getAllBookings(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        List<Booking> bookings;
        
        Booking.BookingStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = Booking.BookingStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore filter
            }
        }
        
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        
        bookings = bookingService.searchBookings(roomId, statusEnum, startDateTime, endDateTime);
        
        List<BookingDTO> bookingDTOs = convertToDTO(bookings);
        
        // Add all rooms for filtering
        List<MeetingRoom> rooms = meetingRoomRepository.findAll();
        
        model.addAttribute("bookings", bookingDTOs);
        model.addAttribute("rooms", rooms);
        model.addAttribute("statuses", Booking.BookingStatus.values());
        model.addAttribute("selectedRoomId", roomId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin/bookings/bookings-management";
    }

    /**
     * 显示预约统计数据页面
     * @deprecated 请使用AdminController中的统计接口
     */
    @GetMapping("/statistics")
    public String getBookingStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        return "redirect:/admin/statistics?startDate=" + 
            (startDate != null ? startDate : "") + 
            "&endDate=" + 
            (endDate != null ? endDate : "");
    }
    
    /**
     * 拒绝预约接口
     */
    @PostMapping("/{id}/reject")
    @ResponseBody
    public ResponseEntity<String> rejectBooking(@PathVariable("id") Long id, 
                                              @RequestParam(required = false) String reason,
                                              Principal principal) {
        try {
            // 获取预订信息
            Booking booking = bookingService.getBookingById(id);
            if (booking == null) {
                return ResponseEntity.badRequest().body("Booking not found");
            }
            
            // 设置拒绝状态
            booking.setStatus(Booking.BookingStatus.REJECTED);
            booking.setUpdatedAt(LocalDateTime.now());
            bookingService.updateBooking(booking);
            
            // 创建拒绝日志 (使用当前登录的管理员)
            if (reason != null && !reason.isEmpty()) {
                // 获取当前登录的用户名
                String username = principal.getName();
                Optional<User> adminOpt = userRepository.findByUsername(username);
                
                if (adminOpt.isPresent()) {
                    User admin = adminOpt.get();
                    BookingRejectLog rejectLog = new BookingRejectLog();
                    rejectLog.setBooking(booking);
                    rejectLog.setAdmin(admin);
                    rejectLog.setRejectionReason(reason);
                    rejectLog.setRejectedAt(LocalDateTime.now());
                    // JPA会在保存前自动触发@PrePersist和@PreUpdate注解的方法
                    bookingRejectLogService.save(rejectLog);
                    
                    // 创建用户通知
                    createRejectionNotification(booking, reason);
                } else {
                    return ResponseEntity.badRequest().body("Admin user not found");
                }
            }
            
            return ResponseEntity.ok("Booking rejected successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error rejecting booking: " + e.getMessage());
        }
    }
    
    /**
     * 创建拒绝通知
     */
    private void createRejectionNotification(Booking booking, String reason) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            String formattedStart = booking.getStartTime().format(formatter);
            String formattedEnd = booking.getEndTime().format(formatter);

            Notification notification = new Notification();
            notification.setUserId(booking.getUser().getId());

            notification.setMessage("Your booking for " + booking.getMeetingRoom().getRoomName()
                    + " from " + formattedStart
                    + " to " + formattedEnd
                    + " has been rejected. Reason: " + reason);

            notificationService.save(notification);
        } catch (Exception e) {
            // 记录日志但不阻止流程
            System.err.println("Failed to create notification: " + e.getMessage());
        }
    }

    private List<BookingDTO> convertToDTO(List<Booking> bookings) {
        return bookings.stream().map(booking -> {
            BookingDTO dto = new BookingDTO();
            dto.setId(booking.getId());
            dto.setRoomId(booking.getMeetingRoom().getId());
            dto.setRoomName(booking.getMeetingRoom().getRoomName());
            dto.setRoomLocation(booking.getMeetingRoom().getLocation());
            dto.setUserId(booking.getUser().getId());
            dto.setUsername(booking.getUser().getUsername());
            dto.setUserEmail(booking.getUser().getEmail());
            dto.setStartTime(booking.getStartTime());
            dto.setEndTime(booking.getEndTime());
            dto.setReason(booking.getReason());
            dto.setStatus(booking.getStatus());
            dto.setCreatedAt(booking.getCreatedAt());
            dto.setUpdatedAt(booking.getUpdatedAt());
            
            // 格式化时间
            if (booking.getStartTime() != null) {
                dto.setFormattedStartTime(booking.getStartTime().toString().replace("T", " ").substring(0, 16));
            }
            if (booking.getEndTime() != null) {
                dto.setFormattedEndTime(booking.getEndTime().toString().replace("T", " ").substring(0, 16));
            }
            if (booking.getCreatedAt() != null) {
                dto.setFormattedCreatedAt(booking.getCreatedAt().toString().substring(0, 10));
            }
            
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 查看拒绝日志页面
     * @deprecated 请使用AdminController中的拒绝日志接口
     */
    @GetMapping("/reject-logs")
    public String viewRejectLogs(
            @RequestParam(defaultValue = "rejectedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            Model model) {
        
        return "redirect:/admin/booking-reject-logs?sortBy=" + sortBy + "&direction=" + direction;
    }
} 