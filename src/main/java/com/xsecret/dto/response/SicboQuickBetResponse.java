package com.xsecret.dto.response;

import com.xsecret.entity.SicboQuickBetConfig;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SicboQuickBetResponse {

    private Long id;
    private String code;
    private String name;
    private String description;
    private BigDecimal payoutMultiplier;
    private String layoutGroup;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SicboQuickBetResponse fromEntity(SicboQuickBetConfig entity) {
        return SicboQuickBetResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .payoutMultiplier(entity.getPayoutMultiplier())
                .layoutGroup(entity.getLayoutGroup())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}


