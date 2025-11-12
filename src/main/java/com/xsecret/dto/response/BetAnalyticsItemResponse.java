package com.xsecret.dto.response;

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
public class BetAnalyticsItemResponse {
    private Long id;
    private String gameType;
    private String username;
    private String betCode;
    private String betType;
    private BigDecimal stake;
    private BigDecimal winAmount;
    private BigDecimal revenue;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;
}


