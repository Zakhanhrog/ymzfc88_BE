package com.xsecret.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminAgentCommissionPayoutRequest {

    @NotBlank(message = "Tháng quyết toán không được để trống (định dạng YYYY-MM).")
    private String month;

    private String note;
}

