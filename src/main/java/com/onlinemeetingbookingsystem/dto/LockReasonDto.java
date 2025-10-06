package com.onlinemeetingbookingsystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: 自动生成 getter, setter等
@NoArgsConstructor
public class LockReasonDto {
    private String reason;
}