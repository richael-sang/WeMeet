package com.onlinemeetingbookingsystem.model;

import lombok.Data;

/**
 * 密码修改请求模型
 */
@Data
public class PasswordChangeRequest {
    
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
    
} 