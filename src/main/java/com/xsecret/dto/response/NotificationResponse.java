package com.xsecret.dto.response;

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
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String priority; // URGENT, WARNING, INFO
    private Integer priorityLevel; // 1, 2, 3
    private String priorityLabel; // "Khẩn cấp", "Cảnh báo", "Thông thường"
    private String priorityColor; // "#ff4d4f", "#faad14", "#52c41a"
    private String type;
    private Long targetUserId;
    private String targetUsername;
    private Long createdById;
    private String createdByUsername;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public static NotificationResponse fromEntity(Notification notification) {
        NotificationResponse response = NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .priority(notification.getPriority().name())
                .priorityLevel(notification.getPriority().getLevel())
                .priorityLabel(notification.getPriority().getLabel())
                .priorityColor(notification.getPriority().getColor())
                .type(notification.getType().name())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .expiresAt(notification.getExpiresAt())
                .build();

        if (notification.getTargetUser() != null) {
            response.setTargetUserId(notification.getTargetUser().getId());
            response.setTargetUsername(notification.getTargetUser().getUsername());
        }

        if (notification.getCreatedBy() != null) {
            response.setCreatedById(notification.getCreatedBy().getId());
            response.setCreatedByUsername(notification.getCreatedBy().getUsername());
        }

        return response;
    }
}

