package com.onlinemeetingbookingsystem.dto;

import com.onlinemeetingbookingsystem.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDTO {

    private Long id;
    private Long roomId;
    private String roomName;
    private String roomLocation;
    private Long userId;
    private String username;
    private String userEmail;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private Booking.BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer userSequenceNumber;
    
    // Add the formatted time string
    private String formattedStartTime;
    private String formattedEndTime;
    private String formattedCreatedAt;
} 