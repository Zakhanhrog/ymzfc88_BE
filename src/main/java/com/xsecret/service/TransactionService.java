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
import com.xsecret.service.OkdpayService.OkdpayCreateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

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
    private final PointService pointService;
    private final TelegramNotificationService telegramNotificationService;
    private final OkdpayService okdpayService;
    
    @Value("${app.okdpay.merchant-id:9182}")
    private String okdpayMerchantId;

    @Value("${app.okdpay.api-key}")
    private String okdpayApiKey;

    @Value("${app.okdpay.default-channel-code:1001}")
    private String okdpayDefaultChannelCode; // Mã kênh mặc định: 1001 - Chuyển khoản ngân hàng Việt Nam

    @Value("${app.okdpay.callback-base-url:https://api.tathiet168.com}")
    private String okdpayCallbackBaseUrl;

    @Value("${app.okdpay.frontend-base-url:https://tathiet168.com}")
    private String okdpayFrontendBaseUrl;
    
    /**
     * Tạo yêu cầu nạp tiền
     * Ưu tiên gateway tự động trước, nếu không có thì fallback về manual
     */
    public TransactionResponseDto createDepositRequest(DepositRequestDto request, String username) {
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
        
        // Ưu tiên: Kiểm tra auto deposit nếu Payment Method có bankCode (loại BANK)
        // ChannelCode có thể dùng mặc định (1001) nếu PaymentMethod không có
        if (paymentMethod.getType() == PaymentMethod.PaymentType.BANK 
                && paymentMethod.getBankCode() != null && !paymentMethod.getBankCode().trim().isEmpty()) {
            
            String channelCodeToUse = (paymentMethod.getChannelCode() != null && !paymentMethod.getChannelCode().trim().isEmpty())
                    ? paymentMethod.getChannelCode().trim()
                    : okdpayDefaultChannelCode + " (default)";
            
            log.info("Payment method has bankCode: {} and channelCode: {}. Attempting auto deposit.", 
                    paymentMethod.getBankCode(), channelCodeToUse);
            
            try {
                TransactionResponseDto result = createAutoDepositRequest(request, username, paymentMethod);
                log.info("Successfully created auto deposit order for user: {}, amount: {}, bankCode: {}, channelCode: {}", 
                        username, request.getAmount(), paymentMethod.getBankCode(), channelCodeToUse);
                return result;
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                // Kiểm tra nếu là lỗi channel bảo trì
                if (errorMsg != null && errorMsg.contains("CHANNEL_MAINTENANCE")) {
                    log.warn("Channel {} is under maintenance for payment method {}. Falling back to manual deposit.", 
                            channelCodeToUse, paymentMethod.getName());
                } else {
                    log.error("Failed to create auto deposit order, falling back to manual. Error: {}", errorMsg, e);
                }
                // Fallback về manual nếu tự động thất bại
            }
        } else {
            log.info("Payment method does not support auto deposit (type: {}, bankCode: {}). Using manual deposit.", 
                    paymentMethod.getType(), 
                    paymentMethod.getBankCode() != null ? paymentMethod.getBankCode() : "N/A");
        }
        
        // Fallback: Tạo đơn nạp thủ công
        return createManualDepositRequest(request, username, paymentMethod);
    }
    
    /**
     * Tạo đơn nạp tiền tự động qua gateway OKDPAY
     */
    private TransactionResponseDto createAutoDepositRequest(
            DepositRequestDto request, 
            String username, 
            PaymentMethod paymentMethod) {
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Tạo transaction code
        String transactionCode = generateTransactionCode("DEP");
        
        // Tính fee (có thể không có fee cho auto deposit hoặc tính từ gateway config)
        BigDecimal fee = BigDecimal.ZERO; // Auto deposit thường không có fee
        BigDecimal netAmount = request.getAmount().subtract(fee);
        
        // Tạo transaction với flag auto deposit
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
                .isAutoDeposit(true)
                .gatewayType("OKDPAY")
                .build();
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Tạo đơn trên OKDPAY với callback URL
        // Lưu ý: context-path là /api, nên endpoint đầy đủ là /api/payments/okdpay/callback
        String notifyUrl = okdpayCallbackBaseUrl + "/api/payments/okdpay/callback";
        // Return URL: khi user thanh toán xong sẽ quay lại trang wallet với tab deposit
        String returnUrl = okdpayFrontendBaseUrl + "/wallet?tab=deposit-withdraw&transaction=" + transactionCode;
        
        // Xác định channel code: dùng từ PaymentMethod nếu có, nếu không thì dùng mặc định 1001 (ngân hàng Việt Nam)
        String channelCode = (paymentMethod.getChannelCode() != null && !paymentMethod.getChannelCode().trim().isEmpty())
                ? paymentMethod.getChannelCode().trim()
                : okdpayDefaultChannelCode;
        
        // Đảm bảo channelCode luôn là "1001" cho ngân hàng Việt Nam nếu không có trong PaymentMethod
        if (channelCode == null || channelCode.trim().isEmpty()) {
            channelCode = "1001";
        }
        
        log.info("=== CREATING OKDPAY ORDER ===");
        log.info("Transaction Code: {}", transactionCode);
        log.info("Amount: {}", request.getAmount());
        log.info("Bank Code: {}", paymentMethod.getBankCode());
        log.info("Channel Code from PaymentMethod: {}", paymentMethod.getChannelCode());
        log.info("Using Channel Code: {} (default: {})", channelCode, okdpayDefaultChannelCode);
        log.info("Notify URL (Callback): {}", notifyUrl);
        log.info("Return URL: {}", returnUrl);
        log.info("Merchant ID: {}", okdpayMerchantId);
        
        OkdpayCreateOrderResponse okdpayResponse;
        try {
            okdpayResponse = okdpayService.createOrder(
                    okdpayMerchantId,
                    okdpayApiKey,
                    channelCode, // Dùng channelCode đã xác định (có fallback)
                    transactionCode,
                    request.getAmount(),
                    notifyUrl,
                    returnUrl
            );
            
            log.info("OKDPAY API response - Status: {}, OrderNo: {}, PayUrl: {}, Msg: {}", 
                    okdpayResponse.getStatus(), okdpayResponse.getOrderNo(), okdpayResponse.getPayUrl(), okdpayResponse.getMsg());
            
            if (!"success".equalsIgnoreCase(okdpayResponse.getStatus())) {
                String errorMsg = okdpayResponse.getMsg();
                log.error("OKDPAY API returned error - Status: {}, Msg: {}, ChannelCode: {}", 
                        okdpayResponse.getStatus(), errorMsg, channelCode);
                
                // Kiểm tra nếu là lỗi channel bảo trì
                if (errorMsg != null && (errorMsg.contains("维护") || errorMsg.contains("维护中") || errorMsg.contains("maintenance"))) {
                    log.warn("Channel code {} is under maintenance. Error message: {}", channelCode, errorMsg);
                    throw new RuntimeException("CHANNEL_MAINTENANCE: Channel " + channelCode + " is under maintenance. " + errorMsg);
                }
                
                throw new RuntimeException("OKDPAY API returned error: " + errorMsg);
            }
            
            if (okdpayResponse.getPayUrl() == null || okdpayResponse.getPayUrl().trim().isEmpty()) {
                log.error("OKDPAY API did not return payment URL. Response: {}", okdpayResponse);
                throw new RuntimeException("OKDPAY API did not return payment URL");
            }
        } catch (Exception e) {
            log.error("Error calling OKDPAY API for transaction {}: {}", transactionCode, e.getMessage(), e);
            
            // Kiểm tra nếu là lỗi channel bảo trì
            if (e.getMessage() != null && e.getMessage().contains("CHANNEL_MAINTENANCE")) {
                log.warn("Channel {} is under maintenance, will fallback to manual deposit", channelCode);
            }
            
            // Xóa transaction đã tạo nếu không tạo được đơn trên OKDPAY
            try {
                transactionRepository.delete(savedTransaction);
                log.info("Deleted transaction {} due to OKDPAY API error", transactionCode);
            } catch (Exception deleteEx) {
                log.error("Failed to delete transaction {}: {}", transactionCode, deleteEx.getMessage());
            }
            
            throw new RuntimeException("Failed to create order on OKDPAY: " + e.getMessage(), e);
        }
        
        // Cập nhật transaction với thông tin từ gateway
        savedTransaction.setGatewayOrderNo(okdpayResponse.getOrderNo());
        savedTransaction.setGatewayPayUrl(okdpayResponse.getPayUrl());
        savedTransaction.setNote("Auto deposit via OKDPAY gateway. Order No: " + okdpayResponse.getOrderNo());
        
        Transaction updatedTransaction = transactionRepository.save(savedTransaction);
        
        // Reload transaction từ DB để đảm bảo có đầy đủ thông tin (bao gồm cả lazy-loaded fields)
        updatedTransaction = transactionRepository.findById(updatedTransaction.getId())
                .orElseThrow(() -> new RuntimeException("Transaction not found after update"));
        
        log.info("Transaction {} updated with gateway info. PayUrl: {}", transactionCode, okdpayResponse.getPayUrl());
        
        log.info("Created auto deposit request: {} for user: {} amount: {} via bank: {} (channel: {})", 
                transactionCode, username, request.getAmount(), paymentMethod.getBankCode(), channelCode);
        
        try {
            String message = "Yêu cầu " + telegramNotificationService.formatBoldText("NẠP TIỀN TỰ ĐỘNG") + " từ khách hàng " + 
                          telegramNotificationService.formatBoldText(username) + " - Số tiền: " + 
                          telegramNotificationService.formatBoldText(telegramNotificationService.formatVnd(request.getAmount()));
            telegramNotificationService.sendMessage(message);
        } catch (Exception ignore) {}
        
        // Map từ entity đã reload để đảm bảo có đầy đủ thông tin
        TransactionResponseDto response = TransactionResponseDto.fromEntity(updatedTransaction);
        // Đảm bảo gatewayPayUrl được set đúng
        response.setGatewayPayUrl(updatedTransaction.getGatewayPayUrl());
        response.setIsAutoDeposit(updatedTransaction.getIsAutoDeposit());
        response.setGatewayOrderNo(updatedTransaction.getGatewayOrderNo());
        response.setGatewayType(updatedTransaction.getGatewayType());
        response.setNote(updatedTransaction.getNote());
        
        log.info("Returning transaction response - ID: {}, isAutoDeposit: {}, gatewayPayUrl: {}", 
                response.getId(), response.getIsAutoDeposit(), response.getGatewayPayUrl());
        
        return response;
    }
    
    /**
     * Tạo đơn nạp tiền thủ công (fallback)
     */
    private TransactionResponseDto createManualDepositRequest(
            DepositRequestDto request, 
            String username, 
            PaymentMethod paymentMethod) {
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
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
                .isAutoDeposit(false)
                .build();
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Created manual deposit request: {} for user: {} amount: {}", 
                transactionCode, username, request.getAmount());
           try {
               String message = "Yêu cầu " + telegramNotificationService.formatBoldText("NẠP TIỀN") + " từ khách hàng " + 
                              telegramNotificationService.formatBoldText(username) + " - Số tiền: " + 
                              telegramNotificationService.formatBoldText(telegramNotificationService.formatVnd(request.getAmount()));
               telegramNotificationService.sendMessage(message);
           } catch (Exception ignore) {}
        
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
        
        // Quy đổi số tiền sang điểm (1000 VND = 1 điểm)
        BigDecimal pointsRequired = request.getAmount().divide(BigDecimal.valueOf(1000), 0, java.math.RoundingMode.UP);
        
        // Kiểm tra số điểm có đủ không
        if (user.getPoints() < pointsRequired.longValue()) {
            throw new RuntimeException("Insufficient points. Need " + pointsRequired + " points (" + request.getAmount() + " VND), have " + user.getPoints() + " points");
        }
        
        // Kiểm tra số điểm (backward compatibility)
        if (request.getPoints() != null) {
            // Validate tính nhất quán giữa điểm và tiền (1000đ = 1 điểm)
            BigDecimal expectedAmount = BigDecimal.valueOf(request.getPoints() * 1000);
            if (request.getAmount().compareTo(expectedAmount) != 0) {
                throw new RuntimeException("Amount and points mismatch. Expected: " + expectedAmount + " VND for " + request.getPoints() + " points");
            }
            
            // Kiểm tra user có đủ điểm không (lấy từ user.points trực tiếp)
            long userPoints = user.getPoints() != null ? user.getPoints() : 0L;
            if (userPoints < request.getPoints()) {
                throw new RuntimeException("Insufficient points. You have " + userPoints + " points but trying to withdraw " + request.getPoints() + " points");
            }
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
                     (userPaymentMethod.getBankCode() != null ? " - " + userPaymentMethod.getBankCode() : "") +
                     (request.getPoints() != null ? " | Points: " + request.getPoints() : ""))
                .build();
        
        // Không set paymentMethod vì đây là UserPaymentMethod, không phải PaymentMethod
        // Thông tin đã được lưu trong methodAccount và note
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Trừ điểm ngay lập tức khi tạo withdraw request (để tránh abuse)
        // Sử dụng points từ request nếu có, nếu không thì tính từ amount
        long pointsToDeduct = request.getPoints() != null ? request.getPoints() : pointsRequired.longValue();
        
            try {
                // Trừ điểm trực tiếp từ user
                long currentPoints = user.getPoints() != null ? user.getPoints() : 0L;
                
                if (currentPoints < pointsToDeduct) {
                    transactionRepository.delete(savedTransaction);
                    throw new RuntimeException("Insufficient points. Available: " + currentPoints + ", Required: " + pointsToDeduct);
                }
                
                long newPoints = currentPoints - pointsToDeduct;
                user.setPoints(newPoints);
                userRepository.save(user);
                
                log.info("Deducted {} points from user {} for withdraw request {}. Points: {} -> {}", 
                    pointsToDeduct, username, transactionCode, currentPoints, newPoints);
        } catch (RuntimeException e) {
            // Rollback transaction nếu trừ điểm thất bại
            if (savedTransaction != null && savedTransaction.getId() != null) {
                transactionRepository.delete(savedTransaction);
            }
            throw e; // Re-throw để controller xử lý
            } catch (Exception e) {
            // Rollback transaction nếu có lỗi khác
            if (savedTransaction != null && savedTransaction.getId() != null) {
                transactionRepository.delete(savedTransaction);
            }
            log.error("Failed to deduct points for user {} withdraw request {}: {}", username, transactionCode, e.getMessage(), e);
            throw new RuntimeException("Failed to deduct points: " + e.getMessage());
        }
        
        log.info("Created user withdraw request: {} for user: {} amount: {} points: {} using payment method: {}", 
                transactionCode, username, request.getAmount(), request.getPoints(), userPaymentMethod.getName());
           try {
               String message = "Yêu cầu " + telegramNotificationService.formatBoldText("RÚT TIỀN") + " từ khách hàng " + 
                              telegramNotificationService.formatBoldText(username) + " - Số tiền: " + 
                              telegramNotificationService.formatBoldText(telegramNotificationService.formatVnd(request.getAmount()));
               telegramNotificationService.sendMessage(message);
           } catch (Exception ignore) {}
        
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
        
        // Quy đổi số tiền sang điểm (1000 VND = 1 điểm)
        BigDecimal pointsRequired = request.getAmount().divide(BigDecimal.valueOf(1000), 0, java.math.RoundingMode.UP);
        
        // Kiểm tra số điểm có đủ không
        if (user.getPoints() < pointsRequired.longValue()) {
            throw new RuntimeException("Insufficient points. Need " + pointsRequired + " points (" + request.getAmount() + " VND), have " + user.getPoints() + " points");
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
        try {
            String message = "Yêu cầu " + telegramNotificationService.formatBoldText("RÚT TIỀN") + " từ khách hàng " + 
                           telegramNotificationService.formatBoldText(username) + " - Số tiền: " + 
                           telegramNotificationService.formatBoldText(telegramNotificationService.formatVnd(request.getAmount()));
            telegramNotificationService.sendMessage(message);
        } catch (Exception ignore) {}
        
        return TransactionResponseDto.fromEntity(savedTransaction);
    }
    
    /**
     * Admin: Xử lý transaction (approve/reject)
     */
    @Transactional
    public TransactionResponseDto processTransaction(ProcessTransactionRequestDto request, String adminUsername) {
        // Load transaction với các relationships cần thiết
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        // Load user để tránh LazyInitializationException
        transaction.getUser().getUsername();
        
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
            
            // Xử lý điểm user
            User user = transaction.getUser();
            if (transaction.getType() == Transaction.TransactionType.DEPOSIT) {
                // Nạp tiền: quy đổi thành điểm và cộng vào (1000 VND = 1 điểm)
                BigDecimal amountToAdd = request.getActualAmount() != null ? 
                    request.getActualAmount() : transaction.getNetAmount();
                
                // Quy đổi VND thành điểm: 1000 VND = 1 điểm
                BigDecimal pointsToAdd = amountToAdd.divide(BigDecimal.valueOf(1000), 0, java.math.RoundingMode.DOWN);
                
                if (pointsToAdd.compareTo(BigDecimal.ZERO) > 0) {
                    // Cộng điểm trực tiếp vào user
                    long currentPoints = user.getPoints() != null ? user.getPoints() : 0L;
                    long newPoints = currentPoints + pointsToAdd.longValue();
                    user.setPoints(newPoints);
                    userRepository.save(user);
                    
                    log.info("Added {} VND as {} points to user {}. Points: {} -> {}. Transaction: {}", 
                            amountToAdd, pointsToAdd, user.getUsername(), currentPoints, newPoints, transaction.getTransactionCode());
                }
            } else if (transaction.getType() == Transaction.TransactionType.WITHDRAW) {
                // Rút tiền: Điểm đã được trừ khi tạo request, không cần làm gì thêm
                log.info("Approved withdraw transaction {} for user {}. Points were already deducted.", 
                        transaction.getTransactionCode(), user.getUsername());
            }
            
        } else {
            transaction.setStatus(Transaction.TransactionStatus.REJECTED);
            
            // Nếu là withdraw bị reject, hoàn lại điểm đã trừ
            if (transaction.getType() == Transaction.TransactionType.WITHDRAW) {
                try {
                    // Parse số điểm từ note của transaction
                    String note = transaction.getNote();
                    if (note != null && note.contains("Points: ")) {
                        String pointsStr = note.substring(note.indexOf("Points: ") + 8);
                        // Lấy số điểm (có thể có thêm text sau)
                        int endIndex = pointsStr.indexOf(" ");
                        if (endIndex == -1) endIndex = pointsStr.length();
                        
                        try {
                            Integer pointsToRefund = Integer.parseInt(pointsStr.substring(0, endIndex).trim());
                            
                            // Hoàn lại điểm trực tiếp vào user
                            User user = transaction.getUser();
                            long currentPoints = user.getPoints() != null ? user.getPoints() : 0L;
                            long newPoints = currentPoints + pointsToRefund;
                            user.setPoints(newPoints);
                            userRepository.save(user);
                            
                            log.info("Refunded {} points to user {} for rejected withdraw transaction {}. Points: {} -> {}", 
                                    pointsToRefund, user.getUsername(), transaction.getTransactionCode(), currentPoints, newPoints);
                        } catch (NumberFormatException e) {
                            log.warn("Could not parse points from transaction note: {}", note);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to refund points for rejected withdraw transaction {}: {}", 
                             transaction.getTransactionCode(), e.getMessage());
                }
            }
        }
        
        Transaction processedTransaction = transactionRepository.save(transaction);
        
        // Đảm bảo processedBy được load trước khi map sang DTO
        if (processedTransaction.getProcessedBy() != null) {
            processedTransaction.getProcessedBy().getUsername();
        }
        
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
        
        // Đảm bảo sắp xếp theo createdAt DESC (mới nhất trước)
        Pageable sortedPageable = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );
        
        Page<Transaction> transactions = transactionRepository.findByUserOrderByCreatedAtDesc(user, sortedPageable);
        return transactions.map(this::enrichTransactionWithPaymentMethod);
    }
    
    /**
     * Lấy giao dịch theo type của user
     */
    public List<TransactionResponseDto> getUserTransactionsByType(String username, Transaction.TransactionType type) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Transaction> transactions = transactionRepository.findByUserAndType(user, type);
        return transactions.stream()
                .map(this::enrichTransactionWithPaymentMethod)
                .collect(Collectors.toList());
    }
    
    /**
     * Admin: Lấy tất cả giao dịch pending
     */
    public Page<TransactionResponseDto> getPendingTransactions(Pageable pageable) {
        Page<Transaction> transactions = transactionRepository.findByStatusOrderByCreatedAtAsc(
                Transaction.TransactionStatus.PENDING, pageable);
        return transactions.map(this::enrichTransactionWithPaymentMethod);
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
        return transactions.map(this::enrichTransactionWithPaymentMethod);
    }
    
    /**
     * Helper method để enrich payment method data cho withdraw transactions
     */
    private TransactionResponseDto enrichTransactionWithPaymentMethod(Transaction transaction) {
        // Đảm bảo processedBy được load để tránh LazyInitializationException
        try {
            if (transaction.getProcessedBy() != null) {
                transaction.getProcessedBy().getUsername();
            }
        } catch (Exception e) {
            log.warn("Failed to load processedBy for transaction {}: {}", transaction.getId(), e.getMessage());
        }
        
        
        return TransactionResponseDto.fromEntity(transaction);
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
        
        return enrichTransactionWithPaymentMethod(transaction);
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
    public Map<String, Object> calculateUserTransactionStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Transaction> userTransactions = transactionRepository.findByUserOrderByCreatedAtDesc(user);
        
        Map<String, Object> stats = new HashMap<>();
        
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
        
        double totalBonus = userTransactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.BONUS && 
                           t.getStatus() == Transaction.TransactionStatus.APPROVED)
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();
        
        long pendingCount = userTransactions.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.PENDING)
                .count();
        
        stats.put("totalDeposit", totalDeposit);
        stats.put("totalWithdraw", totalWithdraw);
        stats.put("totalBonus", totalBonus);
        stats.put("pendingCount", pendingCount);
        
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