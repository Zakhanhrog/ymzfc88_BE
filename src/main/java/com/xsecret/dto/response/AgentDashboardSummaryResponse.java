package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentDashboardSummaryResponse {
    private long totalCustomers;
    private BigDecimal totalBetAmount;
    private BigDecimal totalLostAmount;
    private BigDecimal totalCommissionAmount;
    private double commissionRate;
    private List<AgentCommissionCustomerSummaryResponse> topCustomers;
}

