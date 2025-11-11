package com.xsecret.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(toBuilder = true)
public class SicboBetPlacementResponse {

    private Long sessionId;
    private Integer tableNumber;
    private Long totalStake;
    private Long balanceBefore;
    private Long balanceAfter;
    private List<PlacedBetItem> bets;

    @Data
    @Builder
    public static class PlacedBetItem {
        private String code;
        private String displayName;
        private Long amount;
        private Double payoutMultiplier;
    }
}


