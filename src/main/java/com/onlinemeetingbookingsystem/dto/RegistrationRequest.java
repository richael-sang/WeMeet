package com.onlinemeetingbookingsystem.dto;

import com.onlinemeetingbookingsystem.entity.User.Role; // Import Role enum
import lombok.Data;

@Data // Lombok annotation for getters, setters, toString, etc.
public class RegistrationRequest {
    
    private String username;
    private String password;
    private String email;
    private String registerCode;
    private Role role; // Use the Role enum from User entity
    private String adminKey;
    private String avatarDataUrl; // Field to receive Base64 data

    // Lombok @Data generates necessary getters and setters
}