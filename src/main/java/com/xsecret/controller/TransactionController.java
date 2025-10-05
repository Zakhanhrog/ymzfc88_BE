package com.xsecret.controller;

import com.xsecret.dto.request.DepositRequestDto;
import com.xsecret.dto.request.WithdrawRequestDto;
import com.xsecret.dto.request.UserWithdrawRequestDto;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.PaymentMethodResponseDto;
import com.xsecret.dto.response.TransactionResponseDto;
import com.xsecret.entity.PaymentMethod;
import com.xsecret.entity.Transaction;
import com.xsecret.service.PaymentMethodService;
import com.xsecret.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    
    private final TransactionService transactionService;
    private final PaymentMethodService paymentMethodService;
    
    /**
     * Lấy danh sách payment methods available
     */
    @GetMapping("/payment-methods")
    public ResponseEntity<ApiResponse<List<PaymentMethodResponseDto>>> getPaymentMethods() {
        List<PaymentMethodResponseDto> paymentMethods = paymentMethodService.getActivePaymentMethods();
        return ResponseEntity.ok(ApiResponse.success(paymentMethods));
    }
    
    /**
     * Lấy payment methods theo type
     */
    @GetMapping("/payment-methods/{type}")
    public ResponseEntity<ApiResponse<List<PaymentMethodResponseDto>>> getPaymentMethodsByType(
            @PathVariable String type) {
        try {
            List<PaymentMethodResponseDto> paymentMethods = paymentMethodService.getPaymentMethodsByType(
                    PaymentMethod.PaymentType.valueOf(type.toUpperCase()));
            return ResponseEntity.ok(ApiResponse.success(paymentMethods));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid payment method type"));
        }
    }
    
    /**
     * Tạo yêu cầu nạp tiền
     */
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> createDepositRequest(
            @Valid @RequestBody DepositRequestDto request,
            Authentication authentication) {
        try {
            TransactionResponseDto transaction = transactionService.createDepositRequest(
                    request, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Deposit request created successfully", transaction));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Tạo yêu cầu rút tiền với UserPaymentMethod
     */
    @PostMapping("/user-withdraw")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> createUserWithdrawRequest(
            @Valid @RequestBody UserWithdrawRequestDto request,
            Authentication authentication) {
        try {
            TransactionResponseDto transaction = transactionService.createUserWithdrawRequest(
                    request, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("User withdraw request created successfully", transaction));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Tạo yêu cầu rút tiền (legacy)
     */
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> createWithdrawRequest(
            @Valid @RequestBody WithdrawRequestDto request,
            Authentication authentication) {
        try {
            TransactionResponseDto transaction = transactionService.createWithdrawRequest(
                    request, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Withdraw request created successfully", transaction));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Lấy lịch sử giao dịch của user
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getUserTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TransactionResponseDto> transactions = transactionService.getUserTransactions(
                    authentication.getName(), pageable);
            return ResponseEntity.ok(ApiResponse.success(transactions));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Lấy giao dịch theo type
     */
    @GetMapping("/history/{type}")
    public ResponseEntity<ApiResponse<List<TransactionResponseDto>>> getUserTransactionsByType(
            @PathVariable String type,
            Authentication authentication) {
        try {
            Transaction.TransactionType transactionType = Transaction.TransactionType.valueOf(type.toUpperCase());
            List<TransactionResponseDto> transactions = transactionService.getUserTransactionsByType(
                    authentication.getName(), transactionType);
            return ResponseEntity.ok(ApiResponse.success(transactions));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid transaction type"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Lấy chi tiết giao dịch
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> getTransactionById(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            TransactionResponseDto transaction = transactionService.getTransactionById(id, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success(transaction));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}