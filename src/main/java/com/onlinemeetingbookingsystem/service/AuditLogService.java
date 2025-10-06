package com.onlinemeetingbookingsystem.service;

import org.springframework.data.domain.Page;

import com.onlinemeetingbookingsystem.entity.AuditLog;
import com.onlinemeetingbookingsystem.entity.User;
import java.util.List; // <-- 确保导入 List

import org.springframework.data.domain.Pageable; // 导入 Pageable

public interface AuditLogService {
    void logAdminAction(User admin, String action, User targetUser, String details);
    void logGenericAdminAction(Long adminId, String adminUsername, String action, String details);
    Page<AuditLog> findAllPaginated(Pageable pageable);
    List<AuditLog> findAll();
}
