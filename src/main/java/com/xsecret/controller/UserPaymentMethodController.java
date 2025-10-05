package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.UserPaymentMethodRequestDto;
import com.xsecret.dto.UserPaymentMethodResponseDto;
import com.xsecret.entity.User;
import com.xsecret.service.UserPaymentMethodService;
import com.xsecret.security.UserPrincipal;
import com.xsecret.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/user/payment-methods")
@RequiredArgsConstructor
@Slf4j
public class UserPaymentMethodController {
    
    private final UserPaymentMethodService userPaymentMethodService;
    private final UserService userService;
    
    /**
     * Lấy tất cả phương thức thanh toán của user hiện tại
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserPaymentMethodResponseDto>>> getUserPaymentMethods(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        log.info("User {} getting payment methods", userPrincipal.getId());
        
        User user = userService.getUserById(userPrincipal.getId());
        List<UserPaymentMethodResponseDto> paymentMethods = userPaymentMethodService.getUserPaymentMethods(user);
        
        return ResponseEntity.ok(ApiResponse.success(paymentMethods));
    }
    
    /**
     * Tạo phương thức thanh toán mới
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserPaymentMethodResponseDto>> createUserPaymentMethod(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UserPaymentMethodRequestDto requestDto) {
        
        log.info("User {} creating new payment method", userPrincipal.getId());
        
        User user = userService.getUserById(userPrincipal.getId());
        UserPaymentMethodResponseDto paymentMethod = userPaymentMethodService.createUserPaymentMethod(user, requestDto);
        
        return ResponseEntity.ok(ApiResponse.success("Tạo phương thức thanh toán thành công", paymentMethod));
    }
    
    /**
     * Cập nhật phương thức thanh toán
     */
    @PutMapping("/{paymentMethodId}")
    public ResponseEntity<ApiResponse<UserPaymentMethodResponseDto>> updateUserPaymentMethod(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long paymentMethodId,
            @Valid @RequestBody UserPaymentMethodRequestDto requestDto) {
        
        log.info("User {} updating payment method {}", userPrincipal.getId(), paymentMethodId);
        
        User user = userService.getUserById(userPrincipal.getId());
        UserPaymentMethodResponseDto paymentMethod = userPaymentMethodService.updateUserPaymentMethod(user, paymentMethodId, requestDto);
        
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phương thức thanh toán thành công", paymentMethod));
    }
    
    /**
     * Xóa phương thức thanh toán
     */
    @DeleteMapping("/{paymentMethodId}")
    public ResponseEntity<ApiResponse<Void>> deleteUserPaymentMethod(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long paymentMethodId) {
        
        log.info("User {} deleting payment method {}", userPrincipal.getId(), paymentMethodId);
        
        User user = userService.getUserById(userPrincipal.getId());
        userPaymentMethodService.deleteUserPaymentMethod(user, paymentMethodId);
        
        return ResponseEntity.ok(ApiResponse.success("Xóa phương thức thanh toán thành công", null));
    }
    
    /**
     * Đặt phương thức thanh toán làm mặc định
     */
    @PutMapping("/{paymentMethodId}/set-default")
    public ResponseEntity<ApiResponse<UserPaymentMethodResponseDto>> setDefaultPaymentMethod(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long paymentMethodId) {
        
        log.info("User {} setting payment method {} as default", userPrincipal.getId(), paymentMethodId);
        
        User user = userService.getUserById(userPrincipal.getId());
        UserPaymentMethodResponseDto paymentMethod = userPaymentMethodService.setDefaultPaymentMethod(user, paymentMethodId);
        
        return ResponseEntity.ok(ApiResponse.success("Đã đặt phương thức thanh toán làm mặc định", paymentMethod));
    }
    
    /**
     * Lấy phương thức thanh toán mặc định
     */
    @GetMapping("/default")
    public ResponseEntity<ApiResponse<UserPaymentMethodResponseDto>> getDefaultPaymentMethod(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        log.info("User {} getting default payment method", userPrincipal.getId());
        
        User user = userService.getUserById(userPrincipal.getId());
        UserPaymentMethodResponseDto paymentMethod = userPaymentMethodService.getDefaultPaymentMethod(user);
        
        return ResponseEntity.ok(ApiResponse.success(paymentMethod));
    }
    
    /**
     * Lấy chi tiết phương thức thanh toán theo ID
     */
    @GetMapping("/{paymentMethodId}")
    public ResponseEntity<ApiResponse<UserPaymentMethodResponseDto>> getPaymentMethodById(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long paymentMethodId) {
        
        log.info("User {} getting payment method {}", userPrincipal.getId(), paymentMethodId);
        
        User user = userService.getUserById(userPrincipal.getId());
        UserPaymentMethodResponseDto paymentMethod = userPaymentMethodService.getPaymentMethodById(user, paymentMethodId);
        
        return ResponseEntity.ok(ApiResponse.success(paymentMethod));
    }
}