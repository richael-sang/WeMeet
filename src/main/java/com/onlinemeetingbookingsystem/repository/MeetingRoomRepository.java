package com.onlinemeetingbookingsystem.repository;

import com.onlinemeetingbookingsystem.entity.MeetingRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, Long> {

    MeetingRoom findByRoomName(String roomName);
    
    List<MeetingRoom> findByRoomNameContaining(String name);
    
    List<MeetingRoom> findByCapacityGreaterThanEqual(Integer capacity);
    
    List<MeetingRoom> findByLocation(String location);
    
    List<MeetingRoom> findByFloor(Integer floor);
    
    List<MeetingRoom> findByType(MeetingRoom.RoomType type);
    
    @Query("SELECT r FROM MeetingRoom r WHERE " +
            "(:hasProjector = false OR r.hasProjector = :hasProjector) AND " +
            "(:hasScreen = false OR r.hasScreen = :hasScreen) AND " +
            "(:hasSpeaker = false OR r.hasSpeaker = :hasSpeaker) AND " +
            "(:hasComputer = false OR r.hasComputer = :hasComputer) AND " +
            "(:hasWhiteboard = false OR r.hasWhiteboard = :hasWhiteboard)")
    List<MeetingRoom> findByFacilities(
            @Param("hasProjector") boolean hasProjector,
            @Param("hasScreen") boolean hasScreen,
            @Param("hasSpeaker") boolean hasSpeaker,
            @Param("hasComputer") boolean hasComputer,
            @Param("hasWhiteboard") boolean hasWhiteboard);
            
    @Query("SELECT r FROM MeetingRoom r WHERE " +
            "(:capacity IS NULL OR r.capacity >= :capacity) AND " +
            "(:location IS NULL OR r.location = :location) AND " +
            "(:floor IS NULL OR r.floor = :floor) AND " +
            "(:type IS NULL OR r.type = :type) AND " +
            "(:hasProjector = false OR r.hasProjector = :hasProjector) AND " +
            "(:hasScreen = false OR r.hasScreen = :hasScreen) AND " +
            "(:hasSpeaker = false OR r.hasSpeaker = :hasSpeaker) AND " +
            "(:hasComputer = false OR r.hasComputer = :hasComputer) AND " +
            "(:hasWhiteboard = false OR r.hasWhiteboard = :hasWhiteboard)")
    List<MeetingRoom> searchRooms(
            @Param("capacity") Integer capacity,
            @Param("location") String location,
            @Param("floor") Integer floor,
            @Param("type") MeetingRoom.RoomType type,
            @Param("hasProjector") boolean hasProjector,
            @Param("hasScreen") boolean hasScreen,
            @Param("hasSpeaker") boolean hasSpeaker,
            @Param("hasComputer") boolean hasComputer,
            @Param("hasWhiteboard") boolean hasWhiteboard);
} 