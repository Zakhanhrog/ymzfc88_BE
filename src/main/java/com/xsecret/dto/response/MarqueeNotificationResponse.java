package com.xsecret.dto.response;

import com.xsecret.entity.MarqueeNotification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarqueeNotificationResponse {
    
    private Long id;
    private String content;
    private Boolean isActive;
    private Integer displayOrder;
    private String textColor;
    private String backgroundColor;
    private Integer fontSize;
    private Integer speed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    public static MarqueeNotificationResponse fromEntity(MarqueeNotification entity) {
        return MarqueeNotificationResponse.builder()
                .id(entity.getId())
                .content(entity.getContent())
                .isActive(entity.getIsActive())
                .displayOrder(entity.getDisplayOrder())
                .textColor(entity.getTextColor())
                .backgroundColor(entity.getBackgroundColor())
                .fontSize(entity.getFontSize())
                .speed(entity.getSpeed())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
