package com.xsecret.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawRequestDto {
    
    @NotNull(message = "Payment method ID is required")
    private Long paymentMethodId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "50000", message = "Minimum withdraw amount is 50,000 VND")
    @DecimalMax(value = "50000000", message = "Maximum withdraw amount is 50,000,000 VND")
    private BigDecimal amount;
    
    @NotBlank(message = "Account number is required")
    @Size(max = 50, message = "Account number cannot exceed 50 characters")
    private String accountNumber;
    
    @NotBlank(message = "Account name is required")
    @Size(max = 100, message = "Account name cannot exceed 100 characters")
    private String accountName;
    
    @Size(max = 20, message = "Bank code cannot exceed 20 characters")
    private String bankCode;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}