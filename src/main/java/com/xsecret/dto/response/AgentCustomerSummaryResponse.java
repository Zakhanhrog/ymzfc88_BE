package com.xsecret.dto.response;

import com.xsecret.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentCustomerSummaryResponse {
    private Long id;
    private String username;
    private User.UserStatus status;
    private LocalDateTime joinedAt;
    private BigDecimal totalBetAmount;
    private BigDecimal totalLostAmount;
    private BigDecimal commissionAmount;
}

