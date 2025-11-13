package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffMktGameSummaryResponse {
    private long totalBets;
    private BigDecimal totalStakeAmount;
    private BigDecimal totalWinAmount;
}

