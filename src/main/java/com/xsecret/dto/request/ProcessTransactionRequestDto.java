package com.xsecret.dto.request;

import com.xsecret.entity.Transaction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProcessTransactionRequestDto {
    
    @NotNull(message = "Transaction ID is required")
    private Long transactionId;
    
    @NotNull(message = "Action is required")
    private Action action;
    
    @Size(max = 1000, message = "Admin note cannot exceed 1000 characters")
    private String adminNote;
    
    // Chỉ dùng khi approve deposit - số tiền thực tế cộng vào tài khoản
    private BigDecimal actualAmount;
    
    public enum Action {
        APPROVE, REJECT
    }
}