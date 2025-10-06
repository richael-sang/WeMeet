package com.onlinemeetingbookingsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Reservation Rejection Log Entity Class
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "booking_reject_log")
public class BookingRejectLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
} 