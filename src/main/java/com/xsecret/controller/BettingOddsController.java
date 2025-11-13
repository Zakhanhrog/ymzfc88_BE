package com.xsecret.controller;

import com.xsecret.dto.request.BettingOddsRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.BettingOddsResponse;
import com.xsecret.service.BettingOddsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cho Admin quản lý tỷ lệ cược
 */
@RestController
@RequestMapping("/admin/betting-odds")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN','STAFF_XNK')")
public class BettingOddsController {

    private final BettingOddsService bettingOddsService;

    /**
     * Lấy tất cả tỷ lệ cược
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BettingOddsResponse>>> getAllBettingOdds() {
        try {
            List<BettingOddsResponse> bettingOdds = bettingOddsService.getAllBettingOdds();
            return ResponseEntity.ok(ApiResponse.success(bettingOdds));
        } catch (Exception e) {
            log.error("Error getting all betting odds", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi khi lấy danh sách tỷ lệ cược: " + e.getMessage()));
        }
    }

    /**
     * Lấy tỷ lệ cược theo region
     */
    @GetMapping("/region/{region}")
    public ResponseEntity<ApiResponse<List<BettingOddsResponse>>> getBettingOddsByRegion(
            @PathVariable String region) {
        try {
            List<BettingOddsResponse> bettingOdds = bettingOddsService.getBettingOddsByRegion(region);
            return ResponseEntity.ok(ApiResponse.success(bettingOdds));
        } catch (Exception e) {
            log.error("Error getting betting odds by region: {}", region, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi khi lấy tỷ lệ cược: " + e.getMessage()));
        }
    }

    /**
     * Lấy tỷ lệ cược theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BettingOddsResponse>> getBettingOddsById(
            @PathVariable Long id) {
        try {
            BettingOddsResponse bettingOdds = bettingOddsService.getBettingOddsById(id);
            return ResponseEntity.ok(ApiResponse.success(bettingOdds));
        } catch (Exception e) {
            log.error("Error getting betting odds by id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi khi lấy tỷ lệ cược: " + e.getMessage()));
        }
    }

    /**
     * Tạo mới tỷ lệ cược
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BettingOddsResponse>> createBettingOdds(
            @Valid @RequestBody BettingOddsRequest request) {
        try {
            BettingOddsResponse bettingOdds = bettingOddsService.createBettingOdds(request);
            return ResponseEntity.ok(ApiResponse.success("Tạo tỷ lệ cược thành công", bettingOdds));
        } catch (Exception e) {
            log.error("Error creating betting odds", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi khi tạo tỷ lệ cược: " + e.getMessage()));
        }
    }

    /**
     * Cập nhật tỷ lệ cược
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BettingOddsResponse>> updateBettingOdds(
            @PathVariable Long id,
            @Valid @RequestBody BettingOddsRequest request) {
        try {
            BettingOddsResponse bettingOdds = bettingOddsService.updateBettingOdds(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật tỷ lệ cược thành công", bettingOdds));
        } catch (Exception e) {
            log.error("Error updating betting odds with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi khi cập nhật tỷ lệ cược: " + e.getMessage()));
        }
    }

    /**
     * Cập nhật hàng loạt tỷ lệ cược
     */
    @PutMapping("/batch")
    public ResponseEntity<ApiResponse<List<BettingOddsResponse>>> batchUpdateBettingOdds(
            @Valid @RequestBody List<BettingOddsRequest> requests) {
        try {
            List<BettingOddsResponse> bettingOdds = bettingOddsService.batchUpdateBettingOdds(requests);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật hàng loạt tỷ lệ cược thành công", bettingOdds));
        } catch (Exception e) {
            log.error("Error batch updating betting odds", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi khi cập nhật hàng loạt tỷ lệ cược: " + e.getMessage()));
        }
    }

    /**
     * Xóa tỷ lệ cược
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBettingOdds(@PathVariable Long id) {
        try {
            bettingOddsService.deleteBettingOdds(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa tỷ lệ cược thành công", null));
        } catch (Exception e) {
            log.error("Error deleting betting odds with id: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi khi xóa tỷ lệ cược: " + e.getMessage()));
        }
    }
}

