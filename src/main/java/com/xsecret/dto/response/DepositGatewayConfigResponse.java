package com.xsecret.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class DepositGatewayConfigResponse {
    Long id;
    String bankCode;
    String bankName;
    String channelCode;
    String merchantId;
    String apiKey;
    String notifyUrl;
    String returnUrl;
    Boolean active;
    Integer priorityOrder;
    BigDecimal minAmount;
    BigDecimal maxAmount;
    String description;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}

