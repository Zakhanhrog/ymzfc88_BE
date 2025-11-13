package com.xsecret.dto.response;

import com.xsecret.entity.AgentCommissionPayout;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAgentCommissionReportRowResponse {

    private Long agentId;
    private String username;
    private String fullName;
    private String referralCode;
    private long customerCount;

    private BigDecimal totalBetAmount;
    private BigDecimal totalLostAmount;
    private BigDecimal calculatedCommissionAmount;

    private AgentCommissionPayout.Status payoutStatus;
    private BigDecimal paidCommissionAmount;
    private LocalDateTime paidAt;
    private Long payoutId;
    private String payoutNote;

    private double commissionRate;
    private boolean canPayout;
    private BigDecimal pendingDifference;
}

