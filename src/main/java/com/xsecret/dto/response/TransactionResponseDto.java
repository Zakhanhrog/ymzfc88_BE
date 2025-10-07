package com.xsecret.dto.response;

import com.xsecret.entity.Transaction;
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
public class TransactionResponseDto {
    
    private Long id;
    private String transactionCode;
    private Long userId;
    private String username;
    private Transaction.TransactionType type;
    private String typeName;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal netAmount;
    private Transaction.TransactionStatus status;
    private String statusName;
    private PaymentMethodResponseDto paymentMethod;
    private String methodAccount;
    private String description;
    private String note;
    private String adminNote;
    private String referenceCode;
    private String billImage;
    private String billImageName;
    private String billImageUrl;
    private String processedByUsername;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static TransactionResponseDto fromEntity(Transaction entity) {
        return TransactionResponseDto.builder()
                .id(entity.getId())
                .transactionCode(entity.getTransactionCode())
                .userId(entity.getUser().getId())
                .username(entity.getUser().getUsername())
                .type(entity.getType())
                .typeName(entity.getType().getDisplayName())
                .amount(entity.getAmount())
                .fee(entity.getFee())
                .netAmount(entity.getNetAmount())
                .status(entity.getStatus())
                .statusName(entity.getStatus().getDisplayName())
                .paymentMethod(entity.getPaymentMethod() != null ? 
                    PaymentMethodResponseDto.fromEntity(entity.getPaymentMethod()) : null)
                .methodAccount(entity.getMethodAccount())
                .description(entity.getDescription())
                .note(entity.getNote())
                .adminNote(entity.getAdminNote())
                .referenceCode(entity.getReferenceCode())
                .billImage(entity.getBillImage())
                .billImageName(entity.getBillImageName())
                .billImageUrl(entity.getBillImageUrl())
                .processedByUsername(entity.getProcessedBy() != null ? 
                    entity.getProcessedBy().getUsername() : null)
                .processedAt(entity.getProcessedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}