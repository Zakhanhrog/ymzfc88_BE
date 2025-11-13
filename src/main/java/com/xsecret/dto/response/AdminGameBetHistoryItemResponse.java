package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminGameBetHistoryItemResponse {

    private Long id;
    private String gameType;
    private Long userId;
    private String username;
    private String fullName;
    private String phoneNumber;

    private String betCode;
    private String description;
    private BigDecimal stakeAmount;
    private BigDecimal potentialWinAmount;
    private BigDecimal winAmount;
    private String status;
    private String resultCode;

    private Long sessionId;
    private Integer tableNumber;

    private Instant createdAt;
    private Instant settledAt;
}

