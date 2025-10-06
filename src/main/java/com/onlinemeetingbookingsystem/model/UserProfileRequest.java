package com.onlinemeetingbookingsystem.model;

import lombok.Data;

/**
 * 用户资料更新请求模型
 */
@Data
public class UserProfileRequest {
    
    private String name;
    private String email;
    private String phone;
    private String department;
    private String position;
    
} 