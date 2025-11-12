package com.xsecret.dto.response;

import com.xsecret.entity.SicboResultHistory;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SicboResultHistoryResponse {

    private Long sessionId;
    private Integer tableNumber;
    private String resultCode;
    private Integer resultSum;
    private String category;
    private Instant recordedAt;

    public static SicboResultHistoryResponse fromEntity(SicboResultHistory history) {
        return SicboResultHistoryResponse.builder()
                .sessionId(history.getSession() != null ? history.getSession().getId() : null)
                .tableNumber(history.getTableNumber())
                .resultCode(history.getResultCode())
                .resultSum(history.getResultSum())
                .category(history.getCategory() != null ? history.getCategory().name() : null)
                .recordedAt(history.getRecordedAt())
                .build();
    }
}


