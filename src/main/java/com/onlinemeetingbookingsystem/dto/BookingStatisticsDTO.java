package com.onlinemeetingbookingsystem.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class BookingStatisticsDTO {

    // Getters and Setters
    private Long totalBookings;
    private Long pendingBookings;
    private Long approvedBookings;
    private Long rejectedBookings;
    private Long cancelledBookings;
    
    private Map<String, Long> bookingCountByStatus = new HashMap<>();
    private Map<String, Long> bookingCountByRoom = new HashMap<>();
    private Map<String, Double> roomUtilizationRates = new HashMap<>();
    private Map<LocalDate, Long> bookingCountByDate = new HashMap<>();
    
    // newly added statistical data
    private Map<String, Long> bookingCountByHour = new HashMap<>();
    private Map<String, Double> roomOccupancyRatesByDay = new HashMap<>();
    private Map<String, Double> averageBookingDurationByRoom = new HashMap<>();
    private Map<String, Long> mostUsedRooms = new HashMap<>();
    private Long averageBookingsPerDay;
    private LocalDate mostActiveDay;
    private String formattedMostActiveDay; // add the formatted date string
    private Long mostActiveDayCount;
    private Long averageBookingDuration; // 单位：分钟

}