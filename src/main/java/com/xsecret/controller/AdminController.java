package com.xsecret.controller;

import com.xsecret.dto.request.CreateUserRequestDto;
import com.xsecret.dto.request.LoginRequest;
import com.xsecret.dto.request.PaymentMethodRequestDto;
import com.xsecret.dto.request.ProcessTransactionRequestDto;
import com.xsecret.dto.request.UpdateUserRequestDto;
import com.xsecret.dto.request.UserFilterRequestDto;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.JwtResponse;
import com.xsecret.dto.response.PaymentMethodResponseDto;
import com.xsecret.dto.response.TransactionResponseDto;
import com.xsecret.dto.response.UserResponse;
import com.xsecret.entity.Transaction;
import com.xsecret.entity.User;
import com.xsecret.mapper.UserMapper;
import com.xsecret.security.UserPrincipal;
import com.xsecret.service.AuthService;
import com.xsecret.service.PaymentMethodService;
import com.xsecret.service.TransactionService;
import com.xsecret.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService authService;
    private final UserService userService;
    private final UserMapper userMapper;
    private final TransactionService transactionService;
    private final PaymentMethodService paymentMethodService;

    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<JwtResponse>> adminLogin(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Admin login attempt for: {}", loginRequest.getUsernameOrEmail());
        
        JwtResponse jwtResponse = authService.login(loginRequest);
        
        // Verify user is admin
        if (!jwtResponse.getUser().getRole().equals(User.Role.ADMIN)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Bạn không có quyền truy cập admin"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập admin thành công", jwtResponse));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<Object>> getDashboardStats() {
        log.info("Getting dashboard stats");
        
        var stats = userService.getDashboardStats();
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        log.info("Getting all users");
        
        List<User> users = userService.getAllUsers();
        List<UserResponse> userResponses = users.stream()
                .map(userMapper::toUserResponse)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(userResponses));
    }

    @GetMapping("/users/paginated")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsersWithFilters(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting users with filters - role: {}, status: {}, search: {}", role, status, searchTerm);
        
        UserFilterRequestDto filters = UserFilterRequestDto.builder()
                .role(role)
                .status(status)
                .searchTerm(searchTerm)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();
        
        Page<User> users = userService.getUsersWithFilters(filters);
        Page<UserResponse> userResponses = users.map(userMapper::toUserResponse);
        
        return ResponseEntity.ok(ApiResponse.success(userResponses));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequestDto request) {
        log.info("Creating new user: {}", request.getUsername());
        
        try {
            User user = userService.createUser(request);
            UserResponse userResponse = userMapper.toUserResponse(user);
            return ResponseEntity.ok(ApiResponse.success("Tạo người dùng thành công", userResponse));
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        log.info("Getting user by id: {}", id);
        
        User user = userService.getUserById(id);
        UserResponse userResponse = userMapper.toUserResponse(user);
        
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam User.UserStatus status) {
        log.info("Updating user {} status to {}", id, status);
        
        User user = userService.updateUserStatus(id, status);
        UserResponse userResponse = userMapper.toUserResponse(user);
        
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", userResponse));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequestDto request) {
        log.info("Updating user: {}", id);
        
        try {
            User user = userService.updateUser(id, request);
            UserResponse userResponse = userMapper.toUserResponse(user);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật người dùng thành công", userResponse));
        } catch (Exception e) {
            log.error("Error updating user", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        log.info("Deleting user: {}", id);
        
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.<Void>success("Xóa người dùng thành công", null));
        } catch (Exception e) {
            log.error("Error deleting user", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetUserPassword(
            @PathVariable Long id,
            @RequestParam String newPassword) {
        log.info("Resetting password for user: {}", id);
        
        try {
            userService.resetUserPassword(id, newPassword);
            return ResponseEntity.ok(ApiResponse.<Void>success("Reset mật khẩu thành công", null));
        } catch (Exception e) {
            log.error("Error resetting password", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/balance")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserBalance(
            @PathVariable Long id,
            @RequestParam Double balance) {
        log.info("Updating balance for user: {} to {}", id, balance);
        
        try {
            User user = userService.updateUserBalance(id, balance);
            UserResponse userResponse = userMapper.toUserResponse(user);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật số dư thành công", userResponse));
        } catch (Exception e) {
            log.error("Error updating balance", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/users/stats")
    public ResponseEntity<ApiResponse<Object>> getUserStats() {
        log.info("Getting user statistics");
        
        var stats = userService.getAdvancedUserStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/users/recent-active")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getRecentActiveUsers(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Getting recent active users with limit: {}", limit);
        
        List<User> users = userService.getRecentActiveUsers(limit);
        List<UserResponse> userResponses = users.stream()
                .map(userMapper::toUserResponse)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(userResponses));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getAdminProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Getting admin profile for: {}", userPrincipal.getUsername());
        
        User user = userService.getUserById(userPrincipal.getId());
        UserResponse userResponse = userMapper.toUserResponse(user);
        
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }
    
    // ======================== TRANSACTION MANAGEMENT ========================
    
    /**
     * Lấy tất cả giao dịch pending
     */
    @GetMapping("/transactions/pending")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getPendingTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<TransactionResponseDto> transactions = transactionService.getPendingTransactions(pageable);
            return ResponseEntity.ok(ApiResponse.success(transactions));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Lấy giao dịch với filter
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getTransactionsWithFilters(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Transaction.TransactionType transactionType = type != null ? 
                Transaction.TransactionType.valueOf(type.toUpperCase()) : null;
            Transaction.TransactionStatus transactionStatus = status != null ? 
                Transaction.TransactionStatus.valueOf(status.toUpperCase()) : null;
            
            Pageable pageable = PageRequest.of(page, size);
            Page<TransactionResponseDto> transactions = transactionService.getTransactionsWithFilters(
                    transactionType, transactionStatus, startDate, endDate, pageable);
            return ResponseEntity.ok(ApiResponse.success(transactions));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Xử lý giao dịch (approve/reject)
     */
    @PostMapping("/transactions/process")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> processTransaction(
            @Valid @RequestBody ProcessTransactionRequestDto request,
            Authentication authentication) {
        try {
            TransactionResponseDto transaction = transactionService.processTransaction(
                    request, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Transaction processed successfully", transaction));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Lấy chi tiết giao dịch
     */
    @GetMapping("/transactions/{id}")
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
    
    // ======================== PAYMENT METHOD MANAGEMENT ========================
    
    /**
     * Lấy tất cả payment methods
     */
    @GetMapping("/payment-methods")
    public ResponseEntity<ApiResponse<List<PaymentMethodResponseDto>>> getAllPaymentMethods() {
        try {
            List<PaymentMethodResponseDto> paymentMethods = paymentMethodService.getAllPaymentMethods();
            return ResponseEntity.ok(ApiResponse.success(paymentMethods));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Tạo payment method mới
     */
    @PostMapping("/payment-methods")
    public ResponseEntity<ApiResponse<PaymentMethodResponseDto>> createPaymentMethod(
            @Valid @RequestBody PaymentMethodRequestDto request) {
        try {
            PaymentMethodResponseDto paymentMethod = paymentMethodService.createPaymentMethod(request);
            return ResponseEntity.ok(ApiResponse.success("Payment method created successfully", paymentMethod));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Cập nhật payment method
     */
    @PutMapping("/payment-methods/{id}")
    public ResponseEntity<ApiResponse<PaymentMethodResponseDto>> updatePaymentMethod(
            @PathVariable Long id,
            @Valid @RequestBody PaymentMethodRequestDto request) {
        try {
            PaymentMethodResponseDto paymentMethod = paymentMethodService.updatePaymentMethod(id, request);
            return ResponseEntity.ok(ApiResponse.success("Payment method updated successfully", paymentMethod));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Xóa payment method
     */
    @DeleteMapping("/payment-methods/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePaymentMethod(@PathVariable Long id) {
        try {
            paymentMethodService.deletePaymentMethod(id);
            return ResponseEntity.ok(ApiResponse.<Void>success("Payment method deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Toggle trạng thái payment method
     */
    @PutMapping("/payment-methods/{id}/toggle-status")
    public ResponseEntity<ApiResponse<PaymentMethodResponseDto>> togglePaymentMethodStatus(@PathVariable Long id) {
        try {
            PaymentMethodResponseDto paymentMethod = paymentMethodService.togglePaymentMethodStatus(id);
            return ResponseEntity.ok(ApiResponse.success("Payment method status updated", paymentMethod));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    // ======================== DEPOSIT MANAGEMENT ========================
    
    /**
     * Lấy danh sách yêu cầu nạp tiền
     */
    @GetMapping("/deposits")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getDepositRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Transaction.TransactionStatus transactionStatus = status != null && !status.isEmpty() ? 
                Transaction.TransactionStatus.valueOf(status.toUpperCase()) : null;
            
            Pageable pageable = PageRequest.of(page, size);
            Page<TransactionResponseDto> deposits = transactionService.getTransactionsWithFilters(
                    Transaction.TransactionType.DEPOSIT, transactionStatus, startDate, endDate, pageable);
            return ResponseEntity.ok(ApiResponse.success(deposits));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Duyệt nạp tiền
     */
    @PostMapping("/deposits/{id}/approve")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> approveDeposit(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            ProcessTransactionRequestDto request = new ProcessTransactionRequestDto();
            request.setTransactionId(id);
            request.setAction(ProcessTransactionRequestDto.Action.APPROVE);
            request.setAdminNote("Approved by admin");
                    
            TransactionResponseDto transaction = transactionService.processTransaction(request, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Deposit approved successfully", transaction));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Từ chối nạp tiền
     */
    @PostMapping("/deposits/{id}/reject")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> rejectDeposit(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> requestBody,
            Authentication authentication) {
        try {
            String reason = requestBody != null ? requestBody.get("reason") : "No reason provided";
            
            ProcessTransactionRequestDto request = new ProcessTransactionRequestDto();
            request.setTransactionId(id);
            request.setAction(ProcessTransactionRequestDto.Action.REJECT);
            request.setAdminNote(reason);
                    
            TransactionResponseDto transaction = transactionService.processTransaction(request, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Deposit rejected successfully", transaction));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Lấy thống kê nạp tiền
     */
    @GetMapping("/deposits/statistics")
    public ResponseEntity<ApiResponse<Object>> getDepositStatistics() {
        try {
            var stats = transactionService.getDepositStatistics();
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Error getting deposit statistics", e);
            return ResponseEntity.ok(ApiResponse.success(java.util.Map.of()));
        }
    }
    
    // ======================== WITHDRAW MANAGEMENT ========================
    
    /**
     * Lấy danh sách yêu cầu rút tiền
     */
    @GetMapping("/withdraws")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getWithdrawRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Transaction.TransactionStatus transactionStatus = status != null && !status.isEmpty() ? 
                Transaction.TransactionStatus.valueOf(status.toUpperCase()) : null;
            
            Pageable pageable = PageRequest.of(page, size);
            Page<TransactionResponseDto> withdraws = transactionService.getTransactionsWithFilters(
                    Transaction.TransactionType.WITHDRAW, transactionStatus, startDate, endDate, pageable);
            return ResponseEntity.ok(ApiResponse.success(withdraws));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Duyệt rút tiền
     */
    @PostMapping("/withdraws/{id}/approve")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> approveWithdraw(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            ProcessTransactionRequestDto request = new ProcessTransactionRequestDto();
            request.setTransactionId(id);
            request.setAction(ProcessTransactionRequestDto.Action.APPROVE);
            request.setAdminNote("Approved by admin");
                    
            TransactionResponseDto transaction = transactionService.processTransaction(request, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Withdraw approved successfully", transaction));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Từ chối rút tiền
     */
    @PostMapping("/withdraws/{id}/reject")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> rejectWithdraw(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> requestBody,
            Authentication authentication) {
        try {
            String reason = requestBody != null ? requestBody.get("reason") : "No reason provided";
            
            ProcessTransactionRequestDto request = new ProcessTransactionRequestDto();
            request.setTransactionId(id);
            request.setAction(ProcessTransactionRequestDto.Action.REJECT);
            request.setAdminNote(reason);
                    
            TransactionResponseDto transaction = transactionService.processTransaction(request, authentication.getName());
            return ResponseEntity.ok(ApiResponse.success("Withdraw rejected successfully", transaction));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * Lấy thống kê rút tiền
     */
    @GetMapping("/withdraws/statistics")
    public ResponseEntity<ApiResponse<Object>> getWithdrawStatistics() {
        try {
            var stats = transactionService.getWithdrawStatistics();
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Error getting withdraw statistics", e);
            return ResponseEntity.ok(ApiResponse.success(java.util.Map.of()));
        }
    }
}
