package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.entity.MeetingRoom;
import com.onlinemeetingbookingsystem.repository.MeetingRoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/fix")
public class DataFixController {
    
    private static final Logger logger = LoggerFactory.getLogger(DataFixController.class);

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;

    @GetMapping("/add-sample-rooms")
    public Map<String, Object> addSampleRooms() {
        Map<String, Object> response = new HashMap<>();
        try {
            // Check if rooms already exist to avoid unique constraint violations
            if (meetingRoomRepository.findByRoomName("101") != null) {
                logger.info("Room 101 already exists, skipping creation");
            } else {
                // Create and save sample meeting room 1
                MeetingRoom room1 = new MeetingRoom();
                room1.setRoomName("101");
                room1.setType(MeetingRoom.RoomType.LARGE_CLASSROOM);
                room1.setCapacity(20);
                room1.setLocation("主楼一层");
                room1.setFloor(1);
                room1.setHasProjector(true);
                room1.setHasScreen(true);
                room1.setHasSpeaker(true);
                room1.setHasComputer(false);
                room1.setHasWhiteboard(false);
                room1.setDescription("大型会议室，适合团队讨论和演示");
                // Let @PrePersist handle creation dates
                room1.setAvailable(true);
                
                logger.info("Saving room: {}", room1.getRoomName());
                meetingRoomRepository.save(room1);
            }
            
            if (meetingRoomRepository.findByRoomName("102") != null) {
                logger.info("Room 102 already exists, skipping creation");
            } else {
                // Create and save sample meeting room 2
                MeetingRoom room2 = new MeetingRoom();
                room2.setRoomName("102");
                room2.setType(MeetingRoom.RoomType.LARGE_CLASSROOM);
                room2.setCapacity(30);
                room2.setLocation("主楼一层");
                room2.setFloor(1);
                room2.setHasProjector(true);
                room2.setHasScreen(true);
                room2.setHasSpeaker(true);
                room2.setHasComputer(false);
                room2.setHasWhiteboard(false);
                room2.setDescription("大型演讲厅，适合课程演示和讲座");
                // Let @PrePersist handle creation dates
                room2.setAvailable(true);
                
                logger.info("Saving room: {}", room2.getRoomName());
                meetingRoomRepository.save(room2);
            }
            
            if (meetingRoomRepository.findByRoomName("201") != null) {
                logger.info("Room 201 already exists, skipping creation");
            } else {
                // Create and save sample meeting room 3
                MeetingRoom room3 = new MeetingRoom();
                room3.setRoomName("201");
                room3.setType(MeetingRoom.RoomType.SMALL_CLASSROOM);
                room3.setCapacity(10);
                room3.setLocation("主楼二层");
                room3.setFloor(2);
                room3.setHasProjector(true);
                room3.setHasScreen(true);
                room3.setHasSpeaker(false);
                room3.setHasComputer(false);
                room3.setHasWhiteboard(true);
                room3.setDescription("中型会议室，适合小组讨论");
                // Let @PrePersist handle creation dates
                room3.setAvailable(true);
                
                logger.info("Saving room: {}", room3.getRoomName());
                meetingRoomRepository.save(room3);
            }
            
            response.put("success", true);
            response.put("message", "Sample rooms checked/added successfully");
        } catch (Exception e) {
            logger.error("Error adding sample rooms: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        return response;
    }
} 