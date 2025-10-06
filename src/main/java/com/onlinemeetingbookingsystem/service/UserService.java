package com.onlinemeetingbookingsystem.service;

import com.onlinemeetingbookingsystem.dto.RegistrationRequest;
import com.onlinemeetingbookingsystem.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface UserService {

    User saveUser(User user);
    
    Optional<User> findById(Long id);
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    List<User> findAll();
    
    Page<User> findAll(Pageable pageable);
    
    Page<User> searchUsers(String searchTerm, Pageable pageable);
    
    void lockUser(Long userId, String reason, User admin);
    
    void unlockUser(Long userId, String reason, User admin);
    
    boolean isUserLocked(Long userId);
    
    void incrementFailedAttempts(Long userId);
    
    void resetFailedAttempts(Long userId);
    
    void updateLastLoginTime(Long userId);
    
    void delete(Long id);
    
    // Added method for advanced search with criteria
    Page<User> findUsersByCriteria(String searchTerm, String roleString, String status, Pageable pageable);
    
    // New authentication methods
    
    /**
     * Register a new user with validation
     */
    ResponseEntity<?> register(RegistrationRequest registrationRequest);
    
    /**
     * Authenticate a user and create JWT token
     */
    Map<String, Object> login(String username, String password);
    
    /**
     * Send registration verification code
     */
    ResponseEntity<?> sendRegisterCode(String email);
    
    /**
     * Send password reset verification code
     */
    ResponseEntity<?> sendForgetPasswordCode(String email);
    
    /**
     * Reset password with verification code
     */
    ResponseEntity<?> resetPassword(User user);
} 