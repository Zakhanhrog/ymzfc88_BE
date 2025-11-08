package com.xsecret.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class XocDiaBetSummaryItemResponse {

    Long betId;
    String code;
    String name;
    Long amount;
    String status;
    Long winAmount;
    Double payoutMultiplier;
    Long createdAt;
}


