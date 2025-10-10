package com.xsecret.dto.response;

import com.xsecret.entity.BettingOdds;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BettingOddsResponse {
    
    private Long id;
    private String region;
    private String betType;
    private String betName;
    private String description;
    private Integer odds;
    private Integer pricePerPoint;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Convert từ Entity sang Response DTO
     */
    public static BettingOddsResponse fromEntity(BettingOdds entity) {
        if (entity == null) {
            return null;
        }
        
        return BettingOddsResponse.builder()
                .id(entity.getId())
                .region(entity.getRegion())
                .betType(entity.getBetType())
                .betName(entity.getBetName())
                .description(entity.getDescription())
                .odds(entity.getOdds())
                .pricePerPoint(entity.getPricePerPoint())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

