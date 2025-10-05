package com.xsecret.dto.response;

import com.xsecret.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponseDto {
    
    private Long id;
    private PaymentMethod.PaymentType type;
    private String typeName;
    private String name;
    private String accountNumber;
    private String accountName;
    private String bankCode;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal feePercent;
    private BigDecimal feeFixed;
    private String processingTime;
    private Boolean isActive;
    private Integer displayOrder;
    private String description;
    private String qrCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static PaymentMethodResponseDto fromEntity(PaymentMethod entity) {
        return PaymentMethodResponseDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .typeName(entity.getType().getDisplayName())
                .name(entity.getName())
                .accountNumber(entity.getAccountNumber())
                .accountName(entity.getAccountName())
                .bankCode(entity.getBankCode())
                .minAmount(entity.getMinAmount())
                .maxAmount(entity.getMaxAmount())
                .feePercent(entity.getFeePercent())
                .feeFixed(entity.getFeeFixed())
                .processingTime(entity.getProcessingTime())
                .isActive(entity.getIsActive())
                .displayOrder(entity.getDisplayOrder())
                .description(entity.getDescription())
                .qrCode(entity.getQrCode())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}