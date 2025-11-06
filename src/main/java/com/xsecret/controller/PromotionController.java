package com.xsecret.controller;

import com.xsecret.dto.request.PromotionRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.PromotionResponse;
import com.xsecret.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
@Slf4j
public class PromotionController {
    
    private final PromotionService promotionService;
    
    /**
     * Lấy tất cả promotions đang hoạt động (public)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getActivePromotions() {
        try {
            log.info("Getting active promotions - Request received");
            List<PromotionResponse> promotions = promotionService.getActivePromotions();
            log.info("Active promotions retrieved successfully, count: {}", promotions.size());
            return ResponseEntity.ok(ApiResponse.success(promotions));
        } catch (Exception e) {
            log.error("Error getting active promotions", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Lấy promotions đang hoạt động với phân trang (public)
     */
    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<Page<PromotionResponse>>> getActivePromotionsPaged(Pageable pageable) {
        try {
            log.info("Getting active promotions with pagination - Request received");
            Page<PromotionResponse> promotions = promotionService.getActivePromotions(pageable);
            log.info("Active promotions with pagination retrieved successfully, total: {}", promotions.getTotalElements());
            return ResponseEntity.ok(ApiResponse.success(promotions));
        } catch (Exception e) {
            log.error("Error getting active promotions with pagination", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Lấy promotion theo ID (public)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionResponse>> getPromotionById(@PathVariable Long id) {
        try {
            log.info("Getting promotion by ID: {}", id);
            PromotionResponse promotion = promotionService.getPromotionById(id);
            log.info("Promotion retrieved successfully: {}", promotion.getTitle());
            return ResponseEntity.ok(ApiResponse.success(promotion));
        } catch (Exception e) {
            log.error("Error getting promotion by ID: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}

@RestController
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
class AdminPromotionController {
    
    private final PromotionService promotionService;
    
    /**
     * Lấy tất cả promotions (admin)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PromotionResponse>>> getAllPromotions() {
        try {
            log.info("Getting all promotions for admin - Request received");
            List<PromotionResponse> promotions = promotionService.getAllPromotions();
            log.info("All promotions retrieved successfully for admin, count: {}", promotions.size());
            return ResponseEntity.ok(ApiResponse.success(promotions));
        } catch (Exception e) {
            log.error("Error getting all promotions for admin", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Lấy tất cả promotions với phân trang (admin)
     */
    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<Page<PromotionResponse>>> getAllPromotionsPaged(Pageable pageable) {
        try {
            log.info("Getting all promotions with pagination for admin - Request received");
            Page<PromotionResponse> promotions = promotionService.getAllPromotions(pageable);
            log.info("All promotions with pagination retrieved successfully for admin, total: {}", promotions.getTotalElements());
            return ResponseEntity.ok(ApiResponse.success(promotions));
        } catch (Exception e) {
            log.error("Error getting all promotions with pagination for admin", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Tạo promotion mới (admin)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PromotionResponse>> createPromotion(@Valid @RequestBody PromotionRequest request) {
        try {
            log.info("Creating new promotion for admin - Request received: {}", request.getTitle());
            PromotionResponse promotion = promotionService.createPromotion(request);
            log.info("Promotion created successfully for admin with ID: {}", promotion.getId());
            return ResponseEntity.ok(ApiResponse.success("Tạo khuyến mãi thành công", promotion));
        } catch (Exception e) {
            log.error("Error creating promotion for admin", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Cập nhật promotion (admin)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionResponse>> updatePromotion(
            @PathVariable Long id, 
            @Valid @RequestBody PromotionRequest request) {
        try {
            log.info("Updating promotion for admin - Request received for ID: {}, title: {}", id, request.getTitle());
            PromotionResponse promotion = promotionService.updatePromotion(id, request);
            log.info("Promotion updated successfully for admin with ID: {}", promotion.getId());
            return ResponseEntity.ok(ApiResponse.success("Cập nhật khuyến mãi thành công", promotion));
        } catch (Exception e) {
            log.error("Error updating promotion for admin with ID: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Xóa promotion (admin)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePromotion(@PathVariable Long id) {
        try {
            log.info("Deleting promotion for admin - Request received for ID: {}", id);
            promotionService.deletePromotion(id);
            log.info("Promotion deleted successfully for admin with ID: {}", id);
            return ResponseEntity.ok(ApiResponse.success("Xóa khuyến mãi thành công", null));
        } catch (Exception e) {
            log.error("Error deleting promotion for admin with ID: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Toggle trạng thái promotion (admin)
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<PromotionResponse>> togglePromotionStatus(@PathVariable Long id) {
        try {
            log.info("Toggling promotion status for admin - Request received for ID: {}", id);
            PromotionResponse promotion = promotionService.togglePromotionStatus(id);
            log.info("Promotion status toggled successfully for admin with ID: {}, new status: {}", 
                    promotion.getId(), promotion.getIsActive());
            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái khuyến mãi thành công", promotion));
        } catch (Exception e) {
            log.error("Error toggling promotion status for admin with ID: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
