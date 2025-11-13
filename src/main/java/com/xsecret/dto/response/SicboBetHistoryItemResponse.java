package com.xsecret.dto.response;

import com.xsecret.entity.SicboBet;
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
public class SicboBetHistoryItemResponse {

    private Long id;
    private Long sessionId;
    private Integer tableNumber;
    private String betCode;
    private BigDecimal stake;
    private BigDecimal payoutMultiplier;
    private String status;
    private BigDecimal winAmount;
    private String resultCode;
    private Instant createdAt;
    private Instant settledAt;

    public static SicboBetHistoryItemResponse fromEntity(SicboBet bet) {
        return SicboBetHistoryItemResponse.builder()
                .id(bet.getId())
                .sessionId(bet.getSession() != null ? bet.getSession().getId() : null)
                .tableNumber(bet.getSession() != null ? bet.getSession().getTableNumber() : null)
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

