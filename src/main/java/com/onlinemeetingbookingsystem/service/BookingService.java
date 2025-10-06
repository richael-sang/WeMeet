package com.onlinemeetingbookingsystem.service;

import com.onlinemeetingbookingsystem.dto.BookingDTO;
import com.onlinemeetingbookingsystem.entity.Booking;
import com.onlinemeetingbookingsystem.dto.BookingStatisticsDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BookingService {

    List<BookingDTO> getBookingsByUser(Long userId);

    List<Booking> getAllBookings();
    
    Booking getBookingById(Long id);
    
    Optional<Booking> findById(Long id);
    
    Booking save(Booking booking);
    
    List<Booking> getBookingsByUserId(Long userId);
    
    List<Booking> getBookingsByRoomId(Long roomId);
    
    List<Booking> getBookingsByStatus(Booking.BookingStatus status);
    
    List<Booking> getBookingsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Booking> getBookingsByRoomAndDateRange(Long roomId, LocalDateTime startDate, LocalDateTime endDate);
    
    List<Booking> searchBookings(Long roomId, Booking.BookingStatus status, LocalDateTime startDate, LocalDateTime endDate);
    
    // Statistics methods
    Map<String, Long> getBookingCountByStatus();
    
    Map<String, Long> getBookingCountByRoom();
    
    Map<String, Double> getRoomUtilizationRates();
    
    Map<LocalDate, Long> getBookingCountByDate(LocalDateTime startDate, LocalDateTime endDate);
    
    // 高级统计方法
    Map<String, Long> getBookingCountByHour(LocalDateTime startDate, LocalDateTime endDate);
    
    Map<String, Double> getRoomOccupancyRatesByDay(LocalDateTime startDate, LocalDateTime endDate);
    
    Map<String, Long> getAverageBookingDurationByRoom();
    
    Map<String, Long> getMostUsedRooms(int limit);
    
    Long getAverageBookingsPerDay(LocalDateTime startDate, LocalDateTime endDate);
    
    LocalDate getMostActiveDay(LocalDateTime startDate, LocalDateTime endDate);
    
    Long getAverageBookingDuration();
    
    // 综合统计方法
    BookingStatisticsDTO getComprehensiveStatistics(LocalDateTime startDate, LocalDateTime endDate);
    
    // 更新预订
    Booking updateBooking(Booking booking);
}