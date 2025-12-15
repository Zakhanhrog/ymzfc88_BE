package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentCustomerDetailResponse {
    private Long id;
    private String username;
    private String status;
    private Long currentBalance; // Số dư hiện tại (points)
    private BigDecimal totalDeposit;
    private BigDecimal totalWithdraw;
    private BigDecimal totalBet;
    private BigDecimal totalWin;
    private BigDecimal totalLoss;
    private BigDecimal totalWinLoss; // Tổng Thắng/Thua (totalWin - totalLoss)
    private BigDecimal totalGameRefund;
    private BigDecimal totalDailyLossRefund;
    private BigDecimal totalPromotionalMoney;
}

