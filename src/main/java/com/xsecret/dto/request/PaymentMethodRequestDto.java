package com.xsecret.dto.request;

import com.xsecret.entity.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentMethodRequestDto {
    
    @NotNull(message = "Payment type is required")
    private PaymentMethod.PaymentType type;
    
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;
    
    @NotBlank(message = "Account number is required")
    @Size(max = 50, message = "Account number cannot exceed 50 characters")
    private String accountNumber;
    
    @NotBlank(message = "Account name is required")
    @Size(max = 100, message = "Account name cannot exceed 100 characters")
    private String accountName;
    
    @Size(max = 20, message = "Bank code cannot exceed 20 characters")
    private String bankCode;
    
    @NotNull(message = "Minimum amount is required")
    @DecimalMin(value = "1000", message = "Minimum amount must be at least 1,000 VND")
    private BigDecimal minAmount;
    
    @NotNull(message = "Maximum amount is required")
    @DecimalMin(value = "1000", message = "Maximum amount must be at least 1,000 VND")
    private BigDecimal maxAmount;
    
    @DecimalMin(value = "0", message = "Fee percent must be non-negative")
    @DecimalMax(value = "100", message = "Fee percent cannot exceed 100%")
    private BigDecimal feePercent;
    
    @DecimalMin(value = "0", message = "Fixed fee must be non-negative")
    private BigDecimal feeFixed;
    
    @Size(max = 50, message = "Processing time cannot exceed 50 characters")
    private String processingTime;
    
    private Boolean isActive = true;
    
    private Integer displayOrder;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    
    @Size(max = 1000, message = "QR code cannot exceed 1000 characters")
    private String qrCode;
}