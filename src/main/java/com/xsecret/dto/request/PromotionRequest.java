package com.xsecret.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequest {
    
    @NotBlank(message = "Title không được để trống")
    @Size(max = 255, message = "Title không được vượt quá 255 ký tự")
    private String title;
    
    @Size(max = 2000, message = "Description không được vượt quá 2000 ký tự")
    private String description;
    
    @Size(max = 500, message = "Image URL không được vượt quá 500 ký tự")
    private String imageUrl;
    
    private Boolean isActive;
    
    private Integer displayOrder;
}
