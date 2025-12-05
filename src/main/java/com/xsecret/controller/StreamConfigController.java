package com.xsecret.controller;

import com.xsecret.dto.request.StreamConfigRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.StreamConfigResponse;
import com.xsecret.entity.StreamConfig;
import com.xsecret.service.StreamConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stream-configs")
@RequiredArgsConstructor
@Slf4j
public class StreamConfigController {
    
    private final StreamConfigService streamConfigService;
    
    // Admin Endpoints
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StreamConfigResponse>> createStreamConfig(
            @Valid @RequestBody StreamConfigRequest request) {
        try {
            StreamConfigResponse response = streamConfigService.createStreamConfig(request);
            return ResponseEntity.ok(ApiResponse.success("Tạo stream config thành công", response));
        } catch (IllegalArgumentException e) {
            log.error("Validation error when creating stream config: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating stream config: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi khi tạo stream config: " + e.getMessage()));
        }
    }
    
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<StreamConfigResponse>>> getAllStreamConfigs() {
        List<StreamConfigResponse> configs = streamConfigService.getAllStreamConfigs();
        return ResponseEntity.ok(ApiResponse.success("Stream configs fetched successfully", configs));
    }
    
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StreamConfigResponse>> getStreamConfigById(@PathVariable Long id) {
        StreamConfigResponse response = streamConfigService.getStreamConfigById(id);
        return ResponseEntity.ok(ApiResponse.success("Stream config fetched successfully", response));
    }
    
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StreamConfigResponse>> updateStreamConfig(
            @PathVariable Long id,
            @Valid @RequestBody StreamConfigRequest request) {
        try {
            StreamConfigResponse response = streamConfigService.updateStreamConfig(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật stream config thành công", response));
        } catch (IllegalArgumentException e) {
            log.error("Validation error when updating stream config id {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating stream config id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi khi cập nhật stream config: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStreamConfig(@PathVariable Long id) {
        streamConfigService.deleteStreamConfig(id);
        return ResponseEntity.ok(ApiResponse.success("Stream config deleted successfully", null));
    }
    
    // Public Endpoints (for frontend to get stream URLs)
    @GetMapping("/public/game/{gameType}")
    public ResponseEntity<ApiResponse<StreamConfigResponse>> getStreamConfigByGame(
            @PathVariable StreamConfig.GameType gameType,
            @RequestParam(required = false) Integer tableNumber) {
        StreamConfigResponse response = streamConfigService.getStreamConfigByGameAndTable(gameType, tableNumber);
        return ResponseEntity.ok(ApiResponse.success("Stream config fetched successfully", response));
    }
    
    @GetMapping("/public/active")
    public ResponseEntity<ApiResponse<List<StreamConfigResponse>>> getActiveStreamConfigs() {
        List<StreamConfigResponse> configs = streamConfigService.getActiveStreamConfigs();
        return ResponseEntity.ok(ApiResponse.success("Active stream configs fetched successfully", configs));
    }
    
    @PostMapping("/admin/toggle-live-pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF_XD', 'STAFF_TX1', 'STAFF_TX2')")
    public ResponseEntity<ApiResponse<StreamConfigResponse>> toggleLivePause(
            @RequestParam StreamConfig.GameType gameType,
            @RequestParam(required = false) Integer tableNumber) {
        try {
            StreamConfigResponse response = streamConfigService.toggleLivePause(gameType, tableNumber);
            String message = response.getIsLivePaused() 
                    ? "Đã tạm dừng live stream cho người dùng" 
                    : "Đã tiếp tục live stream cho người dùng";
            return ResponseEntity.ok(ApiResponse.success(message, response));
        } catch (RuntimeException e) {
            log.error("Error toggling live pause: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error toggling live pause: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi khi toggle live pause: " + e.getMessage()));
        }
    }
    
    @PostMapping("/admin/toggle-live-ended")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF_XD', 'STAFF_TX1', 'STAFF_TX2')")
    public ResponseEntity<ApiResponse<StreamConfigResponse>> toggleLiveEnded(
            @RequestParam StreamConfig.GameType gameType,
            @RequestParam(required = false) Integer tableNumber) {
        try {
            StreamConfigResponse response = streamConfigService.toggleLiveEnded(gameType, tableNumber);
            String message = response.getIsLiveEnded() 
                    ? "Đã đánh dấu live stream đã kết thúc cho người dùng" 
                    : "Đã mở lại live stream cho người dùng";
            return ResponseEntity.ok(ApiResponse.success(message, response));
        } catch (RuntimeException e) {
            log.error("Error toggling live ended: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error toggling live ended: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Lỗi khi toggle live ended: " + e.getMessage()));
        }
    }
}

