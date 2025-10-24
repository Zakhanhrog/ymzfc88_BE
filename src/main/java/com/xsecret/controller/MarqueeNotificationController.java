package com.xsecret.controller;

import com.xsecret.dto.request.MarqueeNotificationRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.MarqueeNotificationResponse;
import com.xsecret.service.MarqueeNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Controller quản lý marquee notification
 * Admin endpoints: CRUD marquee notification
 * Public endpoints: Lấy danh sách marquee notification đang hoạt động
 */
@RestController
@RequestMapping("/marquee-notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class MarqueeNotificationController {

    private final MarqueeNotificationService marqueeNotificationService;

    // ==================== ADMIN ENDPOINTS ====================

    /**
     * Admin: Tạo mới marquee notification
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MarqueeNotificationResponse>> createMarqueeNotification(
            @Valid @RequestBody MarqueeNotificationRequest request,
            @RequestHeader("X-Username") String username) {
        
        log.info("Admin {} creating marquee notification", username);
        
        MarqueeNotificationResponse response = marqueeNotificationService.createMarqueeNotification(request, username);
        
        return ResponseEntity.ok(ApiResponse.success("Tạo marquee notification thành công", response));
    }

    /**
     * Admin: Cập nhật marquee notification
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MarqueeNotificationResponse>> updateMarqueeNotification(
            @PathVariable Long id,
            @Valid @RequestBody MarqueeNotificationRequest request,
            @RequestHeader("X-Username") String username) {
        
        log.info("Admin {} updating marquee notification with ID: {}", username, id);
        
        MarqueeNotificationResponse response = marqueeNotificationService.updateMarqueeNotification(id, request, username);
        
        return ResponseEntity.ok(ApiResponse.success("Cập nhật marquee notification thành công", response));
    }

    /**
     * Admin: Xóa marquee notification
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMarqueeNotification(@PathVariable Long id) {
        
        log.info("Deleting marquee notification with ID: {}", id);
        
        marqueeNotificationService.deleteMarqueeNotification(id);
        
        return ResponseEntity.ok(ApiResponse.success("Xóa marquee notification thành công", null));
    }

    /**
     * Admin: Lấy danh sách marquee notification với phân trang
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<MarqueeNotificationResponse>>> getAllMarqueeNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        
        log.info("Getting marquee notifications - page: {}, size: {}, keyword: {}", page, size, keyword);
        
        Page<MarqueeNotificationResponse> response = marqueeNotificationService.getAllMarqueeNotifications(page, size, keyword);
        
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách marquee notification thành công", response));
    }

    /**
     * Admin: Lấy marquee notification theo ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MarqueeNotificationResponse>> getMarqueeNotificationById(@PathVariable Long id) {
        
        log.info("Getting marquee notification by ID: {}", id);
        
        MarqueeNotificationResponse response = marqueeNotificationService.getMarqueeNotificationById(id);
        
        return ResponseEntity.ok(ApiResponse.success("Lấy marquee notification thành công", response));
    }

    /**
     * Admin: Thay đổi trạng thái hoạt động
     */
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MarqueeNotificationResponse>> toggleActiveStatus(
            @PathVariable Long id,
            @RequestHeader("X-Username") String username) {
        
        log.info("Admin {} toggling active status for marquee notification with ID: {}", username, id);
        
        MarqueeNotificationResponse response = marqueeNotificationService.toggleActiveStatus(id, username);
        
        return ResponseEntity.ok(ApiResponse.success("Thay đổi trạng thái thành công", response));
    }

    /**
     * Admin: Cập nhật thứ tự hiển thị
     */
    @PatchMapping("/{id}/display-order")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MarqueeNotificationResponse>> updateDisplayOrder(
            @PathVariable Long id,
            @RequestParam Integer displayOrder,
            @RequestHeader("X-Username") String username) {
        
        log.info("Admin {} updating display order for marquee notification with ID: {} to order: {}", username, id, displayOrder);
        
        MarqueeNotificationResponse response = marqueeNotificationService.updateDisplayOrder(id, displayOrder, username);
        
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thứ tự hiển thị thành công", response));
    }

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Public: Lấy danh sách marquee notification đang hoạt động
     */
    @GetMapping("/public/active")
    public ResponseEntity<ApiResponse<List<MarqueeNotificationResponse>>> getActiveMarqueeNotifications() {
        
        log.info("Getting active marquee notifications for public");
        
        List<MarqueeNotificationResponse> response = marqueeNotificationService.getActiveMarqueeNotifications();
        
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách marquee notification đang hoạt động thành công", response));
    }
}
