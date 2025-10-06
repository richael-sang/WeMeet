package com.onlinemeetingbookingsystem.controller;

import com.onlinemeetingbookingsystem.entity.MeetingRoom;
import com.onlinemeetingbookingsystem.repository.MeetingRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestDatabaseController {

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;

    @GetMapping("/rooms")
    public Map<String, Object> testRooms() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<MeetingRoom> rooms = meetingRoomRepository.findAll();
            response.put("success", true);
            response.put("count", rooms.size());
            response.put("rooms", rooms);
            
            // 输出日志以便在控制台查看
            System.out.println("获取到的会议室数量: " + rooms.size());
            for (MeetingRoom room : rooms) {
                System.out.println("房间ID: " + room.getId() + ", 名称: " + room.getRoomName() + 
                    ", 位置: " + room.getLocation() + ", 容量: " + room.getCapacity());
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            e.printStackTrace();
        }
        return response;
    }
    
    @GetMapping("/db-status")
    public Map<String, Object> testDbStatus() {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean isConnected = meetingRoomRepository.count() >= 0;
            response.put("connected", isConnected);
            response.put("message", isConnected ? "数据库连接正常" : "数据库连接异常");
        } catch (Exception e) {
            response.put("connected", false);
            response.put("message", "数据库连接异常: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }
} 