package com.onlinemeetingbookingsystem.service.impl;

import com.onlinemeetingbookingsystem.entity.BookingRejectLog;
import com.onlinemeetingbookingsystem.repository.BookingRejectLogRepository;
import com.onlinemeetingbookingsystem.service.BookingRejectLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Reservation Rejection Log Service Implementation Class
 */
@Service
public class BookingRejectLogServiceImpl implements BookingRejectLogService {

    @Autowired
    private BookingRejectLogRepository bookingRejectLogRepository;

    @Override
    public BookingRejectLog save(BookingRejectLog bookingRejectLog) {
        return bookingRejectLogRepository.save(bookingRejectLog);
    }

    @Override
    public List<BookingRejectLog> findByBookingId(Long bookingId) {
        return bookingRejectLogRepository.findByBookingId(bookingId);
    }
    
    @Override
    public List<BookingRejectLog> findAll() {
        return bookingRejectLogRepository.findAll();
    }
} 