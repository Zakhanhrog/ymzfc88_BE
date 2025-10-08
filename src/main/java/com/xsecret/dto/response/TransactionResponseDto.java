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
    private String accountName;  // Tên chủ tài khoản
    private String bankCode;     // Mã ngân hàng
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
        // Parse account info từ note field nếu có
        String accountName = null;
        String bankCode = null;
        
        if (entity.getNote() != null && entity.getType() == Transaction.TransactionType.WITHDRAW) {
            String note = entity.getNote();
            
            // Parse account name từ "Account: Nguyen Van A - 0123456789"
            if (note.contains("Account: ")) {
                String accountInfo = note.substring(note.indexOf("Account: ") + 9);
                // Lấy phần trước dấu " - "
                int dashIndex = accountInfo.indexOf(" - ");
                if (dashIndex > 0) {
                    accountName = accountInfo.substring(0, dashIndex).trim();
                }
                
                // Parse bank code từ "- VCB" hoặc "- MOMO"
                if (note.contains(" - ") && note.lastIndexOf(" - ") != note.indexOf(" - ")) {
                    String lastPart = note.substring(note.lastIndexOf(" - ") + 3);
                    if (lastPart.contains(" |")) {
                        bankCode = lastPart.substring(0, lastPart.indexOf(" |")).trim();
                    } else {
                        bankCode = lastPart.trim();
                    }
                }
            }
        }
        
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
                .accountName(accountName)
                .bankCode(bankCode)
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