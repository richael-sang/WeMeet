package com.onlinemeetingbookingsystem.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "admin_id")
    private Long adminId;
    
    @Column(name = "admin_username")
    private String adminUsername;
    
    @Column(name = "target_user_id")
    private Long targetUserId;
    
    @Column(name = "target_username")
    private String targetUsername;
    
    @Column(name = "action")
    private String action; // e.g., "LOCK_USER", "UNLOCK_USER"
    
    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // Reason, explanation
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @Transient // not persisted to the database
    private String formattedDate;
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
    
    public AuditLog(Long adminId, String adminUsername, String action, Long targetUserId, String targetUsername, String details) {
        this.adminId = adminId;
        this.adminUsername = adminUsername;
        this.action = action;
        this.targetUserId = targetUserId;
        this.targetUsername = targetUsername;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }
} 