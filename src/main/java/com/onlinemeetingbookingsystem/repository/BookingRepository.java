package com.onlinemeetingbookingsystem.repository;

import com.onlinemeetingbookingsystem.entity.Booking;
import com.onlinemeetingbookingsystem.entity.MeetingRoom;
import com.onlinemeetingbookingsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUser(User user);
    
    List<Booking> findByMeetingRoom(MeetingRoom meetingRoom);
    
    List<Booking> findByStatus(Booking.BookingStatus status);
    
    List<Booking> findByMeetingRoomAndStatus(MeetingRoom meetingRoom, Booking.BookingStatus status);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT b FROM Booking b WHERE b.startTime >= :startDate AND b.endTime <= :endDate")
    List<Booking> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT b FROM Booking b WHERE b.meetingRoom.id = :roomId AND b.startTime >= :startDate AND b.endTime <= :endDate")
    List<Booking> findByRoomAndDateRange(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT b FROM Booking b WHERE b.startTime >= :startDate AND b.endTime <= :endDate ORDER BY b.startTime")
    List<Booking> findByDateRangeOrderByStartTime(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status")
    Long countByStatus(@Param("status") Booking.BookingStatus status);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.startTime >= :startDate AND b.endTime <= :endDate")
    long countByStartTimeBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status AND b.startTime >= :startDate AND b.endTime <= :endDate")
    long countByStatusAndStartTimeBetween(@Param("status") Booking.BookingStatus status, 
                                        @Param("startDate") LocalDateTime startDate, 
                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.meetingRoom.id = :roomId")
    Long countByRoomId(@Param("roomId") Long roomId);
    
    @Query("SELECT b FROM Booking b WHERE " +
           "((b.startTime <= :endTime) AND (b.endTime >= :startTime)) AND " +
           "b.status = :status")
    List<Booking> findOverlappingBookings(
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime,
            @Param("status") Booking.BookingStatus status);
    
    @Query("SELECT b FROM Booking b WHERE " +
           "b.meetingRoom.id = :roomId AND " +
           "((b.startTime <= :endTime) AND (b.endTime >= :startTime)) AND " +
           "b.status = :status")
    List<Booking> findByMeetingRoomIdAndOverlappingTimeSlot(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime,
            @Param("status") Booking.BookingStatus status);
} 