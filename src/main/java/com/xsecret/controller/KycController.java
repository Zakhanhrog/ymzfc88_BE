package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.KycApprovalRequest;
import com.xsecret.dto.KycResponse;
import com.xsecret.dto.KycSubmissionRequest;
import com.xsecret.service.KycService;
import com.xsecret.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;
    private final UserService userService;

    /**
     * User submits KYC verification
     */
    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<KycResponse>> submitKyc(
            @RequestParam("frontImage") MultipartFile frontImage,
            @RequestParam("backImage") MultipartFile backImage,
            @RequestParam("idNumber") String idNumber,
            @RequestParam("fullName") String fullName) {
        
        try {
            Long userId = getCurrentUserId();
            
            KycSubmissionRequest request = new KycSubmissionRequest();
            request.setIdNumber(idNumber);
            request.setFullName(fullName);
            
            KycResponse response = kycService.submitKyc(userId, request, frontImage, backImage);
            
            return ResponseEntity.ok(ApiResponse.success("Gửi yêu cầu xác thực thành công", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi upload file: " + e.getMessage()));
        }
    }

    /**
     * User gets their KYC status
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<KycResponse>> getKycStatus() {
        try {
            Long userId = getCurrentUserId();
            KycResponse response = kycService.getUserKycStatus(userId);
            
            if (response == null) {
                return ResponseEntity.ok(ApiResponse.success("Chưa có yêu cầu xác thực", null));
            }
            
            return ResponseEntity.ok(ApiResponse.success("Lấy thông tin xác thực thành công", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Admin gets all KYC requests
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<KycResponse>>> getAllKycRequests() {
        try {
            List<KycResponse> responses = kycService.getAllKycRequests();
            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách xác thực thành công", responses));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Admin gets pending KYC requests
     */
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<KycResponse>>> getPendingKycRequests() {
        try {
            List<KycResponse> responses = kycService.getPendingKycRequests();
            return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chờ duyệt thành công", responses));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Admin approves or rejects KYC request
     */
    @PostMapping("/admin/process")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<KycResponse>> processKyc(@RequestBody KycApprovalRequest request) {
        try {
            Long adminId = getCurrentUserId();
            KycResponse response = kycService.approveKyc(adminId, request);
            
            String message = "approve".equalsIgnoreCase(request.getAction()) 
                    ? "Duyệt xác thực thành công" 
                    : "Từ chối xác thực thành công";
            
            return ResponseEntity.ok(ApiResponse.success(message, response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Helper method to get current user ID from JWT
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.getUserByUsername(username).getId();
    }
}

