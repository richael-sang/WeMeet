package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.dto.BookingDTO;
import com.onlinemeetingbookingsystem.entity.Booking;
import com.onlinemeetingbookingsystem.entity.MeetingRoom;
import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.repository.MeetingRoomRepository;
import com.onlinemeetingbookingsystem.service.BookingService;
import com.onlinemeetingbookingsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller for handling the view of the user interface
 */
@Controller
public class UserViewController {

    @Autowired
    private UserService userService;

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;
    
    @Autowired
    private BookingService bookingService;

    /**
     * 用户主页
     */
    @GetMapping("/user/dashboard")
    public String userDashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // FIRST: Check if it's an authenticated admin and redirect immediately
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()) &&
            auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin"; // 管理员重定向到管理员面板
        }

        // If not admin, proceed with normal user logic
        Optional<User> userOpt = Optional.ofNullable(auth)
                                       .filter(Authentication::isAuthenticated)
                                       .map(Authentication::getName)
                                       .filter(name -> !"anonymousUser".equals(name))
                                       .flatMap(userService::findByUsername);

        if (userOpt.isPresent()) {
            User currentUser = userOpt.get(); // Get user object
            model.addAttribute("user", currentUser); // Add user object for the template
            return "user/index"; // 普通用户返回用户主页
        } else {
            // User is not authenticated or not found (and not admin)
            System.out.println("Unauthenticated access attempt to /user/dashboard or user not found.");
            return "redirect:/login"; // Redirect to login
        }
    }
    
    /**
     * 个人资料页面
     */
    @GetMapping("/user/profile")
    public String userProfile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userOpt = Optional.ofNullable(auth)
                                       .filter(Authentication::isAuthenticated)
                                       .map(Authentication::getName)
                                       .filter(name -> !"anonymousUser".equals(name))
                                       .flatMap(userService::findByUsername);

        if (userOpt.isPresent()) {
             User currentUser = userOpt.get();
             model.addAttribute("user", currentUser);
             System.out.println("User " + currentUser.getUsername() + " is authenticated, adding user to model for /profile page.");
             return "user/profile";
        } else {
             System.out.println("Unauthenticated access attempt to /user/profile or user not found.");
             return "redirect:/login"; // Redirect to login
        }
    }

    /**
     * 会议室列表页面
     */
    @GetMapping("/user/rooms")
    public String listRooms(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userOpt = Optional.ofNullable(auth)
                                       .filter(Authentication::isAuthenticated)
                                       .map(Authentication::getName)
                                       .filter(name -> !"anonymousUser".equals(name))
                                       .flatMap(userService::findByUsername);

        if (userOpt.isPresent()) {
            User currentUser = userOpt.get(); // Get the user object
            model.addAttribute("user", currentUser); // <-- Add the full user object
            
            List<MeetingRoom> rooms = meetingRoomRepository.findAll();
            model.addAttribute("rooms", rooms);
            return "user/meeting-rooms";
        } else {
            System.out.println("Unauthenticated access attempt to /user/rooms or user not found.");
            return "redirect:/login";
        }
    }
    
    /**
     * 用户个人预订列表页面
     */
    @GetMapping("/user/bookings")
    public String getMyBookings(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userOpt = Optional.ofNullable(auth)
                                       .filter(Authentication::isAuthenticated)
                                       .map(Authentication::getName)
                                       .filter(name -> !"anonymousUser".equals(name))
                                       .flatMap(userService::findByUsername);

        if (userOpt.isPresent()) {
            User currentUser = userOpt.get(); // Get the user object
            Long currentUserId = currentUser.getId();
            model.addAttribute("user", currentUser); // <-- Add the full user object
            
            // Original logic from UserBookingController starts here, using currentUserId
            List<Booking> allBookings = bookingService.getBookingsByUserId(currentUserId);
            List<Booking> filteredBookings = allBookings;
            
            // Filter by status
            if (status != null && !status.isEmpty()) {
                try {
                    final Booking.BookingStatus statusEnum = Booking.BookingStatus.valueOf(status.toUpperCase());
                    filteredBookings = filteredBookings.stream()
                        .filter(r -> r.getStatus() == statusEnum)
                        .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {/* Ignore invalid status */} 
            }
            
            // Filter by date
            final LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
            final LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
            
            // Filter by room
            if (roomId != null) {
                filteredBookings = filteredBookings.stream()
                        .filter(r -> r.getMeetingRoom().getId().equals(roomId))
                        .collect(Collectors.toList());
            }
            
            // Filter by start date
            if (startDateTime != null) {
                filteredBookings = filteredBookings.stream()
                        .filter(r -> r.getStartTime().isAfter(startDateTime) || r.getStartTime().isEqual(startDateTime))
                        .collect(Collectors.toList());
            }
            
            // Filter by end date
            if (endDateTime != null) {
                filteredBookings = filteredBookings.stream()
                        .filter(r -> r.getEndTime().isBefore(endDateTime) || r.getEndTime().isEqual(endDateTime))
                        .collect(Collectors.toList());
            }
            
            List<BookingDTO> bookingDTOs = convertToDTO(filteredBookings);
            List<MeetingRoom> rooms = meetingRoomRepository.findAll(); // Keep for filter dropdown
            
            model.addAttribute("bookings", bookingDTOs);
            model.addAttribute("rooms", rooms);
            model.addAttribute("statuses", Booking.BookingStatus.values());
            model.addAttribute("selectedRoomId", roomId);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "user/my-bookings";
        } else {
            System.out.println("Unauthenticated access attempt to /user/bookings or user not found.");
            return "redirect:/login"; // Redirect to login
        }
    }

    // Helper method moved from UserBookingController
    private List<BookingDTO> convertToDTO(List<Booking> bookings) {
        return bookings.stream().map(booking -> {
            BookingDTO dto = new BookingDTO();
            dto.setId(booking.getId());
            // Add null checks for safety
            if (booking.getMeetingRoom() != null) { 
                dto.setRoomId(booking.getMeetingRoom().getId());
                dto.setRoomName(booking.getMeetingRoom().getRoomName());
                dto.setRoomLocation(booking.getMeetingRoom().getLocation());
            } 
            if (booking.getUser() != null) {
                dto.setUserId(booking.getUser().getId());
                dto.setUsername(booking.getUser().getUsername());
                dto.setUserEmail(booking.getUser().getEmail());
            } 
            dto.setStartTime(booking.getStartTime());
            dto.setEndTime(booking.getEndTime());
            dto.setReason(booking.getReason());
            dto.setStatus(booking.getStatus());
            dto.setCreatedAt(booking.getCreatedAt());
            dto.setUpdatedAt(booking.getUpdatedAt());
            
            // Format time safely
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
} 