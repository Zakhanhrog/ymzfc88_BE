package com.xsecret.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentCustomerBetHistoryResponse {
    private List<AgentCustomerBetHistoryItemResponse> items;
    private BigDecimal totalStake;
    private BigDecimal totalWinAmount;
    private BigDecimal totalLostAmount;
    private BigDecimal totalNetResult;
}

