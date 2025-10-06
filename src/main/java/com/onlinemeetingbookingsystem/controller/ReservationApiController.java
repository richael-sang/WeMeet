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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservations")
public class ReservationApiController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private MeetingRoomService meetingRoomService;

    @Autowired
    private UserService userService;

    /**
     * 获取单个预订详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getReservation(@PathVariable Long id) {
        try {
            Optional<Booking> bookingOpt = bookingService.findById(id);
            if (!bookingOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Booking booking = bookingOpt.get();
            Map<String, Object> result = new HashMap<>();
            result.put("id", booking.getId());
            result.put("userId", booking.getUser().getId());
            result.put("userName", booking.getUser().getUsername());
            result.put("roomId", booking.getMeetingRoom().getId());
            result.put("roomName", booking.getMeetingRoom().getRoomName());
            result.put("topic", booking.getReason());
            result.put("startTime", booking.getStartTime().toString());
            result.put("endTime", booking.getEndTime().toString());
            result.put("status", booking.getStatus().toString());
            result.put("createdAt", booking.getCreatedAt().toString());

            List<Booking> userBookings = bookingService.getBookingsByUserId(booking.getUser().getId());
            userBookings.sort(Comparator.comparing(Booking::getCreatedAt));

            int sequenceNumber = -1;
            for (int i = 0; i < userBookings.size(); i++) {
                if (userBookings.get(i).getId().equals(booking.getId())) {
                    sequenceNumber = i + 1;
                    break;
                }
            }
            result.put("userSequenceNumber", sequenceNumber);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 获取特定会议室在特定日期的预订情况
     */
    @GetMapping("/room/{roomId}/availability")
    public ResponseEntity<?> getRoomAvailability(
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            
            List<Booking> bookings = bookingService.getBookingsByRoomAndDateRange(roomId, startOfDay, endOfDay);
            
            List<Map<String, Object>> result = bookings.stream()
                    .filter(b -> b.getStatus() == Booking.BookingStatus.APPROVED || 
                                b.getStatus() == Booking.BookingStatus.PENDING)
                    .map(this::convertBookingToMap)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 获取用户的所有预订
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserReservations(@PathVariable Long userId) {
        try {
            List<Booking> bookings = bookingService.getBookingsByUserId(userId);

            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 0; i < bookings.size(); i++) {
                Booking booking = bookings.get(i);
                Map<String, Object> map = convertBookingToMap(booking);
                map.put("userSequenceNumber", i + 1);
                result.add(map);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 创建新预订
     */
    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody Map<String, Object> request) {
        try {
            // 提取请求参数
            Long userId = Long.valueOf(request.get("userId").toString());
            Long roomId = Long.valueOf(request.get("roomId").toString());
            String topic = (String) request.get("topic");
            
            // 解析时间
            String startTimeStr = (String) request.get("startTime");
            String endTimeStr = (String) request.get("endTime");
            
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
            
            // 验证用户和会议室
            Optional<User> userOpt = userService.findById(userId);
            Optional<MeetingRoom> roomOpt = meetingRoomService.findById(roomId);
            
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body("User not found");
            }
            User user = userOpt.get(); // Get the user object
            
            // ---> CHECK IF USER IS LOCKED <---
            if (user.isLocked()) {
                 return ResponseEntity.badRequest().body("Your account is locked. Booking not allowed.");
            }
            // ---> END LOCK CHECK <---
            
            if (!roomOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Meeting room not found");
            }
            
            // 验证时间
            if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
                return ResponseEntity.badRequest().body("End time must be after start time");
            }
            
            if (startTime.isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body("Cannot book in the past");
            }
            
            // 检查时间段是否可用
            if (!meetingRoomService.isRoomAvailableForTimeSlot(roomId, startTime, endTime)) {
                return ResponseEntity.badRequest().body("Time slot is not available");
            }
            
            // 创建预订
            Booking booking = new Booking();
            booking.setUser(user);
            booking.setMeetingRoom(roomOpt.get());
            booking.setStartTime(startTime);
            booking.setEndTime(endTime);
            booking.setReason(topic);
            booking.setStatus(Booking.BookingStatus.APPROVED); // Directly approved for simplicity
            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());
            
            Booking savedBooking = bookingService.save(booking);
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", savedBooking.getId());
            result.put("message", "Reservation created successfully");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Log the exception for debugging
            System.err.println("Error creating reservation: " + e.getMessage());
            e.printStackTrace(); // Print stack trace
            return ResponseEntity.badRequest().body("Error creating reservation: " + e.getMessage());
        }
    }

    /**
     * 更新预订
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReservation(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            // 查找预订
            Optional<Booking> bookingOpt = bookingService.findById(id);
            if (!bookingOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Booking booking = bookingOpt.get();
            
            // 提取请求参数
            String topic = (String) request.get("topic");
            
            // 解析时间
            String startTimeStr = (String) request.get("startTime");
            String endTimeStr = (String) request.get("endTime");
            
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
            
            // 验证时间
            if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
                return ResponseEntity.badRequest().body("End time must be after start time");
            }
            
            if (startTime.isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body("Cannot book in the past");
            }
            
            // 检查时间段是否可用 (不包括当前预订)
            if (!meetingRoomService.isRoomAvailableForTimeSlot(booking.getMeetingRoom().getId(), 
                                                              startTime, endTime, booking.getId())) {
                return ResponseEntity.badRequest().body("Time slot is not available");
            }
            
            // 更新预订
            booking.setStartTime(startTime);
            booking.setEndTime(endTime);
            booking.setReason(topic);
            booking.setUpdatedAt(LocalDateTime.now());
            
            Booking updatedBooking = bookingService.save(booking);
            
            return ResponseEntity.ok("Reservation updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating reservation: " + e.getMessage());
        }
    }

    /**
     * 取消预订
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelReservation(@PathVariable Long id) {
        try {
            // 查找预订
            Optional<Booking> bookingOpt = bookingService.findById(id);
            if (!bookingOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Booking booking = bookingOpt.get();
            
            // 更新状态为已取消
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            booking.setUpdatedAt(LocalDateTime.now());
            
            bookingService.save(booking);
            
            return ResponseEntity.ok("Reservation cancelled successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error cancelling reservation: " + e.getMessage());
        }
    }

    /**
     * 获取特定会议室在特定日期的预订情况（专门为前端时间槽显示设计）
     */
    @GetMapping("/room/{roomId}/date/{date}")
    public ResponseEntity<?> getRoomBookingsByDate(
            @PathVariable Long roomId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            
            List<Booking> bookings = bookingService.getBookingsByRoomAndDateRange(roomId, startOfDay, endOfDay);
            
            // 过滤出有效的预订（已批准或待批准状态）
            List<Map<String, Object>> bookingsList = bookings.stream()
                    .filter(b -> b.getStatus() == Booking.BookingStatus.APPROVED || 
                                b.getStatus() == Booking.BookingStatus.PENDING)
                    .map(booking -> {
                        Map<String, Object> bookingMap = new HashMap<>();
                        bookingMap.put("id", booking.getId());
                        bookingMap.put("userId", booking.getUser().getId());
                        bookingMap.put("roomId", booking.getMeetingRoom().getId());
                        bookingMap.put("startTime", booking.getStartTime().toString());
                        bookingMap.put("endTime", booking.getEndTime().toString());
                        bookingMap.put("reason", booking.getReason());
                        bookingMap.put("status", booking.getStatus().toString());
                        return bookingMap;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("roomId", roomId);
            response.put("date", date.toString());
            response.put("bookings", bookingsList);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取预订信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 将Booking对象转换为Map
     */
    private Map<String, Object> convertBookingToMap(Booking booking) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", booking.getId());
        map.put("userId", booking.getUser().getId());
        map.put("userName", booking.getUser().getUsername());
        map.put("roomId", booking.getMeetingRoom().getId());
        map.put("roomName", booking.getMeetingRoom().getRoomName());
        map.put("topic", booking.getReason());
        map.put("startTime", booking.getStartTime().toString());
        map.put("endTime", booking.getEndTime().toString());
        map.put("status", booking.getStatus().toString());
        if (booking.getCreatedAt() != null) {
            map.put("createdAt", booking.getCreatedAt().toString());
        }
        return map;
    }
} 