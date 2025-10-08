package com.xsecret.controller;

import com.xsecret.dto.request.SystemSettingsRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.SystemSettingsResponse;
import com.xsecret.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/system-settings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class SystemSettingsController {

    private final SystemSettingsService systemSettingsService;

    /**
     * Lấy tất cả settings
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemSettingsResponse>>> getAllSettings() {
        try {
            List<SystemSettingsResponse> settings = systemSettingsService.getAllSettings();
            return ResponseEntity.ok(ApiResponse.success(settings));
        } catch (Exception e) {
            log.error("Error getting all settings", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Lấy settings theo category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<SystemSettingsResponse>>> getSettingsByCategory(
            @PathVariable String category) {
        try {
            List<SystemSettingsResponse> settings = systemSettingsService.getSettingsByCategory(category);
            return ResponseEntity.ok(ApiResponse.success(settings));
        } catch (Exception e) {
            log.error("Error getting settings by category", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Lấy setting theo key
     */
    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<SystemSettingsResponse>> getSettingByKey(
            @PathVariable String key) {
        try {
            SystemSettingsResponse setting = systemSettingsService.getSettingByKey(key);
            return ResponseEntity.ok(ApiResponse.success(setting));
        } catch (Exception e) {
            log.error("Error getting setting by key: {}", key, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Tạo hoặc cập nhật setting
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SystemSettingsResponse>> createOrUpdateSetting(
            @RequestBody SystemSettingsRequest request) {
        try {
            SystemSettingsResponse setting = systemSettingsService.createOrUpdateSetting(request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật cài đặt thành công", setting));
        } catch (Exception e) {
            log.error("Error creating/updating setting", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Xóa setting
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse<Void>> deleteSetting(@PathVariable String key) {
        try {
            systemSettingsService.deleteSetting(key);
            return ResponseEntity.ok(ApiResponse.success("Xóa cài đặt thành công", null));
        } catch (Exception e) {
            log.error("Error deleting setting: {}", key, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Khởi tạo settings mặc định
     */
    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<Void>> initializeDefaultSettings() {
        try {
            systemSettingsService.initializeDefaultSettings();
            return ResponseEntity.ok(ApiResponse.success("Khởi tạo cài đặt mặc định thành công", null));
        } catch (Exception e) {
            log.error("Error initializing default settings", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}

