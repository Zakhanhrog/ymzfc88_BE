package com.xsecret.repository.projections;

import com.xsecret.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface AgentCustomerSummaryProjection {
    Long getUserId();
    String getUsername();
    User.UserStatus getStatus();
    LocalDateTime getJoinedAt();
    BigDecimal getTotalBetAmount();
    BigDecimal getTotalLostAmount();
}

