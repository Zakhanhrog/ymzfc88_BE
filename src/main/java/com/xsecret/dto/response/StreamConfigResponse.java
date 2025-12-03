package com.xsecret.dto.response;

import com.xsecret.entity.StreamConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamConfigResponse {
    
    private Long id;
    private StreamConfig.GameType gameType;
    private Integer tableNumber;
    private String streamKey;
    private Boolean isActive;
    private String description;
    private String createdAt;
    private String updatedAt;
    
    public static StreamConfigResponse fromEntity(StreamConfig config) {
        return StreamConfigResponse.builder()
                .id(config.getId())
                .gameType(config.getGameType())
                .tableNumber(config.getTableNumber())
                .streamKey(config.getStreamKey())
                .isActive(config.getIsActive())
                .description(config.getDescription())
                .createdAt(config.getCreatedAt() != null ? config.getCreatedAt().toString() : null)
                .updatedAt(config.getUpdatedAt() != null ? config.getUpdatedAt().toString() : null)
                .build();
    }
}

