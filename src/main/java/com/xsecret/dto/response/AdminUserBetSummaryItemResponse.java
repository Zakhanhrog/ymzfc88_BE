package com.xsecret.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class AdminUserBetSummaryItemResponse {

    Long userId;
    String username;
    String fullName;
    String phoneNumber;
    BigDecimal totalStakeAmount;
    BigDecimal totalWinAmount;
    BigDecimal totalLossAmount;
    BigDecimal totalDepositAmount;
    BigDecimal netProfitAmount;
}


