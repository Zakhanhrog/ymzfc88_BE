package com.xsecret.dto.request;

import com.xsecret.entity.StreamConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamConfigRequest {
    
    @NotNull(message = "Game type is required")
    private StreamConfig.GameType gameType;
    
    private Integer tableNumber; // null for games without tables
    
    @NotBlank(message = "Stream key is required")
    private String streamKey;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private String description;
}

