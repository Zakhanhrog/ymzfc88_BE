package com.xsecret.dto.response;

import com.xsecret.entity.TelegramConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramConfigResponse {

    private Long id;
    private String botToken;
    private String chatId;
    private Boolean enabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TelegramConfigResponse fromEntity(TelegramConfig config) {
        return TelegramConfigResponse.builder()
                .id(config.getId())
                .botToken(config.getBotToken())
                .chatId(config.getChatId())
                .enabled(config.getEnabled())
                .description(config.getDescription())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
