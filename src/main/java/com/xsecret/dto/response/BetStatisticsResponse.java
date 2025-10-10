package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetStatisticsResponse {
    
    private long totalBets;
    private long wonBets;
    private long lostBets;
    private double winRate;
    private double totalBetAmount;
    private double totalWinAmount;
    private double netProfit;
}
