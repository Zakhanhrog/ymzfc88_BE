package com.xsecret.controller;

import com.xsecret.dto.request.NotificationRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.NotificationResponse;
import com.xsecret.security.UserPrincipal;
import com.xsecret.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Admin: Tạo thông báo mới
     */
    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> createNotification(
            @Valid @RequestBody NotificationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("Admin {} creating notification", principal.getUsername());
        
        try {
            NotificationResponse notification = notificationService.createNotification(
                    request, 
                    principal.getUsername()
            );
            return ResponseEntity.ok(ApiResponse.success("Gửi thông báo thành công", notification));
        } catch (Exception e) {
            log.error("Error creating notification: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi khi gửi thông báo: " + e.getMessage()));
        }
    }

    /**
     * Admin: Lấy tất cả thông báo
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin fetching all notifications");
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<NotificationResponse> notifications = notificationService.getAllNotifications(pageable);
            return ResponseEntity.ok(ApiResponse.success(notifications));
        } catch (Exception e) {
            log.error("Error fetching notifications: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi khi tải thông báo: " + e.getMessage()));
        }
    }

    /**
     * Admin: Xóa thông báo
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        log.info("Admin deleting notification ID: {}", id);
        
        try {
            notificationService.deleteNotification(id);
            return ResponseEntity.ok(ApiResponse.success("Xóa thông báo thành công", null));
        } catch (Exception e) {
            log.error("Error deleting notification: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi khi xóa thông báo: " + e.getMessage()));
        }
    }

    /**
     * User: Lấy thông báo của mình
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("User {} fetching notifications", principal.getUsername());
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<NotificationResponse> notifications = notificationService.getUserNotifications(
                    principal.getUsername(),
                    pageable
            );
            return ResponseEntity.ok(ApiResponse.success(notifications));
        } catch (Exception e) {
            log.error("Error fetching user notifications: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi khi tải thông báo: " + e.getMessage()));
        }
    }

    /**
     * User: Đếm thông báo chưa đọc
     */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        try {
            long count = notificationService.countUnreadNotifications(principal.getUsername());
            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("Error counting unread notifications: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi khi đếm thông báo: " + e.getMessage()));
        }
    }

    /**
     * User: Đánh dấu đã đọc
     */
    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("User {} marking notification {} as read", principal.getUsername(), id);
        
        try {
            NotificationResponse notification = notificationService.markAsRead(id, principal.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đã đọc", notification));
        } catch (Exception e) {
            log.error("Error marking notification as read: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * User: Đánh dấu tất cả đã đọc
     */
    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("User {} marking all notifications as read", principal.getUsername());
        
        try {
            notificationService.markAllAsRead(principal.getUsername());
            return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả đã đọc", null));
        } catch (Exception e) {
            log.error("Error marking all as read: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }

    /**
     * User: Lấy chi tiết thông báo
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            NotificationResponse notification = notificationService.getNotificationById(id, principal.getUsername());
            return ResponseEntity.ok(ApiResponse.success(notification));
        } catch (Exception e) {
            log.error("Error fetching notification: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Lỗi: " + e.getMessage()));
        }
    }
}

