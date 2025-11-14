package com.xsecret.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class AdminUserBetDetailResponse {

    Long userId;
    String username;
    String fullName;
    String phoneNumber;
    BigDecimal totalStakeAmount;
    BigDecimal totalWinAmount;
    BigDecimal totalLossAmount;
    BigDecimal totalDepositAmount;
    BigDecimal netProfitAmount;

    List<AdminGameBetHistoryItemResponse> items;
    long totalItems;
    int page;
    int size;
    boolean hasMore;
}


