package com.xsecret.dto.response;

import com.xsecret.entity.PaymentMethod;
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
        PaymentMethodResponseDto paymentMethodDto = null;
        
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
                
                // Parse bank code và type từ note
                // Format: "Account: ... - ... (BANK) - VCB | Points: ..." hoặc "Account: ... - ... (E_WALLET) - MOMO | Points: ..."
                if (note.contains("(") && note.contains(")")) {
                    int typeStart = note.indexOf("(");
                    int typeEnd = note.indexOf(")");
                    if (typeStart < typeEnd) {
                        String typeStr = note.substring(typeStart + 1, typeEnd).trim();
                        try {
                            PaymentMethod.PaymentType paymentType = PaymentMethod.PaymentType.valueOf(typeStr);
                            
                            // Parse bank code nếu có
                            if (note.contains(" - ") && note.lastIndexOf(" - ") != note.indexOf(" - ")) {
                                String afterType = note.substring(typeEnd + 1);
                                if (afterType.contains(" - ")) {
                                    String lastPart = afterType.substring(afterType.indexOf(" - ") + 3);
                                    if (lastPart.contains(" |")) {
                                        bankCode = lastPart.substring(0, lastPart.indexOf(" |")).trim();
                                    } else {
                                        bankCode = lastPart.trim();
                                    }
                                }
                            }
                            
                            // Tạo PaymentMethodResponseDto từ thông tin đã parse
                            // Nếu paymentMethod null (trường hợp dùng UserPaymentMethod), tạo DTO từ note
                            if (entity.getPaymentMethod() == null) {
                                String methodName = paymentType.getDisplayName();
                                if (PaymentMethod.PaymentType.BANK.equals(paymentType) && bankCode != null) {
                                    methodName = "Ngân hàng " + bankCode;
                                }
                                
                                paymentMethodDto = PaymentMethodResponseDto.builder()
                                        .type(paymentType)
                                        .typeName(paymentType.getDisplayName())
                                        .name(methodName)
                                        .accountNumber(entity.getMethodAccount())
                                        .accountName(accountName)
                                        .bankCode(bankCode)
                                        .build();
                            }
                        } catch (IllegalArgumentException e) {
                            // Type không hợp lệ, bỏ qua
                        }
                    }
                } else {
                    // Fallback: Parse bank code từ format cũ
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
        }
        
        // Nếu paymentMethod đã có từ entity, dùng nó; nếu không, dùng paymentMethodDto đã parse từ note
        if (entity.getPaymentMethod() != null) {
            paymentMethodDto = PaymentMethodResponseDto.fromEntity(entity.getPaymentMethod());
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
                .paymentMethod(paymentMethodDto)
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