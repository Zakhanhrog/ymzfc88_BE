package com.xsecret.dto.response;

import com.xsecret.entity.SystemSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingsResponse {
    private Long id;
    private String settingKey;
    private String settingValue;
    private String description;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SystemSettingsResponse fromEntity(SystemSettings settings) {
        return SystemSettingsResponse.builder()
                .id(settings.getId())
                .settingKey(settings.getSettingKey())
                .settingValue(settings.getSettingValue())
                .description(settings.getDescription())
                .category(settings.getCategory())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}

