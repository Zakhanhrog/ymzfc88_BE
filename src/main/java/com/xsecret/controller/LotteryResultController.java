package com.xsecret.controller;

import com.xsecret.dto.request.LotteryResultRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.LotteryResultResponse;
import com.xsecret.entity.LotteryResult;
import com.xsecret.repository.LotteryResultRepository;
import com.xsecret.service.LotteryResultService;
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
     */
    @PostMapping("/admin/lottery-results/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LotteryResultResponse>> publishResult(@PathVariable Long id) {
        log.info("Admin publishing lottery result ID: {}", id);
        
        try {
            LotteryResultResponse response = lotteryResultService.publishResult(id);
            return ResponseEntity.ok(ApiResponse.success("Công bố kết quả thành công", response));
        } catch (Exception e) {
            log.error("Error publishing lottery result", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi công bố kết quả: " + e.getMessage()));
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
}

