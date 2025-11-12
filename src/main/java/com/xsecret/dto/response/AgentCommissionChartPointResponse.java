package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentCommissionChartPointResponse {
    private LocalDate date;
    private BigDecimal totalBetAmount;
    private BigDecimal totalLostAmount;
    private BigDecimal commissionAmount;
}

