package com.xsecret.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentCustomerBetHistoryItemResponse {
    private Long betId;
    private String gameType;
    private String betCode;
    private BigDecimal stake;
    private BigDecimal winAmount;
    private BigDecimal netResult;
    private String status;
    private LocalDateTime placedAt;
}

