package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.dto.MeetingRoomDTO;
import com.onlinemeetingbookingsystem.entity.MeetingRoom;
import com.onlinemeetingbookingsystem.repository.MeetingRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = {"http://localhost:8080", "http://localhost:9090"}, allowCredentials = "true")
public class MeetingRoomApiController {

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;

    @GetMapping
    public ResponseEntity<List<MeetingRoomDTO>> getAllRooms() {
        List<MeetingRoom> rooms = meetingRoomRepository.findAll();
        List<MeetingRoomDTO> roomDTOs = MeetingRoomDTO.fromEntities(rooms);
        return ResponseEntity.ok(roomDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeetingRoomDTO> getRoomById(@PathVariable Long id) {
        Optional<MeetingRoom> room = meetingRoomRepository.findById(id);
        return room.map(r -> ResponseEntity.ok(MeetingRoomDTO.fromEntity(r)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<String> createRoom(@RequestBody MeetingRoomDTO roomDTO) {
        try {
            // 验证必填字段
            if (roomDTO.getRoomName() == null || roomDTO.getRoomName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Room name cannot be empty");
            }
            
            if (roomDTO.getCapacity() <= 0) {
                return ResponseEntity.badRequest().body("Capacity must be greater than 0");
            }
            
            // 检查名称是否已存在
            MeetingRoom existingRoom = meetingRoomRepository.findByRoomName(roomDTO.getRoomName());
            if (existingRoom != null) {
                return ResponseEntity.badRequest().body("Room name already exists");
            }
            
            // 转换DTO为实体
            MeetingRoom room = MeetingRoomDTO.toEntity(roomDTO);
            
            // 设置创建和更新时间
            LocalDateTime now = LocalDateTime.now();
            room.setCreatedAt(now);
            room.setUpdatedAt(now);
            
            // 确保设备属性不为null
            setDefaultEquipmentValues(room);
            
            meetingRoomRepository.save(room);
            return ResponseEntity.ok("Room created successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create room: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateRoom(@PathVariable Long id, @RequestBody MeetingRoomDTO roomDTO) {
        try {
            // 验证必填字段
            if (roomDTO.getRoomName() == null || roomDTO.getRoomName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Room name cannot be empty");
            }
            
            if (roomDTO.getCapacity() <= 0) {
                return ResponseEntity.badRequest().body("Capacity must be greater than 0");
            }
            
            // 检查会议室是否存在
            Optional<MeetingRoom> existingRoomOpt = meetingRoomRepository.findById(id);
            if (!existingRoomOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Room not found");
            }
            
            MeetingRoom existingRoom = existingRoomOpt.get();
            
            // 如果修改了名称，检查新名称是否已存在
            if (!existingRoom.getRoomName().equals(roomDTO.getRoomName())) {
                MeetingRoom roomWithSameName = meetingRoomRepository.findByRoomName(roomDTO.getRoomName());
                if (roomWithSameName != null && !roomWithSameName.getId().equals(id)) {
                    return ResponseEntity.badRequest().body("Room name already exists");
                }
            }
            
            // 转换DTO为实体，但保留ID和创建时间
            MeetingRoom room = MeetingRoomDTO.toEntity(roomDTO);
            room.setId(id);
            room.setCreatedAt(existingRoom.getCreatedAt());
            room.setUpdatedAt(LocalDateTime.now());
            
            // 保留关联的预订列表
            room.setBookings(existingRoom.getBookings());
            
            // 确保设备属性不为null
            setDefaultEquipmentValues(room);
            
            meetingRoomRepository.save(room);
            return ResponseEntity.ok("Room updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update room: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRoom(@PathVariable Long id) {
        try {
            // 检查会议室是否存在
            if (!meetingRoomRepository.existsById(id)) {
                return ResponseEntity.badRequest().body("Room not found");
            }
            
            // 这里可以添加检查是否有关联的预订
            // 如果有关联的预订，可以返回错误信息或者在删除会议室的同时删除关联的预订
            
            meetingRoomRepository.deleteById(id);
            return ResponseEntity.ok("Room deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete room: " + e.getMessage());
        }
    }
    
    /**
     * 设置设备属性的默认值，防止null值
     */
    private void setDefaultEquipmentValues(MeetingRoom room) {
        if (room.isHasProjector() == false) room.setHasProjector(false);
        if (room.isHasScreen() == false) room.setHasScreen(false);
        if (room.isHasSpeaker() == false) room.setHasSpeaker(false);
        
        // 新增的设备属性
        if (room.isHasComputer() == false) room.setHasComputer(false);
        if (room.isHasWhiteboard() == false) room.setHasWhiteboard(false);
    }

    // 高级搜索API端点
    @GetMapping("/search")
    public ResponseEntity<List<MeetingRoomDTO>> searchRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String floor,
            @RequestParam(required = false) Boolean hasProjector,
            @RequestParam(required = false) Boolean hasWhiteboard,
            @RequestParam(required = false) Boolean hasComputer,
            @RequestParam(required = false) Boolean hasScreen,
            @RequestParam(required = false) Boolean hasSpeaker,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isAvailable) {
        
        // 获取所有会议室
        List<MeetingRoom> allRooms = meetingRoomRepository.findAll();
        
        // 应用过滤条件
        List<MeetingRoom> filteredRooms = allRooms.stream()
            .filter(room -> {
                // 关键词过滤
                if (keyword != null && !keyword.isEmpty()) {
                    String lowerKeyword = keyword.toLowerCase();
                    boolean matchesName = room.getRoomName() != null && 
                                         room.getRoomName().toLowerCase().contains(lowerKeyword);
                    boolean matchesLocation = room.getLocation() != null && 
                                             room.getLocation().toLowerCase().contains(lowerKeyword);
                    boolean matchesDescription = room.getDescription() != null && 
                                                room.getDescription().toLowerCase().contains(lowerKeyword);
                    
                    if (!(matchesName || matchesLocation || matchesDescription)) {
                        return false;
                    }
                }
                
                // 容量过滤
                if (capacity != null && (room.getCapacity() == null || room.getCapacity() < capacity)) {
                    return false;
                }
                
                // 建筑物过滤
                if (building != null && !building.isEmpty() && 
                    (room.getLocation() == null || !room.getLocation().contains(building))) {
                    return false;
                }
                
                // 楼层过滤 - 支持String类型比较
                if (floor != null && !floor.isEmpty()) {
                    if (room.getFloor() == null) {
                        return false;
                    }
                    String roomFloor = room.getFloor().toString();
                    if (!roomFloor.equals(floor)) {
                        return false;
                    }
                }
                
                // 设备过滤
                if (hasProjector != null && hasProjector && !room.isHasProjector()) return false;
                if (hasWhiteboard != null && hasWhiteboard && !room.isHasWhiteboard()) return false;
                if (hasComputer != null && hasComputer && !room.isHasComputer()) return false;
                if (hasScreen != null && hasScreen && !room.isHasScreen()) return false;
                if (hasSpeaker != null && hasSpeaker && !room.isHasSpeaker()) return false;
                
                // 可用性过滤
                if (isAvailable != null && isAvailable && !room.isAvailable()) return false;
                
                // 类型过滤
                if (type != null && !type.isEmpty()) {
                    try {
                        MeetingRoom.RoomType roomType = MeetingRoom.RoomType.valueOf(type);
                        if (room.getType() != roomType) return false;
                    } catch (IllegalArgumentException e) {
                        // 忽略无效的类型值
                    }
                }
                
                return true;
            })
            .collect(Collectors.toList());
        
        // 转换过滤后的实体为DTO
        List<MeetingRoomDTO> filteredRoomDTOs = MeetingRoomDTO.fromEntities(filteredRooms);
        
        return ResponseEntity.ok(filteredRoomDTOs);
    }
    
    // 按名称搜索会议室
    @GetMapping("/searchByName")
    public ResponseEntity<List<MeetingRoomDTO>> searchRoomsByName(
            @RequestParam(required = false) String name) {
        
        System.out.println("SearchByName API called with name: [" + name + "]");
        
        List<MeetingRoom> rooms;
        if (name == null || name.isEmpty()) {
            System.out.println("Name is empty, returning all rooms");
            rooms = meetingRoomRepository.findAll();
        } else {
            rooms = meetingRoomRepository.findByRoomNameContaining(name);
        }
        
        // 转换实体为DTO
        List<MeetingRoomDTO> roomDTOs = MeetingRoomDTO.fromEntities(rooms);
        
        System.out.println("Found " + roomDTOs.size() + " rooms matching name: " + name);
        return ResponseEntity.ok(roomDTOs);
    }
} 