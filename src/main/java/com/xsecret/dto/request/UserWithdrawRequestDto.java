package com.xsecret.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserWithdrawRequestDto {
    
    @NotNull(message = "User payment method ID is required")
    private Long userPaymentMethodId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10000", message = "Minimum withdraw amount is 10,000 VND")
    @DecimalMax(value = "50000000", message = "Maximum withdraw amount is 50,000,000 VND")
    private BigDecimal amount;
    
    @NotNull(message = "Points is required")
    @Min(value = 1, message = "Minimum points is 1")
    @Max(value = 50000, message = "Maximum points is 50,000")
    private Integer points;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
}