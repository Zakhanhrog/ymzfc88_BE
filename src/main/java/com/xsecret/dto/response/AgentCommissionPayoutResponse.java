package com.xsecret.dto.response;

import com.xsecret.entity.AgentCommissionPayout;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentCommissionPayoutResponse {
    private Long id;
    private String periodMonth;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal totalLostAmount;
    private BigDecimal commissionAmount;
    private AgentCommissionPayout.Status status;
    private LocalDateTime paidAt;
    private String notes;
}

