package com.onlinemeetingbookingsystem.repository;

import com.onlinemeetingbookingsystem.entity.BookingRejectLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Reservation Rejection Log Data Access Interface
 */
@Repository
public interface BookingRejectLogRepository extends JpaRepository<BookingRejectLog, Long> {
    
    /**
     * 根据预订ID查找拒绝日志
     * @param bookingId 预订ID
     * @return 拒绝日志列表
     */
    List<BookingRejectLog> findByBookingId(Long bookingId);
} 