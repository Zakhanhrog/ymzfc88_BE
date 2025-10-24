package com.xsecret.dto.response;

import com.xsecret.entity.Banner;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BannerResponse {
    private Long id;
    private String imageUrl;
    private Banner.BannerType bannerType;
    private Integer displayOrder;
    private Boolean isActive;
    private String createdAt;
    private String updatedAt;
}
