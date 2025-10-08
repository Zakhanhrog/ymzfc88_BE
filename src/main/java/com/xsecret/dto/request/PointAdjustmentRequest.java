package com.xsecret.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

@Data
public class PointAdjustmentRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Points is required")
    @Min(value = 1, message = "Points must be greater than 0")
    private Long points;
    
    @NotBlank(message = "Type is required")
    private String type; // ADD or SUBTRACT
    
    @NotBlank(message = "Description is required")
    private String description;
}