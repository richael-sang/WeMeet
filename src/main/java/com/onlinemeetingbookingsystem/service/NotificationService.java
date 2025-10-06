package com.onlinemeetingbookingsystem.service;

import com.onlinemeetingbookingsystem.entity.Notification;
import java.util.List;

/**
 * Notification Service Interface
 */
public interface NotificationService {
    
    /**
     * 保存通知
     * @param notification 通知实体
     * @return 保存后的通知实体
     */
    Notification save(Notification notification);
    
    /**
     * 获取用户的所有通知
     * @param userId 用户ID
     * @return 通知列表
     */
    List<Notification> getNotificationsByUserId(Long userId);
    
    /**
     * 检查用户是否有未读通知。
     * @param userId 用户ID
     * @return 如果有未读通知则返回 true，否则返回 false。
     */
    boolean hasUnreadNotifications(Long userId);
    
    /**
     * 标记通知为已读
     * @param notificationId 通知ID
     * @return 更新后的通知
     */
    Notification markAsRead(Long notificationId);
}