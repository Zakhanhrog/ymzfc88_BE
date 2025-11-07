package com.xsecret.dto.response;

import com.xsecret.entity.XocDiaQuickBetConfig;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class XocDiaQuickBetResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private BigDecimal payoutMultiplier;
    private String pattern;
    private String layoutGroup;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static XocDiaQuickBetResponse fromEntity(XocDiaQuickBetConfig entity) {
        return XocDiaQuickBetResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .payoutMultiplier(entity.getPayoutMultiplier())
                .pattern(entity.getPattern())
                .layoutGroup(entity.getLayoutGroup())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}


