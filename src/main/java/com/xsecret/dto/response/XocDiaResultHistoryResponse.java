package com.xsecret.dto.response;

import com.xsecret.entity.XocDiaResultHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XocDiaResultHistoryResponse {

    private Long id;
    private Long sessionId;
    private Instant recordedAt;
    private String resultCode;
    private String normalizedResultCode;
    private Integer redCount;
    private Parity parity;

    public static XocDiaResultHistoryResponse fromEntity(XocDiaResultHistory history, int redCount, Parity parity) {
        return XocDiaResultHistoryResponse.builder()
                .id(history.getId())
                .sessionId(history.getSession() != null ? history.getSession().getId() : null)
                .recordedAt(history.getRecordedAt())
                .resultCode(history.getResultCode())
                .normalizedResultCode(history.getNormalizedResultCode())
                .redCount(redCount)
                .parity(parity)
                .build();
    }

    public enum Parity {
        CHAN,
        LE
    }
}


