package com.xsecret.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SicboQuickBetRequest {

    private Long id;

    @NotBlank(message = "Mã quick bet không được để trống")
    private String code;

    @NotBlank(message = "Tên quick bet không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Tỷ lệ trả thưởng không được để trống")
    @DecimalMin(value = "0.01", message = "Tỷ lệ trả thưởng phải lớn hơn 0")
    private BigDecimal payoutMultiplier;

    @NotBlank(message = "Nhóm hiển thị không được để trống")
    private String layoutGroup;

    @NotNull(message = "Thứ tự hiển thị không được để trống")
    @Min(value = 0, message = "Thứ tự hiển thị phải lớn hơn hoặc bằng 0")
    private Integer displayOrder;

    private Boolean isActive;
}


