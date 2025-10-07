package com.xsecret.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositRequestDto {
    
    @NotNull(message = "Payment method ID is required")
    private Long paymentMethodId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10000", message = "Minimum deposit amount is 10,000 VND")
    @DecimalMax(value = "100000000", message = "Maximum deposit amount is 100,000,000 VND")
    private BigDecimal amount;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @Size(max = 100, message = "Reference code cannot exceed 100 characters")
    private String referenceCode;
    
    // Base64 image string
    private String billImage;
    
    @Size(max = 255, message = "Bill image name cannot exceed 255 characters")
    private String billImageName;
    
    @Size(max = 500, message = "Bill image URL cannot exceed 500 characters")
    private String billImageUrl;
}