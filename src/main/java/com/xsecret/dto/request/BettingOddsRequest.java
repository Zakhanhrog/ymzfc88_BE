package com.xsecret.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BettingOddsRequest {
    
    @NotBlank(message = "Region không được để trống")
    private String region;
    
    @NotBlank(message = "Bet type không được để trống")
    private String betType;
    
    @NotBlank(message = "Tên loại cược không được để trống")
    private String betName;
    
    private String description;
    
    @NotNull(message = "Tỷ lệ cược không được để trống")
    @Min(value = 1, message = "Tỷ lệ cược phải lớn hơn 0")
    private Integer odds;
    
    @NotNull(message = "Đơn giá không được để trống")
    @Min(value = 1, message = "Đơn giá phải lớn hơn 0")
    private Integer pricePerPoint;
    
    private Boolean isActive;
}

