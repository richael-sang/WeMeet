package com.onlinemeetingbookingsystem.service.impl;

import com.onlinemeetingbookingsystem.entity.Notification;
import com.onlinemeetingbookingsystem.repository.NotificationRepository;
import com.onlinemeetingbookingsystem.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;

/**
 * Notification Service Implementation Class
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Checks if a user has any unread notifications.
     * IMPORTANT: Requires existsByUserIdAndIsReadFalse(Long userId) method in NotificationRepository interface.
     *
     * @param userId The ID of the user.
     * @return true if the user has unread notifications, false otherwise.
     */
    @Override
    public boolean hasUnreadNotifications(Long userId) {
        // Ensure the repository method exists. Adjust method name if needed.
        // You MUST add boolean existsByUserIdAndIsReadFalse(Long userId); to NotificationRepository.java
        return notificationRepository.existsByUserIdAndIsReadFalse(userId);
    }

    @Override
    public Notification markAsRead(Long notificationId) {
        // Correct use of orElseThrow with Supplier
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found with id: " + notificationId));

        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }
} 