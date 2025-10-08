package com.xsecret.dto.request;

import com.xsecret.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String title;
    private String message;
    private Integer priority; // 1=Đỏ, 2=Vàng, 3=Xanh
    private String type; // SYSTEM, MAINTENANCE, PROMOTION, etc.
    private Long targetUserId; // Null = broadcast to all
    private LocalDateTime expiresAt; // Null = không hết hạn
}

