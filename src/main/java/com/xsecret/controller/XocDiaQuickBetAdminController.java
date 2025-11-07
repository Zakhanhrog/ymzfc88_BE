package com.xsecret.controller;

import com.xsecret.dto.request.XocDiaQuickBetRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.XocDiaQuickBetResponse;
import com.xsecret.service.XocDiaQuickBetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/xoc-dia/quick-bets")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class XocDiaQuickBetAdminController {

    private final XocDiaQuickBetService quickBetService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<XocDiaQuickBetResponse>>> getAllConfigs() {
        List<XocDiaQuickBetResponse> data = quickBetService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<XocDiaQuickBetResponse>> getConfig(@PathVariable Long id) {
        try {
            XocDiaQuickBetResponse data = quickBetService.getById(id);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (RuntimeException ex) {
            log.error("Lỗi lấy quick bet {}", id, ex);
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<XocDiaQuickBetResponse>> createConfig(
            @Valid @RequestBody XocDiaQuickBetRequest request) {
        try {
            XocDiaQuickBetResponse data = quickBetService.create(request);
            return ResponseEntity.ok(ApiResponse.success("Tạo cấu hình quick bet thành công", data));
        } catch (RuntimeException ex) {
            log.error("Lỗi tạo quick bet", ex);
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thể tạo quick bet: " + ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<XocDiaQuickBetResponse>> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody XocDiaQuickBetRequest request) {
        try {
            XocDiaQuickBetResponse data = quickBetService.update(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật quick bet thành công", data));
        } catch (RuntimeException ex) {
            log.error("Lỗi cập nhật quick bet {}", id, ex);
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thể cập nhật quick bet: " + ex.getMessage()));
        }
    }

    @PutMapping("/batch")
    public ResponseEntity<ApiResponse<List<XocDiaQuickBetResponse>>> batchUpdate(
            @Valid @RequestBody List<XocDiaQuickBetRequest> requests) {
        try {
            List<XocDiaQuickBetResponse> data = quickBetService.batchUpdate(requests);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật quick bet thành công", data));
        } catch (RuntimeException ex) {
            log.error("Lỗi cập nhật quick bet hàng loạt", ex);
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thể cập nhật quick bet: " + ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable Long id) {
        try {
            quickBetService.delete(id);
            return ResponseEntity.ok(ApiResponse.success("Xoá quick bet thành công", null));
        } catch (RuntimeException ex) {
            log.error("Lỗi xoá quick bet {}", id, ex);
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thể xoá quick bet: " + ex.getMessage()));
        }
    }
}


