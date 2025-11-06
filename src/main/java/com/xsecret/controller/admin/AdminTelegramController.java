package com.xsecret.controller.admin;

import com.xsecret.dto.request.TelegramConfigRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.TelegramConfigResponse;
import com.xsecret.service.TelegramConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/telegram")
@RequiredArgsConstructor
@Slf4j
public class AdminTelegramController {

    private final TelegramConfigService telegramConfigService;

    @PostMapping("/config")
    public ResponseEntity<ApiResponse<TelegramConfigResponse>> createConfig(
            @Valid @RequestBody TelegramConfigRequest request) {
        try {
            TelegramConfigResponse response = telegramConfigService.createConfig(request);
            return ResponseEntity.ok(ApiResponse.success("Tạo cấu hình Telegram thành công", response));
        } catch (Exception e) {
            log.error("Failed to create Telegram config", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Không thể tạo cấu hình Telegram: " + e.getMessage()));
        }
    }

    @PutMapping("/config/{id}")
    public ResponseEntity<ApiResponse<TelegramConfigResponse>> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody TelegramConfigRequest request) {
        try {
            TelegramConfigResponse response = telegramConfigService.updateConfig(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình Telegram thành công", response));
        } catch (Exception e) {
            log.error("Failed to update Telegram config with ID: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Không thể cập nhật cấu hình Telegram: " + e.getMessage()));
        }
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<List<TelegramConfigResponse>>> getAllConfigs() {
        try {
            List<TelegramConfigResponse> configs = telegramConfigService.getAllConfigs();
            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cấu hình Telegram thành công", configs));
        } catch (Exception e) {
            log.error("Failed to get Telegram configs", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Không thể lấy danh sách cấu hình Telegram: " + e.getMessage()));
        }
    }

    @GetMapping("/config/active")
    public ResponseEntity<ApiResponse<TelegramConfigResponse>> getActiveConfig() {
        try {
            return telegramConfigService.getActiveConfig()
                    .map(config -> ResponseEntity.ok(ApiResponse.success("Lấy cấu hình Telegram đang hoạt động thành công", config)))
                    .orElse(ResponseEntity.ok(ApiResponse.success("Không có cấu hình Telegram đang hoạt động", null)));
        } catch (Exception e) {
            log.error("Failed to get active Telegram config", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Không thể lấy cấu hình Telegram đang hoạt động: " + e.getMessage()));
        }
    }

    @DeleteMapping("/config/{id}")
    public ResponseEntity<ApiResponse<String>> deleteConfig(@PathVariable Long id) {
        try {
            telegramConfigService.deleteConfig(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa cấu hình Telegram thành công", "Config deleted"));
        } catch (Exception e) {
            log.error("Failed to delete Telegram config with ID: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Không thể xóa cấu hình Telegram: " + e.getMessage()));
        }
    }
}
