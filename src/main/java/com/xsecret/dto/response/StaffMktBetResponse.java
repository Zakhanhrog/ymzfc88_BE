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
public class StaffMktBetResponse {
    private Long id;
    private String gameType;
    private String username;
    private String betCode;
    private BigDecimal stakeAmount;
    private BigDecimal winAmount;
    private String status;
    private Instant createdAt;
}

