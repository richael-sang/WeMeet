package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.entity.Booking;
import com.onlinemeetingbookingsystem.entity.MeetingRoom;
import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.service.BookingService;
import com.onlinemeetingbookingsystem.service.MeetingRoomService;
import com.onlinemeetingbookingsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
public class UserBookingApiController {

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private MeetingRoomService meetingRoomService;
    
    @Autowired
    private UserService userService;
    
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 创建新预订
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBooking(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取请求参数
            Long roomId = Long.parseLong(request.get("roomId"));
            Long userId = Long.parseLong(request.get("userId"));
            String dateStr = request.get("date");
            String startTimeStr = request.get("startTime");
            String endTimeStr = request.get("endTime");
            String reason = request.get("reason");
            
            // 验证用户和会议室
            Optional<User> userOpt = userService.findById(userId);
            Optional<MeetingRoom> roomOpt = meetingRoomService.findById(roomId);
            
            if (!userOpt.isPresent() || !roomOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "用户或会议室不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 解析日期和时间
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime startTime = LocalTime.parse(startTimeStr);
            LocalTime endTime = LocalTime.parse(endTimeStr);
            
            LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
            LocalDateTime endDateTime = LocalDateTime.of(date, endTime);
            
            // 验证时间是否有效
            if (startDateTime.isAfter(endDateTime) || startDateTime.isEqual(endDateTime)) {
                response.put("success", false);
                response.put("message", "结束时间必须晚于开始时间");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (startDateTime.isBefore(LocalDateTime.now())) {
                response.put("success", false);
                response.put("message", "不能预订过去的时间");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查时间段是否可用
            if (!meetingRoomService.isRoomAvailableForTimeSlot(roomId, startDateTime, endDateTime)) {
                response.put("success", false);
                response.put("message", "所选时间段已被预订");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 创建预订
            Booking booking = new Booking();
            booking.setUser(userOpt.get());
            booking.setMeetingRoom(roomOpt.get());
            booking.setStartTime(startDateTime);
            booking.setEndTime(endDateTime);
            booking.setReason(reason);
            booking.setStatus(Booking.BookingStatus.PENDING);
            
            bookingService.save(booking);
            
            response.put("success", true);
            response.put("bookingId", booking.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "创建预订失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取特定会议室在特定日期的预订情况
     */
    @GetMapping("/room/{roomId}/date/{date}")
    public ResponseEntity<Map<String, Object>> getRoomBookingsByDate(
            @PathVariable Long roomId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            
            // 获取当前登录用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Optional<User> userOpt = userService.findByUsername(auth.getName());
            Long currentUserId = userOpt.isPresent() ? userOpt.get().getId() : null;
            
            // 获取会议室
            Optional<MeetingRoom> roomOpt = meetingRoomService.findById(roomId);
            if (!roomOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "会议室不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取该日期该会议室的所有预订
            List<Booking> bookings = bookingService.getBookingsByRoomAndDateRange(roomId, startOfDay, endOfDay);
            
            // 转换为前端需要的数据格式
            List<Map<String, Object>> bookingsList = bookings.stream()
                    .filter(b -> b.getStatus() == Booking.BookingStatus.APPROVED || 
                                b.getStatus() == Booking.BookingStatus.PENDING)
                    .map(booking -> {
                        Map<String, Object> bookingMap = new HashMap<>();
                        bookingMap.put("id", booking.getId());
                        bookingMap.put("userId", booking.getUser().getId());
                        bookingMap.put("roomId", booking.getMeetingRoom().getId());
                        bookingMap.put("startTime", booking.getStartTime().toLocalTime().format(timeFormatter));
                        bookingMap.put("endTime", booking.getEndTime().toLocalTime().format(timeFormatter));
                        bookingMap.put("reason", booking.getReason());
                        bookingMap.put("status", booking.getStatus().toString());
                        return bookingMap;
                    })
                    .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("roomId", roomId);
            response.put("date", date.toString());
            response.put("bookings", bookingsList);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取预订信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取用户的所有预订
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserBookings(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证用户
            Optional<User> userOpt = userService.findById(userId);
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取用户的所有预订
            List<Booking> bookings = bookingService.getBookingsByUserId(userId);
            
            // 转换为前端需要的数据格式
            List<Map<String, Object>> bookingsList = bookings.stream()
                    .map(booking -> {
                        Map<String, Object> bookingMap = new HashMap<>();
                        bookingMap.put("id", booking.getId());
                        bookingMap.put("roomId", booking.getMeetingRoom().getId());
                        bookingMap.put("roomName", booking.getMeetingRoom().getRoomName());
                        bookingMap.put("date", booking.getStartTime().toLocalDate().toString());
                        bookingMap.put("startTime", booking.getStartTime().toLocalTime().format(timeFormatter));
                        bookingMap.put("endTime", booking.getEndTime().toLocalTime().format(timeFormatter));
                        bookingMap.put("reason", booking.getReason());
                        bookingMap.put("status", booking.getStatus().toString());
                        return bookingMap;
                    })
                    .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("bookings", bookingsList);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取预订信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 取消预订
     */
    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(@PathVariable Long bookingId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 获取当前登录用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Optional<User> userOpt = userService.findByUsername(auth.getName());
            
            if (!userOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "用户未认证");
                return ResponseEntity.badRequest().body(response);
            }
            
            User currentUser = userOpt.get();
            
            // 获取预订信息
            Optional<Booking> bookingOpt = bookingService.findById(bookingId);
            if (!bookingOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "预订不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            Booking booking = bookingOpt.get();
            
            // 验证用户是否有权取消预订
            if (!booking.getUser().getId().equals(currentUser.getId()) && currentUser.getRole() != User.Role.Admin) {
                response.put("success", false);
                response.put("message", "无权取消此预订");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 只能取消待审核或已批准的预订
            if (booking.getStatus() != Booking.BookingStatus.PENDING && booking.getStatus() != Booking.BookingStatus.APPROVED) {
                response.put("success", false);
                response.put("message", "无法取消已拒绝或已取消的预订");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 更新预订状态
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            bookingService.save(booking);
            
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "取消预订失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 