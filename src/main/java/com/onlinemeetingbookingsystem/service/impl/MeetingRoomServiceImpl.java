package com.onlinemeetingbookingsystem.service.impl;

import com.onlinemeetingbookingsystem.entity.Booking;
import com.onlinemeetingbookingsystem.entity.MeetingRoom;
import com.onlinemeetingbookingsystem.repository.BookingRepository;
import com.onlinemeetingbookingsystem.repository.MeetingRoomRepository;
import com.onlinemeetingbookingsystem.service.MeetingRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MeetingRoomServiceImpl implements MeetingRoomService {

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;
    
    @Autowired
    private BookingRepository bookingRepository;

    @Override
    public List<MeetingRoom> findAll() {
        try {
            List<MeetingRoom> allRooms = meetingRoomRepository.findAll();
            System.out.println("MeetingRoomServiceImpl.findAll(): 获取所有会议室 - " + allRooms.size() + " 个房间");
            
            if (allRooms.isEmpty()) {
                System.out.println("警告: 没有找到任何会议室数据。请检查数据库初始化是否正确。");
            } else {
                System.out.println("会议室数据获取成功。详细信息:");
                for (MeetingRoom room : allRooms) {
                    System.out.println("  - ID: " + room.getId() + 
                                      ", 名称: " + room.getRoomName() + 
                                      ", 位置: " + room.getLocation() + 
                                      ", 容量: " + room.getCapacity() +
                                      ", 类型: " + room.getType());
                }
            }
            return allRooms;
        } catch (Exception e) {
            System.err.println("获取会议室数据时发生错误: " + e.getMessage());
            e.printStackTrace();
            // 返回空列表而不是抛出异常，防止页面崩溃
            return List.of();
        }
    }

    @Override
    public Optional<MeetingRoom> findById(Long id) {
        return meetingRoomRepository.findById(id);
    }

    @Override
    public MeetingRoom save(MeetingRoom meetingRoom) {
        return meetingRoomRepository.save(meetingRoom);
    }

    @Override
    public void delete(MeetingRoom meetingRoom) {
        meetingRoomRepository.delete(meetingRoom);
    }

    @Override
    public void deleteById(Long id) {
        meetingRoomRepository.deleteById(id);
    }

    @Override
    public List<MeetingRoom> findByAvailableTrue() {
        // 这里根据具体情况修改，如果MeetingRoom有isAvailable字段就直接使用
        return meetingRoomRepository.findAll().stream()
                .filter(MeetingRoom::isAvailable)
                .collect(Collectors.toList());
    }

    @Override
    public List<MeetingRoom> findByCapacityGreaterThanEqual(Integer capacity) {
        return meetingRoomRepository.findByCapacityGreaterThanEqual(capacity);
    }

    @Override
    public List<MeetingRoom> findAvailableRoomsForTimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
        List<MeetingRoom> allRooms = meetingRoomRepository.findAll();
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(startTime, endTime, Booking.BookingStatus.APPROVED);
        
        // 提取已被预订的会议室ID
        List<Long> bookedRoomIds = overlappingBookings.stream()
                .map(booking -> booking.getMeetingRoom().getId())
                .collect(Collectors.toList());
        
        // 返回未被预订且可用的会议室
        return allRooms.stream()
                .filter(room -> room.isAvailable() && !bookedRoomIds.contains(room.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isRoomAvailableForTimeSlot(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        return isRoomAvailableForTimeSlot(roomId, startTime, endTime, null);
    }

    @Override
    public boolean isRoomAvailableForTimeSlot(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long excludeBookingId) {
        // 检查会议室是否存在且可用
        Optional<MeetingRoom> roomOpt = meetingRoomRepository.findById(roomId);
        if (!roomOpt.isPresent() || !roomOpt.get().isAvailable()) {
            return false;
        }
        
        // 查找与给定时间段重叠的预订
        List<Booking> overlappingBookings = bookingRepository.findByMeetingRoomIdAndOverlappingTimeSlot(roomId, startTime, endTime, Booking.BookingStatus.APPROVED);
        
        // 如果排除某个预订ID，则过滤掉这个预订
        if (excludeBookingId != null) {
            overlappingBookings = overlappingBookings.stream()
                    .filter(booking -> !booking.getId().equals(excludeBookingId))
                    .collect(Collectors.toList());
        }
        
        // 如果没有重叠的预订，则会议室可用
        return overlappingBookings.isEmpty();
    }
} 