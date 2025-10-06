package com.onlinemeetingbookingsystem.service;

import com.onlinemeetingbookingsystem.entity.BookingRejectLog;
import java.util.List;

/**
 * 预订拒绝日志服务接口
 */
public interface BookingRejectLogService {
    
    /**
     * 保存预订拒绝日志
     * @param bookingRejectLog 预订拒绝日志
     * @return 保存后的预订拒绝日志
     */
    BookingRejectLog save(BookingRejectLog bookingRejectLog);
    
    /**
     * 根据预订ID查找拒绝日志
     * @param bookingId 预订ID
     * @return 拒绝日志列表
     */
    List<BookingRejectLog> findByBookingId(Long bookingId);
    
    /**
     * 获取所有拒绝日志
     * @return 所有拒绝日志列表
     */
    List<BookingRejectLog> findAll();
} 