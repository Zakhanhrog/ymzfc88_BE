package com.xsecret.controller;

import com.xsecret.dto.request.PointAdjustmentRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.PointTransactionResponse;
import com.xsecret.dto.response.UserPointResponse;
import com.xsecret.entity.User;
import com.xsecret.service.PointService;
import com.xsecret.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/points")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PointController {

    private final PointService pointService;
    private final UserService userService;

    @GetMapping("/my-points")
    public ResponseEntity<ApiResponse<UserPointResponse>> getMyPoints(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            UserPointResponse response = pointService.getUserPoints(user.getId());
            
            return ResponseEntity.ok(ApiResponse.<UserPointResponse>builder()
                    .success(true)
                    .message("Lấy thông tin điểm thành công")
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<UserPointResponse>builder()
                    .success(false)
                    .message("Lỗi khi lấy thông tin điểm: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/my-history")
    public ResponseEntity<ApiResponse<Page<PointTransactionResponse>>> getMyPointHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            Page<PointTransactionResponse> response = pointService.getUserPointHistory(user.getId(), page, size);
            
            return ResponseEntity.ok(ApiResponse.<Page<PointTransactionResponse>>builder()
                    .success(true)
                    .message("Lấy lịch sử điểm thành công")
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Page<PointTransactionResponse>>builder()
                    .success(false)
                    .message("Lỗi khi lấy lịch sử điểm: " + e.getMessage())
                    .build());
        }
    }

    // Admin endpoints
    @PostMapping("/admin/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PointTransactionResponse>> adjustPoints(
            @Valid @RequestBody PointAdjustmentRequest request,
            Authentication authentication) {
        try {
            String adminUsername = authentication.getName();
            User adminUser = userService.getUserByUsername(adminUsername);
            PointTransactionResponse response = pointService.adjustPointsByAdmin(request, adminUser);
            
            return ResponseEntity.ok(ApiResponse.<PointTransactionResponse>builder()
                    .success(true)
                    .message("Điều chỉnh điểm thành công")
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<PointTransactionResponse>builder()
                    .success(false)
                    .message("Lỗi khi điều chỉnh điểm: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/admin/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserPointResponse>> getUserPoints(@PathVariable Long userId) {
        try {
            UserPointResponse response = pointService.getUserPoints(userId);
            
            return ResponseEntity.ok(ApiResponse.<UserPointResponse>builder()
                    .success(true)
                    .message("Lấy thông tin điểm người dùng thành công")
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<UserPointResponse>builder()
                    .success(false)
                    .message("Lỗi khi lấy thông tin điểm: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/admin/user/{userId}/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PointTransactionResponse>>> getUserPointHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<PointTransactionResponse> response = pointService.getUserPointHistory(userId, page, size);
            
            return ResponseEntity.ok(ApiResponse.<Page<PointTransactionResponse>>builder()
                    .success(true)
                    .message("Lấy lịch sử điểm người dùng thành công")
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Page<PointTransactionResponse>>builder()
                    .success(false)
                    .message("Lỗi khi lấy lịch sử điểm: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/admin/all-history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PointTransactionResponse>>> getAllPointHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long userId) {
        try {
            Page<PointTransactionResponse> response;
            
            if (startDate != null && endDate != null) {
                response = pointService.getPointHistoryByDateRange(startDate, endDate, userId, page, size);
            } else {
                response = pointService.getAllPointHistory(page, size);
            }
            
            return ResponseEntity.ok(ApiResponse.<Page<PointTransactionResponse>>builder()
                    .success(true)
                    .message("Lấy lịch sử điểm tổng quan thành công")
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<Page<PointTransactionResponse>>builder()
                    .success(false)
                    .message("Lỗi khi lấy lịch sử điểm: " + e.getMessage())
                    .build());
        }
    }
}