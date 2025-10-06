package com.onlinemeetingbookingsystem.repository;

import com.onlinemeetingbookingsystem.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 通知数据访问接口
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * 根据用户ID查找通知，并按创建时间降序排序
     * @param userId 用户ID
     * @return 通知列表
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 统计用户未读通知数量
     * @param userId 用户ID
     * @return 未读通知数量
     */
    Long countByUserIdAndIsReadFalse(Long userId);

    boolean existsByUserIdAndIsReadFalse(Long userId);
} 