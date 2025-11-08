package com.xsecret.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class XocDiaBetPlacementResponse {

    Long sessionId;
    Long totalStake;
    Long balanceBefore;
    Long balanceAfter;
    List<PlacedBetItem> bets;

    @Value
    @Builder
    public static class PlacedBetItem {
        String code;
        Long amount;
        String displayName;
        Double payoutMultiplier;
    }
}


