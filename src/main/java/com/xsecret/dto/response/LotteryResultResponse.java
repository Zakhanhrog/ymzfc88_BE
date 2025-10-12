package com.xsecret.dto.response;

import com.xsecret.entity.LotteryResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO response cho kết quả xổ số
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotteryResultResponse {
    
    private Long id;
    private String region;
    private String province;
    private LocalDate drawDate;
    private String results;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static LotteryResultResponse fromEntity(LotteryResult entity) {
        return LotteryResultResponse.builder()
                .id(entity.getId())
                .region(entity.getRegion())
                .province(entity.getProvince())
                .drawDate(entity.getDrawDate())
                .results(entity.getResults())
                .status(entity.getStatus().name())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

