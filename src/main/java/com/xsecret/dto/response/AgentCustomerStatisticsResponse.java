package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentCustomerStatisticsResponse {
    private long totalCustomers;
    private BigDecimal totalDeposit;
    private BigDecimal totalWithdraw;
    private BigDecimal totalBet;
    private BigDecimal totalWin;
    private BigDecimal totalLoss;
    private BigDecimal totalPromotionalMoney;
    private BigDecimal totalGameRefund;
    private BigDecimal totalDailyLossRefund;
    private BigDecimal currentCommission;
    private double commissionRate;
}

