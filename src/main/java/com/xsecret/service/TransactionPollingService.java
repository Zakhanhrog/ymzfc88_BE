package com.xsecret.service;

import com.xsecret.controller.OkdpayCallbackController;
import com.xsecret.entity.Transaction;
import com.xsecret.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Service để check liên tục trạng thái transaction OKDPAY cho đến khi thành công hoặc timeout
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionPollingService {
    
    private final TransactionRepository transactionRepository;
    private final OkdpayService okdpayService;
    private final OkdpayCallbackController okdpayCallbackController;
    
    // Thời gian check mỗi lần (giây)
    private static final int POLLING_INTERVAL_SECONDS = 5;
    
    // Timeout tối đa (phút) - sau thời gian này sẽ dừng check
    private static final int MAX_TIMEOUT_MINUTES = 10;
    
    /**
     * Bắt đầu check liên tục transaction cho đến khi thành công hoặc timeout
     * Chạy async để không block request
     */
    @Async
    public void startPolling(Long transactionId) {
        log.info("🔄 Starting polling for transaction ID: {}", transactionId);
        
        LocalDateTime startTime = LocalDateTime.now();
        int attemptCount = 0;
        
        while (true) {
            try {
                attemptCount++;
                
                // Kiểm tra timeout
                if (startTime.plusMinutes(MAX_TIMEOUT_MINUTES).isBefore(LocalDateTime.now())) {
                    log.warn("⏰ Polling timeout for transaction ID: {} after {} minutes", transactionId, MAX_TIMEOUT_MINUTES);
                    break;
                }
                
                // Lấy transaction từ DB (reload để có status mới nhất)
                Transaction transaction = transactionRepository.findById(transactionId)
                        .orElse(null);
                
                if (transaction == null) {
                    log.warn("⚠️ Transaction ID: {} not found, stopping polling", transactionId);
                    break;
                }
                
                // Nếu transaction đã được xử lý (không còn PENDING) thì dừng
                if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
                    log.info("✅ Transaction ID: {} status changed to {}, stopping polling. Attempts: {}", 
                            transactionId, transaction.getStatus(), attemptCount);
                    break;
                }
                
                // Kiểm tra referenceCode
                String referenceCode = transaction.getReferenceCode();
                if (referenceCode == null || referenceCode.isEmpty()) {
                    log.warn("⚠️ Transaction ID: {} has no referenceCode, stopping polling", transactionId);
                    break;
                }
                
                // Query OKDPAY để check status
                log.debug("🔍 Polling attempt #{} for transaction ID: {}, ReferenceCode: {}", 
                        attemptCount, transactionId, referenceCode);
                
                OkdpayService.OkdpayQueryOrderResponse queryResponse = okdpayService.queryOrder(referenceCode);
                
                if (!"success".equalsIgnoreCase(queryResponse.getStatus())) {
                    log.debug("⚠️ Query order failed for transaction ID: {}: {}", transactionId, queryResponse.getMsg());
                    // Tiếp tục check lần sau
                } else {
                    String refCode = queryResponse.getRefCode();
                    
                    if ("2".equals(refCode)) {
                        // Đã thanh toán thành công
                        log.info("✅ Transaction ID: {} is PAID (refCode=2). Amount: {}, TransactionId: {}. Processing payment success...", 
                                transactionId, queryResponse.getAmount(), queryResponse.getTransactionId());
                        
                        // Gọi handlePaymentSuccess để cộng điểm
                        okdpayCallbackController.processPaymentSuccess(
                                transaction,
                                queryResponse.getTransactionId(),
                                queryResponse.getAmount(),
                                queryResponse.getSuccessTime(),
                                queryResponse.getRefMsg()
                        );
                        
                        log.info("✅ Successfully processed transaction ID: {} - points should be added. Total attempts: {}", 
                                transactionId, attemptCount);
                        break; // Dừng polling
                        
                    } else if ("3".equals(refCode)) {
                        // Đã hủy
                        log.info("❌ Transaction ID: {} is cancelled (refCode=3)", transactionId);
                        okdpayCallbackController.handlePaymentCancelled(transaction, queryResponse.getRefMsg());
                        break; // Dừng polling
                        
                    } else if ("4".equals(refCode)) {
                        // Đã hoàn tiền
                        log.info("🔄 Transaction ID: {} is refunded (refCode=4)", transactionId);
                        okdpayCallbackController.handlePaymentRefunded(transaction, queryResponse.getRefMsg());
                        break; // Dừng polling
                        
                    } else {
                        // Vẫn pending, tiếp tục check
                        log.debug("⏳ Transaction ID: {} still pending (refCode={}), will check again in {} seconds", 
                                transactionId, refCode, POLLING_INTERVAL_SECONDS);
                    }
                }
                
                // Đợi trước khi check lần tiếp theo
                try {
                    TimeUnit.SECONDS.sleep(POLLING_INTERVAL_SECONDS);
                } catch (InterruptedException e) {
                    log.warn("⚠️ Polling interrupted for transaction ID: {}", transactionId);
                    Thread.currentThread().interrupt();
                    break;
                }
                
            } catch (Exception e) {
                log.error("❌ Error while polling transaction ID: {}: {}", transactionId, e.getMessage(), e);
                // Tiếp tục check lần sau, không dừng vì lỗi
                try {
                    TimeUnit.SECONDS.sleep(POLLING_INTERVAL_SECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.info("🏁 Polling stopped for transaction ID: {}. Total attempts: {}", transactionId, attemptCount);
    }
}

