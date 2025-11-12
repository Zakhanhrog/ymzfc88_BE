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
public class TransactionAnalyticsResponse {
    private List<TransactionAnalyticsItemResponse> items;
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
        private BigDecimal totalAmount;
        private BigDecimal totalNetAmount;
        private long totalCount;
    }
}


