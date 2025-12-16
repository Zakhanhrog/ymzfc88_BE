package com.xsecret.controller;

import com.xsecret.entity.Transaction;
import com.xsecret.entity.User;
import com.xsecret.repository.TransactionRepository;
import com.xsecret.service.OkdpayService;
import com.xsecret.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/payments/okdpay")
@RequiredArgsConstructor
@Slf4j
public class OkdpayCallbackController {

    private final OkdpayService okdpayService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    
    @Value("${app.okdpay.merchant-id:9182}")
    private String okdpayMerchantId;
    
    @Value("${app.okdpay.api-key}")
    private String okdpayApiKey;

    /**
     * Callback endpoint để nhận thông báo từ OKDPAY
     * OKDPAY sẽ gửi POST request với form data
     */
    @PostMapping("/callback")
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
            boolean signatureValid = okdpayService.verifyCallbackSignature(params, okdpayApiKey);
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
            if (mchid == null || !okdpayMerchantId.equals(mchid)) {
                log.warn("Invalid or missing merchant ID in callback: {} (expected: {})", mchid, okdpayMerchantId);
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

            // Bước 5: Tìm transaction theo out_trade_no
            Transaction transaction = transactionRepository.findByTransactionCode(outTradeNo)
                    .orElse(null);

            if (transaction == null) {
                log.error("Transaction not found for out_trade_no: {}. mchid: {}", outTradeNo, mchid);
                return ResponseEntity.ok("success");
            }
            
            // Pre-load user để tránh LazyInitializationException
            if (transaction.getUser() != null && transaction.getUser().getId() != null) {
                transaction.getUser().getUsername(); // Trigger lazy load
            }

            // Bước 6: Xử lý callback dựa trên refCode (theo tài liệu OKDPAY)
            // refCode: 1=Chưa xử lý, 2=Đã thanh toán (Thành công), 3=Hủy, 4=Hoàn
            log.info("Processing callback with refCode: {} for transaction: {}", refCode, outTradeNo);
            if ("2".equals(refCode)) {
                // refCode = 2: Đã thanh toán thành công (theo tài liệu)
                log.info("✅ Processing payment SUCCESS callback. Transaction: {}, Amount: {}, TransactionId: {}", 
                        outTradeNo, amount, transactionId);
                handlePaymentSuccess(transaction, transactionId, amount, successTime, refMsg);
                log.info("✅ Payment success callback processed successfully for transaction: {}", outTradeNo);
            } else if ("3".equals(refCode)) {
                // refCode = 3: Hủy
                log.info("Processing payment cancellation callback. Transaction: {}", outTradeNo);
                handlePaymentCancelled(transaction, refMsg);
            } else if ("4".equals(refCode)) {
                // refCode = 4: Hoàn tiền
                log.info("Processing payment refund callback. Transaction: {}", outTradeNo);
                handlePaymentRefunded(transaction, refMsg);
            } else {
                // refCode = 1 hoặc giá trị khác: Chưa xử lý hoặc không xác định
                log.info("Callback received with refCode={} (1=Chưa xử lý or unknown). Transaction: {}, refMsg: {}", 
                        refCode, outTradeNo, refMsg);
                // Không xử lý, chỉ log
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

            // Cập nhật số tiền thực tế nếu có
            BigDecimal depositAmount = transaction.getNetAmount();
            if (amount != null && !amount.isEmpty()) {
                try {
                    BigDecimal actualAmount = new BigDecimal(amount);
                    transaction.setAmount(actualAmount);
                    // Tính lại net amount (có thể không có fee cho auto deposit)
                    transaction.setNetAmount(actualAmount);
                    depositAmount = actualAmount;
                    log.info("Updated transaction amount to: {}", actualAmount);
                } catch (NumberFormatException e) {
                    log.warn("Invalid amount format in callback: {}, using existing netAmount: {}", 
                            amount, depositAmount);
                }
            }

            // Cộng điểm cho user (1000 VND = 1 điểm)
            // Quan trọng: Luôn cộng điểm khi refCode = 2 (Thanh toán thành công)
            BigDecimal pointsToAdd = depositAmount.divide(BigDecimal.valueOf(1000), 0, java.math.RoundingMode.DOWN);
            
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

    private void handlePaymentCancelled(Transaction transaction, String refMsg) {
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

    private void handlePaymentRefunded(Transaction transaction, String refMsg) {
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
