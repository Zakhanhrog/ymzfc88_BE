package com.xsecret.dto.request;

import com.xsecret.entity.Banner;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BannerRequest {
    
    @NotBlank(message = "Image URL cannot be empty")
    private String imageUrl;
    
    @NotNull(message = "Banner type cannot be null")
    private Banner.BannerType bannerType;
    
    @NotNull(message = "Display order cannot be null")
    private Integer displayOrder;
    
    @NotNull(message = "Is active status cannot be null")
    private Boolean isActive;
}
