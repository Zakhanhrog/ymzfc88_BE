package com.xsecret.dto.response;

import com.xsecret.entity.XocDiaBet;
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
public class XocDiaBetHistoryItemResponse {

    private Long id;
    private Long sessionId;
    private String betCode;
    private BigDecimal stake;
    private BigDecimal payoutMultiplier;
    private String status;
    private BigDecimal winAmount;
    private String resultCode;
    private Instant createdAt;
    private Instant settledAt;

    public static XocDiaBetHistoryItemResponse fromEntity(XocDiaBet bet) {
        return XocDiaBetHistoryItemResponse.builder()
                .id(bet.getId())
                .sessionId(bet.getSession() != null ? bet.getSession().getId() : null)
                .betCode(bet.getBetCode())
                .stake(bet.getStake())
                .payoutMultiplier(bet.getPayoutMultiplier())
                .status(bet.getStatus() != null ? bet.getStatus().name() : null)
                .winAmount(bet.getWinAmount())
                .resultCode(bet.getResultCode())
                .createdAt(bet.getCreatedAt())
                .settledAt(bet.getSettledAt())
                .build();
    }
}


