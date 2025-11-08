package com.xsecret.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class XocDiaSessionResultRequest {

    @NotBlank(message = "Mã kết quả không được để trống")
    private String resultCode;
}


