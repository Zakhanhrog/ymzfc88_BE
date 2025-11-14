package com.xsecret.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositGatewayConfigRequest {

    @NotBlank(message = "Vui lòng nhập mã ngân hàng/kênh")
    @Size(max = 50, message = "Mã ngân hàng tối đa 50 ký tự")
    private String bankCode;

    @NotBlank(message = "Vui lòng nhập tên ngân hàng")
    @Size(max = 150, message = "Tên ngân hàng tối đa 150 ký tự")
    private String bankName;

    @NotBlank(message = "Vui lòng nhập mã kênh (code)")
    @Size(max = 100, message = "Mã kênh tối đa 100 ký tự")
    private String channelCode;

    @NotBlank(message = "Vui lòng nhập merchant id")
    @Size(max = 120, message = "Merchant id tối đa 120 ký tự")
    private String merchantId;

    @NotBlank(message = "Vui lòng nhập API key")
    @Size(max = 255, message = "API key tối đa 255 ký tự")
    private String apiKey;

    @Size(max = 255, message = "Notify URL tối đa 255 ký tự")
    private String notifyUrl;

    @Size(max = 255, message = "Return URL tối đa 255 ký tự")
    private String returnUrl;

    private Boolean active;

    private Integer priorityOrder;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String description;
}

