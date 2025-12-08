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
    private BigDecimal totalDepositAmount;
    private BigDecimal totalWithdrawAmount;
    private BigDecimal totalDailyLossRefund; // Tổng hoàn thua
    private BigDecimal totalRefund; // Tổng hoàn cược
    private BigDecimal totalPromotionalMoney; // Tổng KM
    private BigDecimal finalBalance; // Số dư cuối
    private String firstLoginIp;

    private AgentCommissionPayout.Status payoutStatus;
    private BigDecimal paidCommissionAmount;
    private LocalDateTime paidAt;
    private Long payoutId;
    private String payoutNote;
    private BigDecimal customCommissionAmount; // Hoa hồng tự điền

    private double commissionRate;
    private boolean canPayout;
    private BigDecimal pendingDifference;
    private String agentNote; // Ghi chú cho đại lý
}

