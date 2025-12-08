package com.xsecret.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminAgentCommissionPayoutRequest {

    @NotBlank(message = "Tháng quyết toán không được để trống (định dạng YYYY-MM).")
    private String month;

    private String note;
    
    private BigDecimal customCommissionAmount; // Hoa hồng tự điền
}

