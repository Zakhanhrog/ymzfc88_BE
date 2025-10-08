package com.xsecret.service;

import com.xsecret.dto.request.DepositRequestDto;
import com.xsecret.dto.request.ProcessTransactionRequestDto;
import com.xsecret.dto.request.WithdrawRequestDto;
import com.xsecret.dto.request.UserWithdrawRequestDto;
import com.xsecret.dto.response.TransactionResponseDto;
import com.xsecret.entity.PaymentMethod;
import com.xsecret.entity.Transaction;
import com.xsecret.entity.User;
import com.xsecret.entity.UserPaymentMethod;
import com.xsecret.repository.PaymentMethodRepository;
import com.xsecret.repository.TransactionRepository;
import com.xsecret.repository.UserRepository;
import com.xsecret.repository.UserPaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final UserRepository userRepository;
    private final UserPaymentMethodRepository userPaymentMethodRepository;
    private final FileStorageService fileStorageService;
    
    /**
     * Tạo yêu cầu nạp tiền
     */
    public TransactionResponseDto createDepositRequest(DepositRequestDto request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                .orElseThrow(() -> new RuntimeException("Payment method not found"));
        
        // Validate payment method is active
        if (!paymentMethod.getIsActive()) {
            throw new RuntimeException("Payment method is not available");
        }
        
        // Validate amount
        if (request.getAmount().compareTo(paymentMethod.getMinAmount()) < 0) {
            throw new RuntimeException("Amount is below minimum limit: " + paymentMethod.getMinAmount());
        }
        
        if (request.getAmount().compareTo(paymentMethod.getMaxAmount()) > 0) {
            throw new RuntimeException("Amount exceeds maximum limit: " + paymentMethod.getMaxAmount());
        }
        
        // Tính fee
        BigDecimal fee = calculateFee(paymentMethod, request.getAmount());
        BigDecimal netAmount = request.getAmount().subtract(fee);
        
        // Tạo transaction code
        String transactionCode = generateTransactionCode("DEP");
        
        // Xử lý ảnh bill nếu có
        String billImageUrl = null;
        if (request.getBillImage() != null && !request.getBillImage().trim().isEmpty()) {
            if (fileStorageService.isValidBase64Image(request.getBillImage())) {
                try {
                    billImageUrl = fileStorageService.saveBase64Image(
                        request.getBillImage(), 
                        request.getBillImageName()
                    );
                } catch (Exception e) {
                    log.warn("Failed to save bill image for transaction {}: {}", transactionCode, e.getMessage());
                    // Không throw exception, vẫn cho phép tạo transaction
                }
            } else {
                log.warn("Invalid base64 image data for transaction {}", transactionCode);
            }
        }
        
        Transaction transaction = Transaction.builder()
                .transactionCode(transactionCode)
                .user(user)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .fee(fee)
                .netAmount(netAmount)
                .status(Transaction.TransactionStatus.PENDING)
                .paymentMethod(paymentMethod)
                .methodAccount(paymentMethod.getAccountNumber())
                .description(request.getDescription())
                .referenceCode(request.getReferenceCode())
                .billImage(request.getBillImage()) // Lưu base64 để backup
                .billImageName(request.getBillImageName())
                .billImageUrl(billImageUrl) // URL của file đã lưu
                .build();
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Created deposit request: {} for user: {} amount: {}", 
                transactionCode, username, request.getAmount());
        
        return TransactionResponseDto.fromEntity(savedTransaction);
    }
    
    /**
     * Tạo yêu cầu rút tiền với UserPaymentMethod
     */
    public TransactionResponseDto createUserWithdrawRequest(UserWithdrawRequestDto request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Kiểm tra xem người dùng có bị khóa rút tiền không
        if (user.getWithdrawalLocked() != null && user.getWithdrawalLocked()) {
            String reason = user.getWithdrawalLockReason() != null ? 
                user.getWithdrawalLockReason() : "Tài khoản của bạn đã bị khóa rút tiền";
            throw new RuntimeException("WITHDRAWAL_LOCKED: " + reason);
        }
        
        UserPaymentMethod userPaymentMethod = userPaymentMethodRepository.findById(request.getUserPaymentMethodId())
                .orElseThrow(() -> new RuntimeException("User payment method not found"));
        
        // Validate that this payment method belongs to the user
        if (!userPaymentMethod.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied: This payment method does not belong to you");
        }
        
        // Kiểm tra số dư
        if (user.getBalance() < request.getAmount().doubleValue()) {
            throw new RuntimeException("Insufficient balance");
        }
        
        // Tính fee (có thể set fee cố định hoặc tính theo % cho withdraw)
        BigDecimal fee = BigDecimal.ZERO; // Có thể customize fee cho withdraw
        BigDecimal netAmount = request.getAmount().subtract(fee);
        
        // Tạo transaction code
        String transactionCode = generateTransactionCode("WDR");
        
        Transaction transaction = Transaction.builder()
                .transactionCode(transactionCode)
                .user(user)
                .type(Transaction.TransactionType.WITHDRAW)
                .amount(request.getAmount())
                .fee(fee)
                .netAmount(netAmount)
                .status(Transaction.TransactionStatus.PENDING)
                .methodAccount(userPaymentMethod.getAccountNumber())
                .description(request.getDescription())
                .note("Account: " + userPaymentMethod.getAccountName() + " - " + userPaymentMethod.getAccountNumber() + 
                     " (" + userPaymentMethod.getType().name() + ")" +
                     (userPaymentMethod.getBankCode() != null ? " - " + userPaymentMethod.getBankCode() : ""))
                .build();
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Created user withdraw request: {} for user: {} amount: {} using payment method: {}", 
                transactionCode, username, request.getAmount(), userPaymentMethod.getName());
        
        return TransactionResponseDto.fromEntity(savedTransaction);
    }
    
    /**
     * Tạo yêu cầu rút tiền (legacy method)
     */
    public TransactionResponseDto createWithdrawRequest(WithdrawRequestDto request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Kiểm tra xem người dùng có bị khóa rút tiền không
        if (user.getWithdrawalLocked() != null && user.getWithdrawalLocked()) {
            String reason = user.getWithdrawalLockReason() != null ? 
                user.getWithdrawalLockReason() : "Tài khoản của bạn đã bị khóa rút tiền";
            throw new RuntimeException("WITHDRAWAL_LOCKED: " + reason);
        }
        
        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                .orElseThrow(() -> new RuntimeException("Payment method not found"));
        
        // Validate payment method is active
        if (!paymentMethod.getIsActive()) {
            throw new RuntimeException("Payment method is not available");
        }
        
        // Validate amount
        if (request.getAmount().compareTo(paymentMethod.getMinAmount()) < 0) {
            throw new RuntimeException("Amount is below minimum limit: " + paymentMethod.getMinAmount());
        }
        
        if (request.getAmount().compareTo(paymentMethod.getMaxAmount()) > 0) {
            throw new RuntimeException("Amount exceeds maximum limit: " + paymentMethod.getMaxAmount());
        }
        
        // Kiểm tra số dư
        if (user.getBalance() < request.getAmount().doubleValue()) {
            throw new RuntimeException("Insufficient balance");
        }
        
        // Tính fee
        BigDecimal fee = calculateFee(paymentMethod, request.getAmount());
        BigDecimal netAmount = request.getAmount().subtract(fee);
        
        // Tạo transaction code
        String transactionCode = generateTransactionCode("WDR");
        
        Transaction transaction = Transaction.builder()
                .transactionCode(transactionCode)
                .user(user)
                .type(Transaction.TransactionType.WITHDRAW)
                .amount(request.getAmount())
                .fee(fee)
                .netAmount(netAmount)
                .status(Transaction.TransactionStatus.PENDING)
                .paymentMethod(paymentMethod)
                .methodAccount(request.getAccountNumber())
                .description(request.getDescription())
                .note("Account: " + request.getAccountName() + " - " + request.getAccountNumber() + 
                     (request.getBankCode() != null ? " (" + request.getBankCode() + ")" : ""))
                .build();
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Created withdraw request: {} for user: {} amount: {}", 
                transactionCode, username, request.getAmount());
        
        return TransactionResponseDto.fromEntity(savedTransaction);
    }
    
    /**
     * Admin: Xử lý transaction (approve/reject)
     */
    public TransactionResponseDto processTransaction(ProcessTransactionRequestDto request, String adminUsername) {
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new RuntimeException("Transaction has already been processed");
        }
        
        transaction.setProcessedBy(admin);
        transaction.setProcessedAt(LocalDateTime.now());
        transaction.setAdminNote(request.getAdminNote());
        
        if (request.getAction() == ProcessTransactionRequestDto.Action.APPROVE) {
            transaction.setStatus(Transaction.TransactionStatus.APPROVED);
            
            // Xử lý số dư user
            User user = transaction.getUser();
            if (transaction.getType() == Transaction.TransactionType.DEPOSIT) {
                // Nạp tiền: cộng vào số dư
                BigDecimal amountToAdd = request.getActualAmount() != null ? 
                    request.getActualAmount() : transaction.getNetAmount();
                user.setBalance(user.getBalance() + amountToAdd.doubleValue());
                log.info("Added {} to user {} balance. New balance: {}", 
                        amountToAdd, user.getUsername(), user.getBalance());
            } else if (transaction.getType() == Transaction.TransactionType.WITHDRAW) {
                // Rút tiền: trừ từ số dư
                user.setBalance(user.getBalance() - transaction.getAmount().doubleValue());
                log.info("Deducted {} from user {} balance. New balance: {}", 
                        transaction.getAmount(), user.getUsername(), user.getBalance());
            }
            userRepository.save(user);
            
        } else {
            transaction.setStatus(Transaction.TransactionStatus.REJECTED);
        }
        
        Transaction processedTransaction = transactionRepository.save(transaction);
        log.info("Admin {} {} transaction {}", adminUsername, 
                request.getAction().name().toLowerCase(), transaction.getTransactionCode());
        
        return TransactionResponseDto.fromEntity(processedTransaction);
    }
    
    /**
     * Lấy lịch sử giao dịch của user
     */
    public Page<TransactionResponseDto> getUserTransactions(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Page<Transaction> transactions = transactionRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return transactions.map(TransactionResponseDto::fromEntity);
    }
    
    /**
     * Lấy giao dịch theo type của user
     */
    public List<TransactionResponseDto> getUserTransactionsByType(String username, Transaction.TransactionType type) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Transaction> transactions = transactionRepository.findByUserAndType(user, type);
        return transactions.stream()
                .map(TransactionResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Admin: Lấy tất cả giao dịch pending
     */
    public Page<TransactionResponseDto> getPendingTransactions(Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findByStatusOrderByCreatedAtAsc(
                Transaction.TransactionStatus.PENDING, pageable);
        return transactions.map(TransactionResponseDto::fromEntity);
    }
    
    /**
     * Admin: Lấy giao dịch theo filter
     */
    public Page<TransactionResponseDto> getTransactionsWithFilters(
            Transaction.TransactionType type,
            Transaction.TransactionStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        
        Page<Transaction> transactions = transactionRepository.findTransactionsWithFilters(
                type, status, startDate, endDate, pageable);
        return transactions.map(TransactionResponseDto::fromEntity);
    }
    
    /**
     * Lấy thông tin chi tiết giao dịch
     */
    public TransactionResponseDto getTransactionById(Long id, String username) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        // Kiểm tra quyền xem (user chỉ được xem giao dịch của mình)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() != User.Role.ADMIN && !transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        return TransactionResponseDto.fromEntity(transaction);
    }
    
    /**
     * Tính toán fee
     */
    private BigDecimal calculateFee(PaymentMethod paymentMethod, BigDecimal amount) {
        BigDecimal fee = BigDecimal.ZERO;
        
        // Tính fee theo phần trăm
        if (paymentMethod.getFeePercent() != null && paymentMethod.getFeePercent().compareTo(BigDecimal.ZERO) > 0) {
            fee = fee.add(amount.multiply(paymentMethod.getFeePercent()).divide(BigDecimal.valueOf(100)));
        }
        
        // Cộng thêm fee cố định
        if (paymentMethod.getFeeFixed() != null && paymentMethod.getFeeFixed().compareTo(BigDecimal.ZERO) > 0) {
            fee = fee.add(paymentMethod.getFeeFixed());
        }
        
        return fee;
    }
    
    /**
     * Tạo mã giao dịch
     */
    private String generateTransactionCode(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomNumber = String.valueOf((int)(Math.random() * 1000));
        String code = prefix + timestamp + randomNumber;
        
        // Đảm bảo unique
        while (transactionRepository.existsByTransactionCode(code)) {
            randomNumber = String.valueOf((int)(Math.random() * 1000));
            code = prefix + timestamp + randomNumber;
        }
        
        return code;
    }
    
    /**
     * Tính toán thống kê giao dịch của user
     */
    public Map<String, Double> calculateUserTransactionStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Transaction> userTransactions = transactionRepository.findByUserOrderByCreatedAtDesc(user);
        
        Map<String, Double> stats = new HashMap<>();
        
        double totalDeposit = userTransactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.DEPOSIT && 
                           t.getStatus() == Transaction.TransactionStatus.APPROVED)
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();
        
        double totalWithdraw = userTransactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.WITHDRAW && 
                           t.getStatus() == Transaction.TransactionStatus.APPROVED)
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();
        
        stats.put("totalDeposit", totalDeposit);
        stats.put("totalWithdraw", totalWithdraw);
        
        return stats;
    }
    
    /**
     * Lấy thống kê nạp tiền cho admin
     */
    public Map<String, Object> getDepositStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        
        // Tổng số pending deposits
        long pendingCount = transactionRepository.countByTypeAndStatus(
                Transaction.TransactionType.DEPOSIT, 
                Transaction.TransactionStatus.PENDING);
        
        // Số deposits đã duyệt hôm nay
        long approvedToday = transactionRepository.countByTypeAndStatusAndCreatedAtBetween(
                Transaction.TransactionType.DEPOSIT,
                Transaction.TransactionStatus.APPROVED,
                today, tomorrow);
        
        // Tổng tiền deposits hôm nay
        BigDecimal totalAmountToday = transactionRepository.sumAmountByTypeAndStatusAndCreatedAtBetween(
                Transaction.TransactionType.DEPOSIT,
                Transaction.TransactionStatus.APPROVED,
                today, tomorrow);
        
        // Số deposits bị từ chối hôm nay
        long rejectedToday = transactionRepository.countByTypeAndStatusAndCreatedAtBetween(
                Transaction.TransactionType.DEPOSIT,
                Transaction.TransactionStatus.REJECTED,
                today, tomorrow);
        
        stats.put("pending", pendingCount);
        stats.put("approvedToday", approvedToday);
        stats.put("totalAmountToday", totalAmountToday != null ? totalAmountToday.doubleValue() : 0.0);
        stats.put("rejectedToday", rejectedToday);
        
        return stats;
    }
    
    /**
     * Lấy thống kê rút tiền cho admin
     */
    public Map<String, Object> getWithdrawStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        
        // Tổng số pending withdraws
        long pendingCount = transactionRepository.countByTypeAndStatus(
                Transaction.TransactionType.WITHDRAW, 
                Transaction.TransactionStatus.PENDING);
        
        // Số withdraws đã duyệt hôm nay
        long approvedToday = transactionRepository.countByTypeAndStatusAndCreatedAtBetween(
                Transaction.TransactionType.WITHDRAW,
                Transaction.TransactionStatus.APPROVED,
                today, tomorrow);
        
        // Tổng tiền withdraws hôm nay
        BigDecimal totalAmountToday = transactionRepository.sumAmountByTypeAndStatusAndCreatedAtBetween(
                Transaction.TransactionType.WITHDRAW,
                Transaction.TransactionStatus.APPROVED,
                today, tomorrow);
        
        // Số withdraws bị từ chối hôm nay
        long rejectedToday = transactionRepository.countByTypeAndStatusAndCreatedAtBetween(
                Transaction.TransactionType.WITHDRAW,
                Transaction.TransactionStatus.REJECTED,
                today, tomorrow);
        
        stats.put("pending", pendingCount);
        stats.put("approvedToday", approvedToday);
        stats.put("totalAmountToday", totalAmountToday != null ? totalAmountToday.doubleValue() : 0.0);
        stats.put("rejectedToday", rejectedToday);
        
        return stats;
    }
}