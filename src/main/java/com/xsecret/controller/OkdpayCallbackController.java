package com.xsecret.controller;

import com.xsecret.entity.Transaction;
import com.xsecret.entity.User;
import com.xsecret.repository.TransactionRepository;
import com.xsecret.service.OkdpayService;
import com.xsecret.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
@Slf4j
public class OkdpayCallbackController {

    private final OkdpayService okdpayService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    
    // OKDPAY Config - Dùng trực tiếp trong code như mẫu
    private static final String OKDPAY_MERCHANT_ID = "9182";
    private static final String OKDPAY_API_KEY = "iFilLS5aURGLl4der7krYAZ3LqfwWKJV6O0wDux3jbX50RdH83btRtik31KYzoje";

    /**
     * Callback endpoint để nhận thông báo từ OKDPAY
     * OKDPAY sẽ gửi POST request với form data
     */
    @PostMapping("/callbackbank")
    @Transactional
    public ResponseEntity<String> handleCallback(
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {
        
        try {
            String clientIp = getClientIp(request);
            log.info("=== OKDPAY CALLBACK RECEIVED ===");
            log.info("Callback IP: {}", clientIp);
            log.info("Callback params: {}", params);
            log.info("Callback headers: {}", request.getHeaderNames());

            // Lấy thông tin từ callback (theo tài liệu OKDPAY)
            String mchid = params.get("mchid");
            String outTradeNo = params.get("out_trade_no");
            String amount = params.get("amount");
            String transactionId = params.get("transaction_id");
            String refCode = params.get("refCode");
            String refMsg = params.get("refMsg");
            String successTime = params.get("success_time");

            // Bước 1: Xác thực chữ ký TRƯỚC (theo tài liệu: "Kiểm tra chữ ký cho mọi request & callback")
            boolean signatureValid = okdpayService.verifyCallbackSignature(params, OKDPAY_API_KEY);
            log.info("Signature verification result: {} for transaction: {}", signatureValid, outTradeNo);
            if (!signatureValid) {
                log.error("❌ INVALID SIGNATURE in callback. IP: {}, mchid: {}, out_trade_no: {}", 
                        clientIp, mchid, outTradeNo);
                log.error("Callback params received: {}", params);
                // Vẫn trả về "success" để OKDPAY không gửi lại, nhưng không xử lý
                return ResponseEntity.ok("success");
            }
            log.info("✅ Signature verified successfully for transaction: {}", outTradeNo);
            
            // Bước 2: Kiểm tra merchant ID
            if (mchid == null || !OKDPAY_MERCHANT_ID.equals(mchid)) {
                log.warn("Invalid or missing merchant ID in callback: {} (expected: {})", mchid, OKDPAY_MERCHANT_ID);
                return ResponseEntity.ok("success");
            }

            // Bước 3: Kiểm tra các tham số bắt buộc
            if (outTradeNo == null || outTradeNo.isEmpty()) {
                log.error("Missing out_trade_no in callback. mchid: {}", mchid);
                return ResponseEntity.ok("success");
            }

            // Bước 4: Kiểm tra IP callback (whitelist) - chỉ log warning, không block
            if (!okdpayService.isValidCallbackIp(clientIp)) {
                log.warn("Callback from unexpected IP: {} (expected: {}). Continuing processing...", 
                        clientIp, "45.58.184.162");
            }

            // Bước 5: Xử lý callback y hệt mẫu (line 1886)
            // Mẫu: if (Number(body.amount) > 0) { ... tìm payment và cộng tiền ... }
            log.info("=== Processing callback - out_trade_no: {}, amount: {}, refCode: {}, transactionId: {} ===", 
                    outTradeNo, amount, refCode, transactionId);
            
            // Check amount > 0 như mẫu (line 1886) - ĐIỀU KIỆN CHÍNH
            if (amount != null && !amount.isEmpty()) {
                try {
                    BigDecimal amountValue = new BigDecimal(amount);
                    if (amountValue.compareTo(BigDecimal.ZERO) > 0) {
                        // Amount > 0: Tìm transaction và cộng điểm (y hệt mẫu)
                        log.info("✅ Amount > 0, processing payment success. out_trade_no: {}, amount: {}", outTradeNo, amount);
                        
                        // Tìm transaction y hệt mẫu (line 1889-1897)
                        // Mẫu: transactionid: body.out_trade_no, status_payment: "Pending", money: Number(body.amount)
                        // QUAN TRỌNG: Mẫu dùng findOneAndUpdate - tìm VÀ update cùng lúc
                        // Nhưng trong Spring JPA, ta tìm trước rồi update sau
                        Transaction transaction = null;
                        
                        // Ưu tiên: Tìm với cả amount match (y hệt mẫu line 1893)
                        // Nhưng nếu không tìm thấy, thử không check amount (có thể có sai số decimal)
                        Optional<Transaction> transactionOpt = transactionRepository.findByReferenceCodeAndStatusAndAmount(
                                outTradeNo,
                                Transaction.TransactionStatus.PENDING,
                                amountValue
                        );
                        if (transactionOpt.isPresent()) {
                            transaction = transactionOpt.get();
                            log.info("✅ Found transaction with amount match. Transaction: {}, Amount: {}, ReferenceCode: {}", 
                                    transaction.getTransactionCode(), transaction.getAmount(), transaction.getReferenceCode());
                        } else {
                            // Fallback: Tìm chỉ bằng referenceCode và status PENDING (KHÔNG check amount)
                            // Vì có thể amount không match chính xác do decimal precision
                            log.warn("Transaction not found with amount match. out_trade_no: {}, amount: {}. Trying without amount check...", 
                                    outTradeNo, amountValue);
                            transactionOpt = transactionRepository.findByReferenceCode(outTradeNo);
                            if (transactionOpt.isPresent()) {
                                Transaction t = transactionOpt.get();
                                if (t.getStatus() == Transaction.TransactionStatus.PENDING) {
                                    transaction = t;
                                    log.info("✅ Found transaction by referenceCode (no amount check). Transaction: {}, Amount: {} (callback amount: {})", 
                                            transaction.getTransactionCode(), transaction.getAmount(), amountValue);
                                } else {
                                    log.warn("Transaction found but status is not PENDING: {} (status: {})", 
                                            t.getTransactionCode(), t.getStatus());
                                }
                            } else {
                                log.warn("Transaction not found by referenceCode: {}", outTradeNo);
                            }
                        }
                        
                        // Fallback 2: Thử tìm bằng transactionCode (cho backward compatibility)
                        if (transaction == null) {
                            log.warn("Transaction not found with referenceCode. Trying transactionCode...");
                            transactionOpt = transactionRepository.findByTransactionCode(outTradeNo);
                            if (transactionOpt.isPresent()) {
                                Transaction t = transactionOpt.get();
                                if (t.getStatus() == Transaction.TransactionStatus.PENDING) {
                                    transaction = t;
                                    log.info("✅ Found transaction by transactionCode. Transaction: {}, Amount: {} (callback amount: {})", 
                                            transaction.getTransactionCode(), transaction.getAmount(), amountValue);
                                } else {
                                    log.warn("Transaction found by transactionCode but status is not PENDING: {} (status: {})", 
                                            t.getTransactionCode(), t.getStatus());
                                }
                            } else {
                                log.warn("Transaction not found by transactionCode: {}", outTradeNo);
                            }
                        }
                        
                        if (transaction == null) {
                            log.error("❌❌❌ Transaction not found for out_trade_no: {} with amount: {} (tried referenceCode+amount, referenceCode, transactionCode). Cannot process callback.", 
                                    outTradeNo, amountValue);
                            log.error("Please check if transaction exists in DB with referenceCode={} and status=PENDING", outTradeNo);
                            return ResponseEntity.ok("success");
                        }
                        
                        // Double-check status PENDING
                        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
                            log.warn("Transaction {} already processed with status: {}. Skipping...", 
                                    transaction.getTransactionCode(), transaction.getStatus());
                            return ResponseEntity.ok("success");
                        }
                        
                        // Pre-load user để tránh LazyInitializationException
                        if (transaction.getUser() != null && transaction.getUser().getId() != null) {
                            transaction.getUser().getUsername(); // Trigger lazy load
                        }
                        
                        // Cộng điểm (y hệt mẫu line 1946)
                        log.info("✅ Processing payment SUCCESS callback. Transaction: {}, Amount: {}, TransactionId: {}", 
                                transaction.getTransactionCode(), amount, transactionId);
                        log.info("✅ Transaction found - Code: {}, Status: {}, Amount: {}, User: {}", 
                                transaction.getTransactionCode(), 
                                transaction.getStatus(),
                                transaction.getAmount(),
                                transaction.getUser() != null ? transaction.getUser().getUsername() : "NULL");
                        
                        try {
                            handlePaymentSuccess(transaction, transactionId, amount, successTime, refMsg);
                            log.info("✅✅✅ Payment success callback processed successfully for transaction: {}", transaction.getTransactionCode());
                        } catch (Exception e) {
                            log.error("❌❌❌ EXCEPTION in handlePaymentSuccess for transaction {}: {}", 
                                    transaction.getTransactionCode(), e.getMessage(), e);
                            // Không throw để vẫn trả về "success" cho OKDPAY
                        }
                    } else {
                        log.warn("Amount is zero or negative: {} for out_trade_no: {}", amount, outTradeNo);
                    }
                } catch (NumberFormatException e) {
                    log.error("Invalid amount format in callback: {} for out_trade_no: {}", amount, outTradeNo);
                }
            } else if (params != null && "timeout".equals(params.get("status"))) {
                // Xử lý timeout như mẫu (line 1955-1964)
                log.info("Processing timeout callback for out_trade_no: {}", outTradeNo);
                Transaction transaction = transactionRepository.findByReferenceCode(outTradeNo).orElse(null);
                if (transaction == null) {
                    transaction = transactionRepository.findByTransactionCode(outTradeNo).orElse(null);
                }
                if (transaction != null && transaction.getStatus() == Transaction.TransactionStatus.PENDING) {
                    handlePaymentCancelled(transaction, "Timeout");
                }
            } else {
                log.warn("Callback received with amount={} or status={} for out_trade_no: {}. Not processing.", 
                        amount, params.get("status"), outTradeNo);
            }

            // Bước 7: Bắt buộc trả về "success" (theo tài liệu: "Sau khi nhận callback phải trả về chuỗi: success")
            // Nếu không trả về "success", hệ thống OKDPAY sẽ gửi lại callback nhiều lần
            return ResponseEntity.ok("success");

        } catch (Exception e) {
            log.error("❌ EXCEPTION processing OKDPAY callback: {}", e.getMessage(), e);
            log.error("Exception stack trace:", e);
            // Vẫn trả về success để tránh OKDPAY retry nhiều lần
            // NHƯNG cần check logs để fix lỗi
            return ResponseEntity.ok("success");
        }
    }

    /**
     * Public method để xử lý payment success (dùng cho scheduled task)
     */
    public void processPaymentSuccess(
            Transaction transaction,
            String gatewayTransactionId,
            String amount,
            String successTime,
            String refMsg) {
        handlePaymentSuccess(transaction, gatewayTransactionId, amount, successTime, refMsg);
    }

    private void handlePaymentSuccess(
            Transaction transaction,
            String gatewayTransactionId,
            String amount,
            String successTime,
            String refMsg) {
        
        String transactionCode = transaction.getTransactionCode();
        
        try {
            // Kiểm tra transaction đã được xử lý chưa
            if (transaction.getStatus() == Transaction.TransactionStatus.APPROVED ||
                transaction.getStatus() == Transaction.TransactionStatus.COMPLETED) {
                log.info("Transaction {} already processed with status: {}", 
                        transactionCode, transaction.getStatus());
                return;
            }

            log.info("Processing payment success for transaction: {}, amount: {}", transactionCode, amount);

            // Load user từ repository để tránh LazyInitializationException
            User transactionUser = transaction.getUser();
            if (transactionUser == null || transactionUser.getId() == null) {
                log.error("Transaction {} has no user associated", transactionCode);
                throw new RuntimeException("Transaction has no user associated");
            }
            
            // Reload user từ repository để đảm bảo có latest data
            final Long userId = transactionUser.getId();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            
            log.info("Loaded user: {} for transaction: {}", user.getUsername(), transactionCode);

            // Cập nhật số tiền thực tế từ callback (y hệt mẫu: dùng amount từ callback)
            // Mẫu: Number(checkPayment.money) - dùng số tiền đã lưu trong payment
            BigDecimal depositAmount = transaction.getAmount(); // Dùng amount (số tiền nạp ban đầu)
            
            if (amount != null && !amount.isEmpty()) {
                try {
                    BigDecimal actualAmount = new BigDecimal(amount);
                    // Cập nhật amount từ callback (số tiền thực tế thanh toán)
                    transaction.setAmount(actualAmount);
                    // NetAmount = amount - fee (nếu có fee)
                    BigDecimal fee = transaction.getFee() != null ? transaction.getFee() : BigDecimal.ZERO;
                    transaction.setNetAmount(actualAmount.subtract(fee));
                    // Để tính điểm, dùng actualAmount (số tiền thực tế từ callback)
                    depositAmount = actualAmount;
                    log.info("Updated transaction - Amount: {}, Fee: {}, NetAmount: {}, DepositAmount for points: {}", 
                            actualAmount, fee, actualAmount.subtract(fee), depositAmount);
                } catch (NumberFormatException e) {
                    log.warn("Invalid amount format in callback: {}, using existing amount: {}", 
                            amount, depositAmount);
                }
            }

            // Cộng điểm cho user (1000 VND = 1 điểm) - y hệt mẫu
            // Mẫu: money: Number(checkPayment.user.money) + Number(checkPayment.money) + ...
            // Tức là: cộng số tiền nạp vào tài khoản (1000 VND = 1 điểm)
            BigDecimal pointsToAdd = depositAmount.divide(BigDecimal.valueOf(1000), 0, java.math.RoundingMode.DOWN);
            
            log.info("Calculating points - DepositAmount: {}, PointsToAdd: {}", depositAmount, pointsToAdd);
            
            if (pointsToAdd.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Points to add is zero or negative: {} for amount: {}. Transaction: {}", 
                        pointsToAdd, depositAmount, transactionCode);
                // Vẫn cập nhật transaction status dù không có điểm để cộng
                transaction.setStatus(Transaction.TransactionStatus.APPROVED);
                transaction.setGatewayTransactionId(gatewayTransactionId);
                transaction.setProcessedAt(LocalDateTime.now());
                transaction.setNote("Auto approved via OKDPAY gateway (no points added). " + 
                        (refMsg != null ? refMsg : "") + 
                        (successTime != null ? " Time: " + successTime : ""));
                transactionRepository.save(transaction);
            } else {
                // Cộng điểm vào tài khoản user
                long currentPoints = user.getPoints() != null ? user.getPoints() : 0L;
                long newPoints = currentPoints + pointsToAdd.longValue();
                user.setPoints(newPoints);
                
                log.info("Adding points. User: {}, Current: {}, Adding: {}, New: {}", 
                        user.getUsername(), currentPoints, pointsToAdd, newPoints);
                
                // Lưu user trước
                userRepository.save(user);
                log.info("User points updated successfully. User: {}, Points: {} -> {}", 
                        user.getUsername(), currentPoints, newPoints);

                // Cập nhật transaction sau khi cộng điểm thành công
                transaction.setStatus(Transaction.TransactionStatus.APPROVED);
                transaction.setGatewayTransactionId(gatewayTransactionId);
                transaction.setProcessedAt(LocalDateTime.now());
                transaction.setNote("Auto approved via OKDPAY gateway. " + 
                        (refMsg != null ? refMsg : "") + 
                        (successTime != null ? " Time: " + successTime : "") +
                        " Points added: " + pointsToAdd);
                
                transactionRepository.save(transaction);
                log.info("Transaction status updated to APPROVED. Transaction: {}", transactionCode);

                log.info("✅ Successfully processed payment: {} VND = {} points added to user {}. Points: {} -> {}. Transaction: {}", 
                        depositAmount, pointsToAdd, user.getUsername(), currentPoints, newPoints, transactionCode);
            }

            log.info("✅✅✅ Successfully processed OKDPAY payment success callback for transaction: {}", transactionCode);
            log.info("=== END PAYMENT SUCCESS HANDLING ===");

        } catch (Exception e) {
            log.error("❌❌❌ EXCEPTION in handlePaymentSuccess for transaction {}: {}", 
                    transactionCode, e.getMessage(), e);
            // Log thêm thông tin để debug
            log.error("Transaction details - ID: {}, Code: {}, Status: {}, Amount: {}, NetAmount: {}", 
                    transaction.getId(), transactionCode, transaction.getStatus(), 
                    transaction.getAmount(), transaction.getNetAmount());
            if (transaction.getUser() != null) {
                log.error("User ID: {}", transaction.getUser().getId());
            }
            log.error("Exception cause: {}", e.getCause());
            log.error("Full stack trace:");
            e.printStackTrace();
            throw e; // Re-throw để outer catch có thể log
        }
    }

    /**
     * Public method để xử lý payment cancelled (dùng cho scheduled task)
     */
    public void handlePaymentCancelled(Transaction transaction, String refMsg) {
        String transactionCode = transaction.getTransactionCode();
        try {
            if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
                log.info("Transaction {} already processed with status: {}, skipping cancellation", 
                        transactionCode, transaction.getStatus());
                return; // Đã xử lý rồi
            }

            transaction.setStatus(Transaction.TransactionStatus.CANCELLED);
            transaction.setNote("Cancelled via OKDPAY gateway. " + (refMsg != null ? refMsg : ""));
            transaction.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(transaction);

            log.info("Transaction {} cancelled via OKDPAY callback", transactionCode);

        } catch (Exception e) {
            log.error("Error handling payment cancellation for transaction {}: {}", 
                    transactionCode, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Public method để xử lý payment refunded (dùng cho scheduled task)
     */
    public void handlePaymentRefunded(Transaction transaction, String refMsg) {
        String transactionCode = transaction.getTransactionCode();
        try {
            // Nếu đã cộng điểm rồi thì cần trừ lại
            if (transaction.getStatus() == Transaction.TransactionStatus.APPROVED) {
                User transactionUser = transaction.getUser();
                if (transactionUser == null || transactionUser.getId() == null) {
                    log.error("Transaction {} has no user associated for refund", transactionCode);
                    throw new RuntimeException("Transaction has no user associated");
                }
                
                // Reload user từ repository để đảm bảo có latest data
                final Long userId = transactionUser.getId();
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
                
                BigDecimal refundAmount = transaction.getNetAmount();
                BigDecimal pointsToRefund = refundAmount.divide(BigDecimal.valueOf(1000), 0, java.math.RoundingMode.DOWN);
                
                if (pointsToRefund.compareTo(BigDecimal.ZERO) > 0) {
                    long currentPoints = user.getPoints() != null ? user.getPoints() : 0L;
                    long newPoints = Math.max(0, currentPoints - pointsToRefund.longValue());
                    user.setPoints(newPoints);
                    userRepository.save(user);

                    log.info("Refunded {} points from user {} for transaction {}. Points: {} -> {}", 
                            pointsToRefund, user.getUsername(), transactionCode, currentPoints, newPoints);
                }
            }

            transaction.setStatus(Transaction.TransactionStatus.REJECTED);
            transaction.setNote("Refunded via OKDPAY gateway. " + (refMsg != null ? refMsg : ""));
            transaction.setProcessedAt(LocalDateTime.now());
            transactionRepository.save(transaction);

            log.info("Transaction {} refunded via OKDPAY callback", transactionCode);

        } catch (Exception e) {
            log.error("Error handling payment refund for transaction {}: {}", 
                    transactionCode, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Lấy IP thực của client (xử lý proxy/load balancer)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Nếu có nhiều IP, lấy IP đầu tiên
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
