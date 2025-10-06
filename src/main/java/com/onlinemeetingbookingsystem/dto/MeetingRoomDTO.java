package com.onlinemeetingbookingsystem.dto;

import com.onlinemeetingbookingsystem.entity.MeetingRoom;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for MeetingRoom entity to ensure compatibility with frontend.
 * Handles type conversions between entity and frontend representation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingRoomDTO {
    // Integer ID instead of Long to match frontend expectation
    private int id;
    private String roomName;
    // String type instead of enum
    private String type;
    private int capacity;
    private String location;
    // String floor instead of Integer
    private String floor;
    private boolean hasProjector;
    private boolean hasComputer;
    private boolean hasWhiteboard;
    private boolean hasScreen;
    private boolean hasSpeaker;
    // Named isAvailable to match frontend property
    private boolean isAvailable;
    private String imageUrl;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert entity to DTO
     * @param room The MeetingRoom entity
     * @return MeetingRoomDTO
     */
    public static MeetingRoomDTO fromEntity(MeetingRoom room) {
        if (room == null) return null;
        
        MeetingRoomDTO dto = new MeetingRoomDTO();
        
        // Convert Long to int - note potential data loss for very large IDs
        dto.setId(room.getId().intValue());
        dto.setRoomName(room.getRoomName());
        
        // Convert enum to string
        if (room.getType() != null) {
            dto.setType(room.getType().name());
        }
        
        dto.setCapacity(room.getCapacity());
        dto.setLocation(room.getLocation());
        
        // Convert Integer floor to String
        if (room.getFloor() != null) {
            dto.setFloor(room.getFloor().toString());
        }
        
        dto.setHasProjector(room.isHasProjector());
        dto.setHasComputer(room.isHasComputer());
        dto.setHasWhiteboard(room.isHasWhiteboard());
        dto.setHasScreen(room.isHasScreen());
        dto.setHasSpeaker(room.isHasSpeaker());
        
        // Convert available to isAvailable
        dto.setAvailable(room.isAvailable());
        
        dto.setImageUrl(room.getImageUrl());
        dto.setDescription(room.getDescription());
        dto.setCreatedAt(room.getCreatedAt());
        dto.setUpdatedAt(room.getUpdatedAt());
        
        return dto;
    }
    
    /**
     * Convert DTO to entity
     * @param dto The MeetingRoomDTO
     * @return MeetingRoom entity
     */
    public static MeetingRoom toEntity(MeetingRoomDTO dto) {
        if (dto == null) return null;
        
        MeetingRoom room = new MeetingRoom();
        
        // Don't set ID for new entities - let the database handle it
        if (dto.getId() > 0) {
            room.setId((long) dto.getId());
        }
        
        room.setRoomName(dto.getRoomName());
        
        // Convert string type to enum
        if (dto.getType() != null && !dto.getType().isEmpty()) {
            try {
                room.setType(MeetingRoom.RoomType.valueOf(dto.getType()));
            } catch (IllegalArgumentException e) {
                // Default to LARGE_CLASSROOM if type doesn't match enum
                room.setType(MeetingRoom.RoomType.LARGE_CLASSROOM);
            }
        }
        
        room.setCapacity(dto.getCapacity());
        room.setLocation(dto.getLocation());
        
        // Convert String floor to Integer
        if (dto.getFloor() != null && !dto.getFloor().isEmpty()) {
            try {
                room.setFloor(Integer.parseInt(dto.getFloor()));
            } catch (NumberFormatException e) {
                // Handle non-numeric floor values - leave as null
            }
        }
        
        room.setHasProjector(dto.isHasProjector());
        room.setHasComputer(dto.isHasComputer());
        room.setHasWhiteboard(dto.isHasWhiteboard());
        room.setHasScreen(dto.isHasScreen());
        room.setHasSpeaker(dto.isHasSpeaker());
        
        // Set available from isAvailable
        room.setAvailable(dto.isAvailable());
        
        room.setImageUrl(dto.getImageUrl());
        room.setDescription(dto.getDescription());
        
        // Don't override createdAt for existing entities
        if (room.getCreatedAt() == null) {
            room.setCreatedAt(LocalDateTime.now());
        }
        
        room.setUpdatedAt(LocalDateTime.now());
        
        return room;
    }
    
    /**
     * Convert a list of entities to a list of DTOs
     */
    public static List<MeetingRoomDTO> fromEntities(List<MeetingRoom> rooms) {
        if (rooms == null) return null;
        
        List<MeetingRoomDTO> dtos = new ArrayList<>();
        for (MeetingRoom room : rooms) {
            dtos.add(fromEntity(room));
        }
        return dtos;
    }
    
    // Getter that matches frontend expectation
    public boolean isAvailable() {
        return isAvailable;
    }
    
    // Setter that matches frontend expectation 
    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }
} 