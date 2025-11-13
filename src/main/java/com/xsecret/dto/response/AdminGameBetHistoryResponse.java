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
public class AdminGameBetHistoryResponse {

    private List<AdminGameBetHistoryItemResponse> items;
    private long totalItems;
    private int totalPages;
    private int page;
    private int size;
    private BigDecimal totalStakeAmount;
    private BigDecimal totalWinAmount;
}

