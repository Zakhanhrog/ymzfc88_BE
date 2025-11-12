package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetAnalyticsResponse {
    private List<BetAnalyticsItemResponse> items;
    private long totalItems;
    private int totalPages;
    private int page;
    private int size;
    private Summary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BigDecimal totalStake;
        private BigDecimal totalWinAmount;
        private BigDecimal totalLostAmount;
    }
}


