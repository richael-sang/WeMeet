package com.onlinemeetingbookingsystem.service;

import com.onlinemeetingbookingsystem.entity.MeetingRoom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MeetingRoomService {
    
    List<MeetingRoom> findAll();
    
    Optional<MeetingRoom> findById(Long id);
    
    MeetingRoom save(MeetingRoom meetingRoom);
    
    void delete(MeetingRoom meetingRoom);
    
    void deleteById(Long id);
    
    List<MeetingRoom> findByAvailableTrue();
    
    List<MeetingRoom> findByCapacityGreaterThanEqual(Integer capacity);
    
    List<MeetingRoom> findAvailableRoomsForTimeSlot(LocalDateTime startTime, LocalDateTime endTime);
    
    boolean isRoomAvailableForTimeSlot(Long roomId, LocalDateTime startTime, LocalDateTime endTime);
    
    boolean isRoomAvailableForTimeSlot(Long roomId, LocalDateTime startTime, LocalDateTime endTime, Long excludeBookingId);
}