package com.xsecret.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SicboSessionStartRequest {

    @NotNull(message = "Thiếu thông tin bàn")
    private Integer tableNumber;
}


