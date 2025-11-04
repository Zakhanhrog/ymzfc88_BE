package com.xsecret.dto.request;

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
public class TelegramConfigRequest {

    @NotBlank(message = "Bot token không được để trống")
    private String botToken;

    @NotBlank(message = "Chat ID không được để trống")
    private String chatId;

    @NotNull(message = "Trạng thái enabled không được để trống")
    private Boolean enabled;

    private String description;
}
