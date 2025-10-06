package com.onlinemeetingbookingsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "avatar", length = 255)
    private String avatar = "/images/default-avatar.png";

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role = Role.User;

    @Column(name = "is_locked")
    private boolean isLocked = false;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_password_change_at")
    private LocalDateTime lastPasswordChangeAt;

    @Column(name = "lock_until")
    private LocalDateTime lockUntil;

    @Column(name = "failed_attempts")
    private Integer failedAttempts = 0;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Booking> bookings = new ArrayList<>();
    
    // Transient fields for registration and password management
    @Transient
    private String registerCode; // Registration email verification code
    
    @Transient
    private String forgetPwdCode; // Password reset verification code
    
    @Transient
    private String adminKey; // Admin registration key

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Role {
        Admin, User
    }

}