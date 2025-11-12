package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentCommissionSummaryResponse {
    private String month;
    private double commissionRate;
    private BigDecimal totalBetAmount;
    private BigDecimal totalLostAmount;
    private BigDecimal totalCommissionAmount;
    private int totalCustomers;
    private List<AgentCommissionCustomerSummaryResponse> customers;
}

