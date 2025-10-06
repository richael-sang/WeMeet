package com.onlinemeetingbookingsystem.repository;

import com.onlinemeetingbookingsystem.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findByAdminId(Long adminId);
    
    List<AuditLog> findByTargetUserId(Long targetUserId);
    
    List<AuditLog> findByAction(String action);
    
    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    List<AuditLog> findTop50ByOrderByTimestampDesc();
} 