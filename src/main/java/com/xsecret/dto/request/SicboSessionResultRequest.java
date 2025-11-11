package com.xsecret.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SicboSessionResultRequest {

    @NotBlank(message = "Mã kết quả không được để trống")
    private String resultCode;

    @NotNull(message = "Thiếu thông tin bàn")
    private Integer tableNumber;
}


