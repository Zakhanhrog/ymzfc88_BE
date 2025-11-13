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
public class AdminAgentCommissionReportResponse {

    private String month;
    private int totalAgents;
    private long totalCustomers;
    private BigDecimal totalBetAmount;
    private BigDecimal totalLostAmount;
    private BigDecimal totalCalculatedCommission;
    private BigDecimal totalPaidCommission;
    private BigDecimal totalPendingCommission;
    private List<AdminAgentCommissionReportRowResponse> agents;
}

