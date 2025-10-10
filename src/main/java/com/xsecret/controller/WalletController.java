package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.entity.User;
import com.xsecret.service.UserService;
import com.xsecret.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {
    
    private final UserService userService;
    private final TransactionService transactionService;
    
    /**
     * Lấy điểm và thông tin ví của user hiện tại
     */
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWalletBalance(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            
            // Tính toán các thống kê từ transactions
            Map<String, Double> transactionStats = transactionService.calculateUserTransactionStats(user.getUsername());
            
            Map<String, Object> walletData = new HashMap<>();
            walletData.put("points", user.getPoints() != null ? user.getPoints() : 0L);
            walletData.put("totalDeposit", transactionStats.getOrDefault("totalDeposit", 0.0));
            walletData.put("totalWithdraw", transactionStats.getOrDefault("totalWithdraw", 0.0));
            walletData.put("totalBonus", 0.0); // Có thể tính từ bonus transactions sau
            walletData.put("frozenAmount", 0.0); // Có thể tính từ pending transactions
            
            return ResponseEntity.ok(ApiResponse.success(walletData));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi khi lấy thông tin ví: " + e.getMessage()));
        }
    }
    
    /**
     * Lấy thông tin user hiện tại
     */
    @GetMapping("/user-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserInfo(Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName());
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("phone", user.getPhoneNumber());
            userInfo.put("points", user.getPoints() != null ? user.getPoints() : 0L);
            userInfo.put("status", user.getStatus());
            userInfo.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(ApiResponse.success(userInfo));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Lỗi khi lấy thông tin user: " + e.getMessage()));
        }
    }
}