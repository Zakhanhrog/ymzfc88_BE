package com.xsecret.controller;

import com.xsecret.dto.request.LotteryResultRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.LotteryResultResponse;
import com.xsecret.entity.LotteryResult;
import com.xsecret.repository.LotteryResultRepository;
import com.xsecret.service.LotteryResultService;
import com.xsecret.entity.Bet;
import com.xsecret.repository.BetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller quản lý kết quả xổ số
 * Admin endpoints: CRUD kết quả
 * Public endpoints: Xem kết quả đã published
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class LotteryResultController {

    private final LotteryResultService lotteryResultService;
    private final LotteryResultRepository lotteryResultRepository;
    private final BetRepository betRepository;

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Admin: Tạo kết quả xổ số mới
     */
    @PostMapping("/admin/lottery-results")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LotteryResultResponse>> createLotteryResult(
            @RequestBody LotteryResultRequest request) {
        log.info("Admin creating lottery result: region={}, province={}, drawDate={}", 
                request.getRegion(), request.getProvince(), request.getDrawDate());
        
        try {
            LotteryResultResponse response = lotteryResultService.createLotteryResult(request);
            return ResponseEntity.ok(ApiResponse.success("Tạo kết quả thành công", response));
        } catch (Exception e) {
            log.error("Error creating lottery result", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi tạo kết quả: " + e.getMessage()));
        }
    }

    /**
     * Admin: Cập nhật kết quả xổ số
     */
    @PutMapping("/admin/lottery-results/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LotteryResultResponse>> updateLotteryResult(
            @PathVariable Long id,
            @RequestBody LotteryResultRequest request) {
        log.info("Admin updating lottery result ID: {}", id);
        
        try {
            LotteryResultResponse response = lotteryResultService.updateLotteryResult(id, request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật kết quả thành công", response));
        } catch (Exception e) {
            log.error("Error updating lottery result", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi cập nhật kết quả: " + e.getMessage()));
        }
    }

    /**
     * Admin: Xóa kết quả xổ số
     */
    @DeleteMapping("/admin/lottery-results/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteLotteryResult(@PathVariable Long id) {
        log.info("Admin deleting lottery result ID: {}", id);
        
        try {
            lotteryResultService.deleteLotteryResult(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa kết quả thành công", null));
        } catch (Exception e) {
            log.error("Error deleting lottery result", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi xóa kết quả: " + e.getMessage()));
        }
    }

    /**
     * Admin: Publish kết quả (chuyển từ DRAFT sang PUBLISHED)
     * Tự động check bet ngay sau khi publish
     */
    @PostMapping("/admin/lottery-results/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LotteryResultResponse>> publishResult(@PathVariable Long id) {
        log.info("Admin publishing lottery result ID: {}", id);

        try {
            LotteryResultResponse response = lotteryResultService.publishResult(id);
            
            // Trigger auto bet check ngay sau khi publish
            try {
                log.info("🚀 Triggering auto bet check after admin publish for date: {}", response.getDrawDate());
                
                // Check bet cho ngày của kết quả được publish
                log.info("🎯 Manual trigger: Checking bets for result date: {}", response.getDrawDate());
                betService.checkBetResultsForDate(response.getDrawDate().toString());
                
                log.info("✅ Auto bet check completed after admin publish");
            } catch (Exception e) {
                log.error("❌ Error during auto bet check after publish: {}", e.getMessage(), e);
                // Không throw exception, chỉ log lỗi
            }

            return ResponseEntity.ok(ApiResponse.success("Công bố kết quả thành công và đã check bet tự động.", response));
        } catch (Exception e) {
            log.error("Error publishing lottery result", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi công bố kết quả: " + e.getMessage()));
        }
    }

    /**
     * Admin: Manual trigger check bet cho ngày cụ thể
     * Dùng để test và debug
     */
    @PostMapping("/admin/lottery-results/check-bets/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> manualCheckBets(@PathVariable String date) {
        log.info("🔧 ADMIN MANUAL TRIGGER - Check bet for date: {}", date);
        
        try {
            log.info("🔍 DEBUG: Starting manual bet check for date: {}", date);
            betService.checkBetResultsForDate(date);
            log.info("✅ DEBUG: Manual bet check completed successfully");
            return ResponseEntity.ok(ApiResponse.success("Manual bet check completed for date: " + date, null));
        } catch (Exception e) {
            log.error("❌ DEBUG: Error during manual bet check for date: {}", date, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi check bet: " + e.getMessage()));
        }
    }
    
    /**
     * Admin: Force check bet cho ngày hôm nay
     * Endpoint đơn giản để test
     */
    @PostMapping("/admin/lottery-results/force-check-today")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> forceCheckToday() {
        String today = java.time.LocalDate.now().toString();
        log.info("🔧 FORCE CHECK TODAY - Date: {}", today);
        
        try {
            betService.checkBetResultsForDate(today);
            return ResponseEntity.ok(ApiResponse.success("Force check today completed: " + today, null));
        } catch (Exception e) {
            log.error("❌ Force check today failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi force check: " + e.getMessage()));
        }
    }
    
    /**
     * Admin: Force check bet cho ngày 2025-10-21 (test)
     */
    @PostMapping("/admin/lottery-results/force-check-2025-10-21")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> forceCheck20251021() {
        String date = "2025-10-21";
        log.info("🔧 FORCE CHECK 2025-10-21 - Date: {}", date);
        
        try {
            betService.checkBetResultsForDate(date);
            return ResponseEntity.ok(ApiResponse.success("Force check 2025-10-21 completed: " + date, null));
        } catch (Exception e) {
            log.error("❌ Force check 2025-10-21 failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi force check: " + e.getMessage()));
        }
    }
    
    /**
     * Admin: Kiểm tra kết quả trong database
     */
    @GetMapping("/admin/lottery-results/check-db/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkDatabaseResults(@PathVariable String date) {
        log.info("🔍 DEBUG: Checking database results for date: {}", date);
        
        try {
            // Tìm tất cả kết quả của ngày
            java.time.LocalDate targetDate = java.time.LocalDate.parse(date);
            List<LotteryResult> allResults = lotteryResultRepository.findByDrawDateBetween(targetDate, targetDate);
            
            Map<String, Object> result = new HashMap<>();
            result.put("date", date);
            result.put("totalResults", allResults.size());
            
            List<Map<String, Object>> resultDetails = new ArrayList<>();
            for (LotteryResult lr : allResults) {
                Map<String, Object> resultInfo = new HashMap<>();
                resultInfo.put("id", lr.getId());
                resultInfo.put("region", lr.getRegion());
                resultInfo.put("province", lr.getProvince());
                resultInfo.put("status", lr.getStatus());
                resultInfo.put("drawDate", lr.getDrawDate());
                resultInfo.put("createdAt", lr.getCreatedAt());
                resultDetails.add(resultInfo);
            }
            result.put("results", resultDetails);
            
            return ResponseEntity.ok(ApiResponse.success("Database results retrieved", result));
        } catch (Exception e) {
            log.error("❌ Error checking database results: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi kiểm tra database: " + e.getMessage()));
        }
    }
    
    /**
     * Admin: Tạo kết quả thủ công cho test
     */
    @PostMapping("/admin/lottery-results/create-manual")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LotteryResultResponse>> createManualResult() {
        log.info("🔧 ADMIN: Creating manual lottery result for testing");
        
        try {
            // Tạo kết quả Miền Bắc cho ngày hôm nay
            String today = java.time.LocalDate.now().toString();
            String sampleResults = "[\"12345\", \"67890\", \"11111\", \"22222\", \"33333\"]";
            
            LotteryResultRequest request = LotteryResultRequest.builder()
                    .region("mienBac")
                    .province(null)
                    .drawDate(today)
                    .results(sampleResults)
                    .status("PUBLISHED") // Tạo trực tiếp với status PUBLISHED
                    .build();
            
            LotteryResultResponse response = lotteryResultService.createLotteryResult(request);
            
            log.info("✅ Manual result created: ID={}, region={}, drawDate={}, status={}", 
                    response.getId(), response.getRegion(), response.getDrawDate(), response.getStatus());
            
            return ResponseEntity.ok(ApiResponse.success("Tạo kết quả thủ công thành công và đã trigger check bet", response));
        } catch (Exception e) {
            log.error("❌ Error creating manual result: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi tạo kết quả thủ công: " + e.getMessage()));
        }
    }
    
    /**
     * Admin: Tạo kết quả cho ngày cụ thể (để fix vấn đề ngày)
     */
    @PostMapping("/admin/lottery-results/create-for-date/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LotteryResultResponse>> createResultForDate(@PathVariable String date) {
        log.info("🔧 ADMIN: Creating lottery result for specific date: {}", date);
        
        try {
            // Validate date format
            java.time.LocalDate.parse(date);
            
            String sampleResults = "[\"12345\", \"67890\", \"11111\", \"22222\", \"33333\"]";
            
            LotteryResultRequest request = LotteryResultRequest.builder()
                    .region("mienBac")
                    .province(null)
                    .drawDate(date)
                    .results(sampleResults)
                    .status("PUBLISHED") // Tạo trực tiếp với status PUBLISHED
                    .build();
            
            LotteryResultResponse response = lotteryResultService.createLotteryResult(request);
            
            log.info("✅ Manual result created for date {}: ID={}, region={}, drawDate={}, status={}", 
                    date, response.getId(), response.getRegion(), response.getDrawDate(), response.getStatus());
            
            return ResponseEntity.ok(ApiResponse.success("Tạo kết quả cho ngày " + date + " thành công và đã trigger check bet", response));
        } catch (Exception e) {
            log.error("❌ Error creating result for date {}: {}", date, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi tạo kết quả cho ngày " + date + ": " + e.getMessage()));
        }
    }
    
    /**
     * Admin: Trigger auto import thủ công (khi API đã có dữ liệu)
     */
    @PostMapping("/admin/lottery-results/trigger-auto-import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerAutoImport() {
        log.info("🔧 ADMIN: Triggering manual auto import");
        
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Trigger auto import Miền Bắc
            try {
                lotteryResultAutoImportService.autoImportMienBac();
                result.put("mienBac", "SUCCESS");
                log.info("✅ Miền Bắc auto import triggered successfully");
            } catch (Exception e) {
                result.put("mienBac", "FAILED: " + e.getMessage());
                log.error("❌ Miền Bắc auto import failed: {}", e.getMessage());
            }
            
            // Trigger auto import Miền Trung Nam
            try {
                lotteryResultAutoImportService.autoImportAllProvinces();
                result.put("provinces", "SUCCESS");
                log.info("✅ Provinces auto import triggered successfully");
            } catch (Exception e) {
                result.put("provinces", "FAILED: " + e.getMessage());
                log.error("❌ Provinces auto import failed: {}", e.getMessage());
            }
            
            return ResponseEntity.ok(ApiResponse.success("Auto import triggered", result));
        } catch (Exception e) {
            log.error("❌ Error triggering auto import: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi trigger auto import: " + e.getMessage()));
        }
    }
    
    /**
     * Admin: Kiểm tra chi tiết kết quả theo ID
     */
    @GetMapping("/admin/lottery-results/debug/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugLotteryResult(@PathVariable Long id) {
        log.info("🔍 DEBUG: Getting lottery result details for ID: {}", id);
        
        try {
            LotteryResult result = lotteryResultRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả với ID: " + id));
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("id", result.getId());
            debugInfo.put("region", result.getRegion());
            debugInfo.put("province", result.getProvince());
            debugInfo.put("drawDate", result.getDrawDate());
            debugInfo.put("drawDateString", result.getDrawDate().toString());
            debugInfo.put("status", result.getStatus());
            debugInfo.put("createdAt", result.getCreatedAt());
            debugInfo.put("updatedAt", result.getUpdatedAt());
            debugInfo.put("results", result.getResults());
            
            // Thêm thông tin timezone
            debugInfo.put("currentTime", java.time.LocalDateTime.now());
            debugInfo.put("currentDate", java.time.LocalDate.now());
            debugInfo.put("timezone", "Asia/Ho_Chi_Minh");
            
            return ResponseEntity.ok(ApiResponse.success("Debug info retrieved", debugInfo));
        } catch (Exception e) {
            log.error("❌ Error getting debug info: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi lấy debug info: " + e.getMessage()));
        }
    }
    
    /**
     * Admin: Kiểm tra trạng thái bet của ngày hôm nay
     * Dùng để debug
     */
    @GetMapping("/admin/lottery-results/bet-status/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBetStatus(@PathVariable String date) {
        log.info("🔍 DEBUG: Getting bet status for date: {}", date);
        
        try {
            // Tìm tất cả bet của ngày
            List<Bet> allBets = betRepository.findPendingBetsToCheck(date);
            
            Map<String, Object> result = new HashMap<>();
            result.put("date", date);
            result.put("totalBets", allBets.size());
            
            List<Map<String, Object>> betDetails = new ArrayList<>();
            for (Bet bet : allBets) {
                Map<String, Object> betInfo = new HashMap<>();
                betInfo.put("id", bet.getId());
                betInfo.put("status", bet.getStatus());
                betInfo.put("region", bet.getRegion());
                betInfo.put("province", bet.getProvince());
                betInfo.put("betType", bet.getBetType());
                betInfo.put("selectedNumbers", bet.getSelectedNumbers());
                betInfo.put("isWin", bet.getIsWin());
                betInfo.put("winAmount", bet.getWinAmount());
                betDetails.add(betInfo);
            }
            result.put("bets", betDetails);
            
            return ResponseEntity.ok(ApiResponse.success("Bet status retrieved", result));
        } catch (Exception e) {
            log.error("❌ Error getting bet status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi lấy trạng thái bet: " + e.getMessage()));
        }
    }

    /**
     * Admin: Trigger auto-import lottery results từ API
     * Import tất cả kết quả còn thiếu (Miền Bắc + các tỉnh)
     */
    @PostMapping("/admin/lottery-results/auto-import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerAutoImport() {
        log.info("Admin triggering auto-import lottery results...");

        try {
            Map<String, Object> result = new HashMap<>();
            int totalImported = 0;
            List<String> errors = new ArrayList<>();

            // Import Miền Bắc
            try {
                autoImportService.autoImportMienBac();
                totalImported++;
                result.put("mienBac", "Success");
            } catch (Exception e) {
                log.error("Error importing Miền Bắc: {}", e.getMessage());
                errors.add("Miền Bắc: " + e.getMessage());
                result.put("mienBac", "Failed: " + e.getMessage());
            }

            // Import các tỉnh theo lịch quay hôm nay (LOGIC MỚI)
            try {
                log.info("🔄 [DEBUG] Triggering auto import for all provinces drawing today...");
                autoImportService.autoImportAllProvinces();
                result.put("provinces", "Success - All provinces for today imported");
                totalImported++;
                log.info("✅ [DEBUG] All provinces auto import completed successfully");
            } catch (Exception e) {
                log.error("❌ [DEBUG] Error importing provinces for today: {}", e.getMessage());
                errors.add("Provinces: " + e.getMessage());
                result.put("provinces", "Failed: " + e.getMessage());
            }

            result.put("totalImported", totalImported);
            result.put("totalProvinces", 2); // Miền Bắc + All Provinces for today
            result.put("errors", errors);

            String message = String.format("Auto-import hoàn thành: %d/2 thành công (Miền Bắc + Tất cả tỉnh quay hôm nay)", 
                    totalImported);

            return ResponseEntity.ok(ApiResponse.success(message, result));
        } catch (Exception e) {
            log.error("Error during auto-import", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi auto-import: " + e.getMessage()));
        }
    }

    /**
     * Admin: Unpublish kết quả (chuyển từ PUBLISHED về DRAFT)
     */
    @PostMapping("/admin/lottery-results/{id}/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LotteryResultResponse>> unpublishResult(@PathVariable Long id) {
        log.info("Admin unpublishing lottery result ID: {}", id);
        
        try {
            LotteryResultResponse response = lotteryResultService.unpublishResult(id);
            return ResponseEntity.ok(ApiResponse.success("Hủy công bố kết quả thành công", response));
        } catch (Exception e) {
            log.error("Error unpublishing lottery result", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi hủy công bố kết quả: " + e.getMessage()));
        }
    }

    /**
     * Admin: Lấy tất cả kết quả (có phân trang)
     */
    @GetMapping("/admin/lottery-results")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<LotteryResultResponse>>> getAllLotteryResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin getting all lottery results: page={}, size={}", page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<LotteryResultResponse> results = lotteryResultService.getAllLotteryResults(pageable);
            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            log.error("Error getting lottery results", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi lấy kết quả: " + e.getMessage()));
        }
    }

    /**
     * Admin: Lấy kết quả theo region
     */
    @GetMapping("/admin/lottery-results/region/{region}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<LotteryResultResponse>>> getLotteryResultsByRegion(
            @PathVariable String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin getting lottery results by region: {}", region);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<LotteryResultResponse> results = lotteryResultService.getLotteryResultsByRegion(region, pageable);
            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            log.error("Error getting lottery results by region", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi lấy kết quả: " + e.getMessage()));
        }
    }

    /**
     * Admin: Lấy kết quả theo region và province
     */
    @GetMapping("/admin/lottery-results/region/{region}/province/{province}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<LotteryResultResponse>>> getLotteryResultsByRegionAndProvince(
            @PathVariable String region,
            @PathVariable String province,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin getting lottery results by region and province: {}, {}", region, province);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<LotteryResultResponse> results = lotteryResultService.getLotteryResultsByRegionAndProvince(
                    region, province, pageable);
            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            log.error("Error getting lottery results by region and province", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi lấy kết quả: " + e.getMessage()));
        }
    }

    /**
     * Admin: Lấy kết quả theo province
     */
    @GetMapping("/admin/lottery-results/province/{province}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<LotteryResultResponse>>> getLotteryResultsByProvince(
            @PathVariable String province,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin getting lottery results by province: {}", province);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<LotteryResultResponse> results = lotteryResultService.getLotteryResultsByProvince(province, pageable);
            return ResponseEntity.ok(ApiResponse.success(results));
        } catch (Exception e) {
            log.error("Error getting lottery results by province", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi lấy kết quả: " + e.getMessage()));
        }
    }

    /**
     * Admin: Lấy kết quả theo ID
     */
    @GetMapping("/admin/lottery-results/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LotteryResultResponse>> getLotteryResult(@PathVariable Long id) {
        log.info("Admin getting lottery result ID: {}", id);
        
        try {
            LotteryResultResponse response = lotteryResultService.getLotteryResult(id);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error getting lottery result", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi lấy kết quả: " + e.getMessage()));
        }
    }


    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Public: Lấy kết quả đã published theo region và drawDate
     */
    @GetMapping("/public/lottery-results/{region}/{drawDate}")
    public ResponseEntity<ApiResponse<LotteryResultResponse>> getPublishedResult(
            @PathVariable String region,
            @PathVariable String drawDate,
            @RequestParam(required = false) String province) {
        log.info("Getting published lottery result: region={}, province={}, drawDate={}", 
                region, province, drawDate);
        
        try {
            LocalDate date = LocalDate.parse(drawDate, DateTimeFormatter.ISO_LOCAL_DATE);
            LotteryResult result = lotteryResultService.getPublishedResultForBetCheck(region, province, date);
            
            if (result == null) {
                return ResponseEntity.ok(ApiResponse.error("Chưa có kết quả cho ngày này"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(LotteryResultResponse.fromEntity(result)));
        } catch (Exception e) {
            log.error("Error getting published lottery result", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi lấy kết quả: " + e.getMessage()));
        }
    }

    /**
     * Debug: Kiểm tra kết quả cho bet check
     */
    @GetMapping("/debug/lottery-results/check")
    public ResponseEntity<ApiResponse<Object>> debugCheckLotteryResult(
            @RequestParam String region,
            @RequestParam(required = false) String province,
            @RequestParam String drawDate) {
        log.info("Debug check lottery result: region={}, province={}, drawDate={}", 
                region, province, drawDate);
        
        try {
            LocalDate date = LocalDate.parse(drawDate, DateTimeFormatter.ISO_LOCAL_DATE);
            LotteryResult result = lotteryResultService.getPublishedResultForBetCheck(region, province, date);
            
            if (result == null) {
                return ResponseEntity.ok(ApiResponse.error("Không tìm thấy kết quả published cho region=" + region + 
                    ", province=" + province + ", drawDate=" + drawDate));
            }
            
            java.util.Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("id", result.getId());
            responseData.put("region", result.getRegion());
            responseData.put("province", result.getProvince());
            responseData.put("drawDate", result.getDrawDate());
            responseData.put("status", result.getStatus());
            responseData.put("results", result.getResults());
            
            return ResponseEntity.ok(ApiResponse.success("Tìm thấy kết quả", responseData));
        } catch (Exception e) {
            log.error("Error debug check lottery result", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi debug: " + e.getMessage()));
        }
    }
    
    /**
     * Debug: Liệt kê tất cả kết quả published
     */
    @GetMapping("/debug/lottery-results/list-published")
    public ResponseEntity<ApiResponse<Object>> listAllPublishedResults() {
        log.info("Debug list all published lottery results");
        
        try {
            // Query direct từ repository
            java.util.List<LotteryResult> allResults = lotteryResultRepository.findByStatusOrderByDrawDateDesc(
                LotteryResult.ResultStatus.PUBLISHED, org.springframework.data.domain.PageRequest.of(0, 100))
                .getContent();
            
            java.util.List<java.util.Map<String, Object>> resultList = allResults.stream()
                .map(result -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", result.getId());
                    map.put("region", result.getRegion());
                    map.put("province", result.getProvince() != null ? result.getProvince() : "null");
                    map.put("drawDate", result.getDrawDate().toString());
                    map.put("status", result.getStatus().toString());
                    map.put("hasResults", result.getResults() != null && !result.getResults().trim().isEmpty());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
            
            java.util.Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("count", resultList.size());
            responseData.put("results", resultList);
            
            return ResponseEntity.ok(ApiResponse.success("Danh sách kết quả published", responseData));
        } catch (Exception e) {
            log.error("Error listing published lottery results", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi list: " + e.getMessage()));
        }
    }

    /**
     * Public: Lấy kết quả mới nhất đã published theo region
     */
    @GetMapping("/public/lottery-results/{region}/latest")
    public ResponseEntity<ApiResponse<LotteryResultResponse>> getLatestPublishedResult(
            @PathVariable String region,
            @RequestParam(required = false) String province) {
        log.info("Getting latest published lottery result: region={}, province={}", region, province);
        
        try {
            LotteryResult result = lotteryResultService.getLatestPublishedResult(region, province);
            
            if (result == null) {
                return ResponseEntity.ok(ApiResponse.error("Chưa có kết quả"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(LotteryResultResponse.fromEntity(result)));
        } catch (Exception e) {
            log.error("Error getting latest published lottery result", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi lấy kết quả: " + e.getMessage()));
        }
    }
    
    // ==================== TEST/DEBUG ENDPOINTS ====================
    
    private final com.xsecret.service.lottery.LotteryResultAutoImportService autoImportService;
    private final com.xsecret.service.BetService betService;
    
    /**
     * Admin: Test import Miền Bắc manually
     */
    @PostMapping("/admin/lottery-results/test-import/mien-bac")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> testImportMienBac() {
        log.info("Admin manually triggering Miền Bắc import test");
        
        try {
            autoImportService.autoImportMienBac();
            return ResponseEntity.ok(ApiResponse.success("Import Miền Bắc thành công", null));
        } catch (Exception e) {
            log.error("Test import Miền Bắc failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi import: " + e.getMessage()));
        }
    }
    
    /**
     * Admin: Test import 1 tỉnh manually
     */
    @PostMapping("/admin/lottery-results/test-import/province/{province}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> testImportProvince(@PathVariable String province) {
        log.info("Admin manually triggering {} import test", province);
        
        try {
            autoImportService.autoImportProvince(province);
            return ResponseEntity.ok(ApiResponse.success("Import " + province + " thành công", null));
        } catch (Exception e) {
            log.error("Test import {} failed", province, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi import: " + e.getMessage()));
        }
    }
    
    /**
     * Admin: Test import tất cả các tỉnh manually
     */
    @PostMapping("/admin/lottery-results/test-import/all-provinces")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> testImportAllProvinces() {
        log.info("Admin manually triggering all provinces import test");
        
        try {
            autoImportService.autoImportAllProvinces();
            return ResponseEntity.ok(ApiResponse.success("Import tất cả tỉnh thành công", null));
        } catch (Exception e) {
            log.error("Test import all provinces failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi import: " + e.getMessage()));
        }
    }
    
    /**
     * Admin: Auto import kết quả cho ngày hôm nay (trigger từ admin panel)
     * Logic: Check xem có kết quả hôm nay chưa, nếu chưa thì import
     */
    @PostMapping("/admin/lottery-results/auto-import-today")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autoImportToday() {
        log.info("🔧 [DEBUG] Admin triggering auto import for today's results");
        
        try {
            Map<String, Object> result = new HashMap<>();
            List<String> imported = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            
            // 1. Import Miền Bắc
            try {
                log.info("🔄 [DEBUG] Importing Miền Bắc for today...");
                autoImportService.autoImportMienBac();
                imported.add("Miền Bắc");
                log.info("✅ [DEBUG] Miền Bắc imported successfully");
            } catch (Exception e) {
                log.error("❌ [DEBUG] Miền Bắc import failed: {}", e.getMessage());
                errors.add("Miền Bắc: " + e.getMessage());
            }
            
            // 2. Import tất cả tỉnh quay hôm nay
            try {
                log.info("🔄 [DEBUG] Importing all provinces drawing today...");
                autoImportService.autoImportAllProvinces();
                imported.add("Tất cả tỉnh quay hôm nay");
                log.info("✅ [DEBUG] All provinces imported successfully");
            } catch (Exception e) {
                log.error("❌ [DEBUG] Provinces import failed: {}", e.getMessage());
                errors.add("Tỉnh: " + e.getMessage());
            }
            
            result.put("imported", imported);
            result.put("errors", errors);
            result.put("success", errors.isEmpty());
            
            String message = String.format("Auto-import hôm nay hoàn thành: %d thành công, %d lỗi", 
                    imported.size(), errors.size());
            
            return ResponseEntity.ok(ApiResponse.success(message, result));
        } catch (Exception e) {
            log.error("❌ [DEBUG] Error during auto import today: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi auto-import hôm nay: " + e.getMessage()));
        }
    }
}

