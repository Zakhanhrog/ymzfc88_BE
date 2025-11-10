package com.xsecret.controller;

import com.xsecret.dto.request.SicboQuickBetRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.SicboQuickBetResponse;
import com.xsecret.service.SicboQuickBetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/sicbo/quick-bets")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class SicboQuickBetAdminController {

    private final SicboQuickBetService quickBetService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SicboQuickBetResponse>>> getAllConfigs() {
        List<SicboQuickBetResponse> data = quickBetService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SicboQuickBetResponse>> getConfig(@PathVariable Long id) {
        try {
            SicboQuickBetResponse data = quickBetService.getById(id);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (RuntimeException ex) {
            log.error("Lỗi lấy quick bet Sicbo {}", id, ex);
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SicboQuickBetResponse>> createConfig(
            @Valid @RequestBody SicboQuickBetRequest request) {
        try {
            SicboQuickBetResponse data = quickBetService.create(request);
            return ResponseEntity.ok(ApiResponse.success("Tạo cấu hình quick bet thành công", data));
        } catch (RuntimeException ex) {
            log.error("Lỗi tạo quick bet Sicbo", ex);
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thể tạo quick bet: " + ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SicboQuickBetResponse>> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody SicboQuickBetRequest request) {
        try {
            SicboQuickBetResponse data = quickBetService.update(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật quick bet thành công", data));
        } catch (RuntimeException ex) {
            log.error("Lỗi cập nhật quick bet Sicbo {}", id, ex);
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thể cập nhật quick bet: " + ex.getMessage()));
        }
    }

    @PutMapping("/batch")
    public ResponseEntity<ApiResponse<List<SicboQuickBetResponse>>> batchUpdate(
            @Valid @RequestBody List<SicboQuickBetRequest> requests) {
        try {
            List<SicboQuickBetResponse> data = quickBetService.batchUpdate(requests);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật quick bet thành công", data));
        } catch (RuntimeException ex) {
            log.error("Lỗi cập nhật quick bet Sicbo hàng loạt", ex);
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thể cập nhật quick bet: " + ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable Long id) {
        try {
            quickBetService.delete(id);
            return ResponseEntity.ok(ApiResponse.success("Xoá quick bet thành công", null));
        } catch (RuntimeException ex) {
            log.error("Lỗi xoá quick bet Sicbo {}", id, ex);
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thể xoá quick bet: " + ex.getMessage()));
        }
    }
}


