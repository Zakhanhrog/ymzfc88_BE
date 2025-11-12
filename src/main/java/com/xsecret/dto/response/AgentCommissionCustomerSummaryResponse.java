package com.xsecret.dto.response;

import com.xsecret.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentCommissionCustomerSummaryResponse {
    private Long customerId;
    private String username;
    private User.UserStatus status;
    private BigDecimal totalBetAmount;
    private BigDecimal totalLostAmount;
    private BigDecimal commissionAmount;
}

