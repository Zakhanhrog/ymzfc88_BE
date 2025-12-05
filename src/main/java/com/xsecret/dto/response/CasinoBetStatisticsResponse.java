package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasinoBetStatisticsResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private String period; // "TODAY", "YESTERDAY", "LAST_7_DAYS", "LAST_30_DAYS", "THIS_MONTH", "LAST_MONTH"
    
    // Sicbo statistics
    private Long sicboTotalBets;
    private BigDecimal sicboTotalWagered;
    private BigDecimal sicboTotalWon;
    private BigDecimal sicboTotalLost;
    private BigDecimal sicboTotalRefund;
    private BigDecimal sicboNetProfit; // = won - lost + refund
    
    // Xoc Dia statistics
    private Long xocDiaTotalBets;
    private BigDecimal xocDiaTotalWagered;
    private BigDecimal xocDiaTotalWon;
    private BigDecimal xocDiaTotalLost;
    private BigDecimal xocDiaTotalRefund;
    private BigDecimal xocDiaNetProfit;
    
    // Combined statistics
    private Long totalBets;
    private BigDecimal totalWagered;
    private BigDecimal totalWon;
    private BigDecimal totalLost;
    private BigDecimal totalRefund;
    private BigDecimal netProfit;
}
