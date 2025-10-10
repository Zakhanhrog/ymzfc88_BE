package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.BettingOddsResponse;
import com.xsecret.service.BettingOddsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller public cho user lấy tỷ lệ cược
 */
@RestController
@RequestMapping("/api/betting-odds")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class PublicBettingOddsController {

    private final BettingOddsService bettingOddsService;

    /**
     * Lấy tất cả tỷ lệ cược đang active
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BettingOddsResponse>>> getAllActiveBettingOdds() {
        try {
            List<BettingOddsResponse> bettingOdds = bettingOddsService.getAllBettingOdds();
            return ResponseEntity.ok(ApiResponse.success(bettingOdds));
        } catch (Exception e) {
            log.error("Error getting all active betting odds", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi khi lấy danh sách tỷ lệ cược: " + e.getMessage()));
        }
    }

    /**
     * Lấy tỷ lệ cược đang active theo region (cho user)
     */
    @GetMapping("/region/{region}")
    public ResponseEntity<ApiResponse<List<BettingOddsResponse>>> getActiveBettingOddsByRegion(
            @PathVariable String region) {
        try {
            List<BettingOddsResponse> bettingOdds = bettingOddsService.getActiveBettingOddsByRegion(region);
            return ResponseEntity.ok(ApiResponse.success(bettingOdds));
        } catch (Exception e) {
            log.error("Error getting active betting odds by region: {}", region, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi khi lấy tỷ lệ cược: " + e.getMessage()));
        }
    }
}

