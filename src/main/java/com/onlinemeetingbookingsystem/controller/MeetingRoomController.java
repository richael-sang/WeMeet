package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.entity.MeetingRoom;
import com.onlinemeetingbookingsystem.repository.MeetingRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class MeetingRoomController {

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;

    // Admin routes
    
    @GetMapping("/admin/rooms")
    public String adminRooms(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String floor,
            Model model) {
        
        // Get all meeting rooms initially to populate filters
        List<MeetingRoom> allRooms = meetingRoomRepository.findAll();

        // Apply filters
        List<MeetingRoom> filteredRooms = allRooms.stream()
            .filter(room -> name == null || name.isEmpty() || 
                   (room.getRoomName() != null && room.getRoomName().toLowerCase().contains(name.toLowerCase())))
            .filter(room -> location == null || location.isEmpty() || 
                   (room.getLocation() != null && room.getLocation().equalsIgnoreCase(location)))
            .filter(room -> floor == null || floor.isEmpty() || 
                   (room.getFloor() != null && room.getFloor().toString().equals(floor)))
            .collect(Collectors.toList());
        
        model.addAttribute("rooms", filteredRooms); // Use filtered rooms
        
        // Extract unique locations from all rooms for filter dropdown
        Set<String> locations = allRooms.stream()
                .filter(room -> room.getLocation() != null && !room.getLocation().isEmpty())
                .map(MeetingRoom::getLocation)
                .collect(Collectors.toSet());
        model.addAttribute("locations", locations);
        
        // Extract unique floors from all rooms for filter dropdown
        Set<Integer> floors = allRooms.stream()
                .filter(room -> room.getFloor() != null)
                .map(MeetingRoom::getFloor)
                .collect(Collectors.toSet());
        model.addAttribute("floors", floors);
        
        // Add applied filter values to the model for Thymeleaf
        model.addAttribute("filterName", name);
        model.addAttribute("filterLocation", location);
        model.addAttribute("filterFloor", floor);
        
        return "admin/rooms/meeting-rooms-management";
    }
    
    @PostMapping("/admin/rooms/add")
    public String addRoom(@ModelAttribute MeetingRoom room, RedirectAttributes redirectAttributes) {
        try {
            // Set default values for fields that might be null
            if (room.isHasProjector() == false) room.setHasProjector(false);
            if (room.isHasScreen() == false) room.setHasScreen(false);
            if (room.isHasSpeaker() == false) room.setHasSpeaker(false);
            if (room.isHasComputer() == false) room.setHasComputer(false);
            if (room.isHasWhiteboard() == false) room.setHasWhiteboard(false);
            
            // Set timestamps
            LocalDateTime now = LocalDateTime.now();
            room.setCreatedAt(now);
            room.setUpdatedAt(now);
            
            // Set default type if null
            if (room.getType() == null) {
                room.setType(MeetingRoom.RoomType.LARGE_CLASSROOM);
            }
            
            meetingRoomRepository.save(room);
            redirectAttributes.addAttribute("success", "add");
            return "redirect:/admin/rooms";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "add");
            return "redirect:/admin/rooms";
        }
    }
    
    @PostMapping("/admin/rooms/update")
    public String updateRoom(@ModelAttribute MeetingRoom room, RedirectAttributes redirectAttributes) {
        try {
            // Get the existing room
            MeetingRoom existingRoom = meetingRoomRepository.findById(room.getId())
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + room.getId()));
            
            // Update fields
            existingRoom.setRoomName(room.getRoomName());
            existingRoom.setCapacity(room.getCapacity());
            existingRoom.setLocation(room.getLocation());
            existingRoom.setFloor(room.getFloor());
            existingRoom.setDescription(room.getDescription());
            
            // Handle checkbox values
            existingRoom.setHasProjector(room.isHasProjector());
            existingRoom.setHasScreen(room.isHasScreen());
            existingRoom.setHasSpeaker(room.isHasSpeaker());
            existingRoom.setHasComputer(room.isHasComputer());
            existingRoom.setHasWhiteboard(room.isHasWhiteboard());
            
            // Update timestamp
            existingRoom.setUpdatedAt(LocalDateTime.now());
            
            meetingRoomRepository.save(existingRoom);
            redirectAttributes.addAttribute("success", "update");
            return "redirect:/admin/rooms";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "update");
            return "redirect:/admin/rooms";
        }
    }
    
    @PostMapping("/admin/rooms/delete")
    public String deleteRoom(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            meetingRoomRepository.deleteById(id);
            redirectAttributes.addAttribute("success", "delete");
            return "redirect:/admin/rooms";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "delete");
            return "redirect:/admin/rooms";
        }
    }
    
    @PostMapping("/admin/rooms/update-image")
    public String updateRoomImage(@RequestParam Long id, @RequestParam String imageUrl, RedirectAttributes redirectAttributes) {
        try {
            // Get the existing room
            MeetingRoom existingRoom = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
            
            // Update image URL
            existingRoom.setImageUrl(imageUrl);
            
            // Update timestamp
            existingRoom.setUpdatedAt(LocalDateTime.now());
            
            meetingRoomRepository.save(existingRoom);
            redirectAttributes.addAttribute("success", "image");
            return "redirect:/admin/rooms";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "image");
            return "redirect:/admin/rooms";
        }
    }
    
    @GetMapping("/admin/reservations")
    public String adminReservations() {
        return "redirect:/admin/bookings-dashboard";
    }
    
    // Redirect for user reservations
    @GetMapping("/reservations")
    public String userReservationsRedirect() {
        return "redirect:/user/bookings";
    }
} 