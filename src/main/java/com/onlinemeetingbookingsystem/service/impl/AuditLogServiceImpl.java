package com.onlinemeetingbookingsystem.service.impl;

import com.onlinemeetingbookingsystem.entity.AuditLog;
import com.onlinemeetingbookingsystem.entity.User;
import com.onlinemeetingbookingsystem.repository.AuditLogRepository;
import com.onlinemeetingbookingsystem.service.AuditLogService;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 启动新事务进行日志记录
    public void logAdminAction(User admin, String action, User targetUser, String details) {
        if (admin == null || admin.getId() == null || admin.getUsername() == null) {
            log.error("无法记录审计操作：管理员用户信息缺失或不完整。");
            return;
        }
        insertLogEntry(admin.getId(), admin.getUsername(), action, targetUser, details);
    }

    @Override
    public Page<AuditLog> findAllPaginated(Pageable pageable) {
        log.debug("获取分页审计日志，分页参数: {}", pageable);
        return auditLogRepository.findAll(pageable);
    }

    @Override
    public List<AuditLog> findAll() {
        log.debug("获取所有审计日志以供导出，按时间戳降序排序。");
        return auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 启动新事务进行日志记录
    public void logGenericAdminAction(Long adminId, String adminUsername, String action, String details) {
        if (adminId == null || adminUsername == null) {
            log.error("无法记录通用管理员操作：管理员ID或用户名缺失。");
            return;
        }
        insertLogEntry(adminId, adminUsername, action, null, details);
    }

    private void insertLogEntry(Long adminId, String adminUsername, String action, User targetUser, String details) {
        try {
            Long targetUserId = (targetUser != null) ? targetUser.getId() : null;
            String targetUsername = (targetUser != null) ? targetUser.getUsername() : null;

            String truncatedDetails = details;
            if (details != null && details.length() > 1000) {
                log.warn("审计日志详情截断，操作: '{}', 管理员: '{}'. 原始长度: {}", action, adminUsername, details.length());
                truncatedDetails = details.substring(0, 997) + "...";
            }

            AuditLog logEntry = new AuditLog(
                adminId,
                adminUsername,
                action,
                targetUserId,
                targetUsername,
                truncatedDetails
            );

            auditLogRepository.save(logEntry);
            log.debug("审计日志保存成功：管理员={}, 操作={}, 目标用户={}", adminUsername, action, targetUsername);

        } catch (Exception e) {
            log.error("保存审计日志时发生异常: 管理员={}, 操作={}, 目标用户={}, 详情='{}', 错误: {}",
                      adminUsername, action, (targetUser != null ? targetUser.getUsername() : "N/A"), details, e.getMessage(), e);
        }
    }
}