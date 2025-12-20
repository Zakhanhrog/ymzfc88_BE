package com.xsecret.service;

import com.xsecret.controller.OkdpayCallbackController;
import com.xsecret.entity.Transaction;
import com.xsecret.repository.TransactionRepository;
import com.xsecret.service.lottery.LotteryResultAutoImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service để chạy các task tự động theo lịch
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final BetService betService;
    private final LotteryResultAutoImportService lotteryResultAutoImportService;
    private final LotteryResultService lotteryResultService;
    private final GameRefundService gameRefundService;
    private final DailyLossRefundService dailyLossRefundService;
    private final OkdpayService okdpayService;
    private final TransactionRepository transactionRepository;
    private final OkdpayCallbackController okdpayCallbackController;
    
    // Timezone Vietnam
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    
    // Track import success state by date to avoid duplicate imports
    private final Map<String, Boolean> mienBacImportSuccess = new HashMap<>();
    private final Map<String, Boolean> provinceImportSuccess = new HashMap<>();

    /**
     * REMOVED: Check bet results automatically - chạy lúc 18:30 mỗi ngày
     * Logic mới: Check bet ngay khi có kết quả trong thời gian auto import (18:20-19:00)
     * Chỉ giữ 19:00 làm backup check
     */
    // @Scheduled(cron = "0 30 18 * * ?", zone = "Asia/Ho_Chi_Minh")
    // public void checkBetResultsAt1830() {
    //     // REMOVED: Logic check bet đã chuyển vào auto import
    // }

    /**
     * Check bet results automatically - chạy lúc 19:00 mỗi ngày
     * Backup check sau khi auto import mở rộng thời gian (18:20-19:00)
     */
    @Scheduled(cron = "0 0 19 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void checkBetResultsAt1900() {
        try {
            log.info("🔄 Scheduled task [19:00]: Starting backup automatic bet result check...");
            
            // Kiểm tra có kết quả hôm nay trước khi check bet
            String today = LocalDate.now(VN_ZONE).toString();
            boolean hasMienBacResult = lotteryResultService.hasPublishedResult("mienBac", null, today);
            boolean hasProvinceResult = lotteryResultService.hasPublishedResult("mienTrungNam", null, today);
            
            if (hasMienBacResult || hasProvinceResult) {
                betService.checkBetResults();
                log.info("✅ Scheduled task [19:00]: Backup bet result check completed successfully");
            } else {
                log.warn("⚠️ Scheduled task [19:00]: No lottery results available, skipping bet check");
            }
        } catch (Exception e) {
            log.error("❌ Scheduled task [19:00]: Error during backup automatic bet result check", e);
        }
    }

    /**
     * Auto import Miền Bắc - chạy mỗi phút từ 18:20 đến 19:00
     * Retry logic: nếu fail thì retry mỗi 1 phút cho đến 19:00
     * Mở rộng thời gian để đợi API có dữ liệu
     */
    @Scheduled(cron = "0 */1 18 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void autoImportMienBac() {
        LocalTime now = LocalTime.now(VN_ZONE);
        String today = LocalDate.now(VN_ZONE).toString();
        
        // Chỉ chạy từ 18:20 đến 19:00 (mở rộng thêm 10 phút)
        if (now.isBefore(LocalTime.of(18, 20)) || now.isAfter(LocalTime.of(19, 0))) {
            return;
        }
        
        // Nếu đã import thành công hôm nay rồi thì skip
        if (Boolean.TRUE.equals(mienBacImportSuccess.get(today))) {
            return;
        }
        
        try {
            log.info("🎲 Auto importing Miền Bắc lottery result at {}", now);
            lotteryResultAutoImportService.autoImportMienBac();
            
            // Mark success
            mienBacImportSuccess.put(today, true);
            log.info("✅ Miền Bắc import SUCCESS at {}", now);
            
            // 🎯 CHECK BET NGAY KHI IMPORT THÀNH CÔNG (18:30-19:00)
            if (now.isAfter(LocalTime.of(18, 29))) {
                log.info("🚀 Miền Bắc import thành công, đang check bet ngay lập tức...");
                try {
                    // Kiểm tra có kết quả hôm nay trước khi check bet
                    String todayResult = LocalDate.now(VN_ZONE).toString();
                    boolean hasTodayResult = lotteryResultService.hasPublishedResult("mienBac", null, todayResult);
                    if (hasTodayResult) {
                        betService.checkBetResults();
                        log.info("✅ Bet check completed immediately after Miền Bắc import");
                    } else {
                        log.warn("⚠️ Miền Bắc import thành công nhưng chưa có kết quả PUBLISHED, skip check bet");
                    }
                } catch (Exception betError) {
                    log.error("❌ Error checking bets after Miền Bắc import: {}", betError.getMessage());
                }
            }
            
            // Clean up old dates (keep only today)
            mienBacImportSuccess.keySet().removeIf(date -> !date.equals(today));
            
        } catch (Exception e) {
            log.error("❌ Miền Bắc import failed at {}: {}", now, e.getMessage());
            
            // Nếu đã hết thời gian retry (19:00) thì alert
            if (now.isAfter(LocalTime.of(18, 59))) {
                log.error("🚨 ALERT: Miền Bắc import FAILED after all retries! Please check manually.");
            }
        }
    }

    /**
     * Auto import Miền Trung/Nam - chạy mỗi phút từ 17:20 đến 17:50
     * Retry logic: nếu fail thì retry mỗi 1 phút cho đến 17:50
     */
    @Scheduled(cron = "0 */1 17 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void autoImportProvinces() {
        LocalTime now = LocalTime.now(VN_ZONE);
        String today = LocalDate.now(VN_ZONE).toString();
        
        // Chỉ chạy từ 17:20 đến 17:50
        if (now.isBefore(LocalTime.of(17, 20)) || now.isAfter(LocalTime.of(17, 50))) {
            return;
        }
        
        // Nếu đã import thành công hôm nay rồi thì skip
        if (Boolean.TRUE.equals(provinceImportSuccess.get(today))) {
            return;
        }
        
        try {
            log.info("🎲 Auto importing all provinces lottery results at {}", now);
            lotteryResultAutoImportService.autoImportAllProvinces();
            
            // Mark success
            provinceImportSuccess.put(today, true);
            log.info("✅ All provinces import SUCCESS at {}", now);
            
            // 🎯 CHECK BET NGAY KHI IMPORT THÀNH CÔNG (17:30-18:00)
            if (now.isAfter(LocalTime.of(17, 29))) {
                log.info("🚀 Provinces import thành công, đang check bet ngay lập tức...");
                try {
                    // Kiểm tra có kết quả hôm nay trước khi check bet
                    String todayResult = LocalDate.now(VN_ZONE).toString();
                    boolean hasTodayResult = lotteryResultService.hasPublishedResult("mienTrungNam", null, todayResult);
                    if (hasTodayResult) {
                        betService.checkBetResults();
                        log.info("✅ Bet check completed immediately after provinces import");
                    } else {
                        log.warn("⚠️ Provinces import thành công nhưng chưa có kết quả PUBLISHED, skip check bet");
                    }
                } catch (Exception betError) {
                    log.error("❌ Error checking bets after provinces import: {}", betError.getMessage());
                }
            }
            
            // Clean up old dates (keep only today)
            provinceImportSuccess.keySet().removeIf(date -> !date.equals(today));
            
        } catch (Exception e) {
            log.error("❌ Provinces import failed at {}: {}", now, e.getMessage());
            
            // Nếu đã hết thời gian retry (17:50) thì alert
            if (now.isAfter(LocalTime.of(17, 49))) {
                log.error("🚨 ALERT: Provinces import FAILED after all retries! Please check manually.");
            }
        }
    }

    /**
     * Auto cancel expired bets - chạy lúc 20:00 mỗi ngày
     * Logic:
     * 1. Kiểm tra xem có kết quả trong DB chưa
     * 2. Nếu có → Check bet trước, sau đó cancel bet còn PENDING
     * 3. Nếu không → Cancel tất cả bet PENDING và hoàn tiền
     */
    @Scheduled(cron = "0 0 20 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void autoCancelExpiredBets() {
        try {
            log.info("🔄 Scheduled task [20:00]: Starting auto cancel expired bets...");
            
            String today = LocalDate.now(VN_ZONE).toString();
            
            // Kiểm tra xem có kết quả Miền Bắc trong DB chưa
            boolean hasMienBacResult = lotteryResultService.hasPublishedResult("mienBac", null, today);
            
            if (hasMienBacResult) {
                log.info("✅ Found Miền Bắc result in DB. Checking bets before cancel...");
                
                // Check bet trước khi cancel (có thể có kết quả mới được import)
                try {
                    betService.checkBetResults();
                    log.info("✅ Bet check completed before auto cancel");
                } catch (Exception e) {
                    log.error("❌ Error checking bets before auto cancel: {}", e.getMessage());
                }
                
                // Sau đó cancel các bet vẫn còn PENDING
                int cancelledCount = betService.autoCancelExpiredBets();
                log.info("✅ Auto cancelled {} remaining pending bets (after check)", cancelledCount);
            } else {
                log.warn("⚠️ No Miền Bắc result found in DB. Cancelling all pending bets...");
                
                // Không có kết quả → Cancel tất cả
                int cancelledCount = betService.autoCancelExpiredBets();
                log.info("✅ Auto cancelled {} pending bets (no result available)", cancelledCount);
            }
            
        } catch (Exception e) {
            log.error("❌ Scheduled task [20:00]: Error during auto cancel expired bets", e);
        }
    }

    /**
     * Reset import success flags vào đầu ngày mới (00:01)
     */
    @Scheduled(cron = "0 1 0 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void resetImportFlags() {
        mienBacImportSuccess.clear();
        provinceImportSuccess.clear();
        log.info("🔄 Reset lottery import flags for new day");
    }

    /**
     * Log status mỗi 5 phút để theo dõi
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void logSystemStatus() {
        log.info("📊 System Status: Scheduled tasks are running normally");
    }

    /**
     * Xử lý hoàn trả cược theo lịch mỗi phút
     */
    @Scheduled(cron = "0 */1 * * * ?", zone = "Asia/Ho_Chi_Minh")
    public void processScheduledRefunds() {
        try {
            gameRefundService.processDueRefunds();
        } catch (Exception ex) {
            log.error("❌ Error while processing scheduled game refunds", ex);
        }
    }

    /**
     * Tính toán daily loss refund cho ngày hôm qua - chạy lúc 00:01 mỗi ngày
     * Tính toán cho ngày hôm qua (00:00 - 23:59)
     */
    @Scheduled(cron = "0 1 0 * * ?", zone = "Asia/Ho_Chi_Minh")
    public void calculateDailyLossRefunds() {
        try {
            log.info("🔄 Scheduled task [00:01]: Starting daily loss refund calculation for yesterday...");
            java.time.LocalDate yesterday = java.time.LocalDate.now(VN_ZONE).minusDays(1);
            dailyLossRefundService.calculateAndCreateDailyLossRefunds(yesterday);
            log.info("✅ Scheduled task [00:01]: Daily loss refund calculation completed for date: {}", yesterday);
        } catch (Exception ex) {
            log.error("❌ Scheduled task [00:01]: Error during daily loss refund calculation", ex);
        }
    }

    /**
     * Xử lý hoàn trả daily loss refund đã đến hạn - chạy mỗi phút
     */
    @Scheduled(cron = "0 */1 * * * ?", zone = "Asia/Ho_Chi_Minh")
    public void processDueDailyLossRefunds() {
        try {
            dailyLossRefundService.processDueDailyLossRefunds();
        } catch (Exception ex) {
            log.error("❌ Error while processing scheduled daily loss refunds", ex);
        }
    }

    /**
     * Tự động check lại các transaction PENDING (auto deposit) từ OKDPAY
     * Chạy mỗi 2 phút để check lại transaction status
     * Dùng khi callback không đến được (ví dụ: chạy local)
     * 
     * ĐÃ TẮT: Thay bằng TransactionPollingService - check liên tục cho từng transaction ngay sau khi tạo
     */
    // @Scheduled(cron = "0 */2 * * * ?", zone = "Asia/Ho_Chi_Minh")
    public void checkPendingOkdpayTransactions() {
        try {
            log.info("🔄 Checking pending OKDPAY transactions...");
            
            // Tìm tất cả transaction PENDING
            List<com.xsecret.entity.Transaction> pendingTransactions = transactionRepository.findByStatusOrderByCreatedAtAsc(
                    com.xsecret.entity.Transaction.TransactionStatus.PENDING
            );
            
            log.info("Found {} total PENDING transactions", pendingTransactions.size());
            
            int checkedCount = 0;
            int successCount = 0;
            int skippedNotAutoDeposit = 0;
            int skippedTooNew = 0;
            int skippedTooOld = 0;
            int skippedNoReferenceCode = 0;
            
            for (com.xsecret.entity.Transaction transaction : pendingTransactions) {
                // Check auto deposit transactions HOẶC có gateway_type = OKDPAY
                // Vì có thể transaction cũ không có isAutoDeposit set nhưng vẫn là OKDPAY
                boolean isAutoDeposit = transaction.getIsAutoDeposit() != null && transaction.getIsAutoDeposit();
                boolean isOkdpay = "OKDPAY".equalsIgnoreCase(transaction.getGatewayType());
                String referenceCode = transaction.getReferenceCode();
                boolean hasReferenceCode = referenceCode != null && !referenceCode.isEmpty();
                
                // Chỉ check nếu là auto deposit HOẶC có gateway_type = OKDPAY
                // Nếu không phải auto deposit và không phải OKDPAY thì skip
                if (!isAutoDeposit && !isOkdpay) {
                    skippedNotAutoDeposit++;
                    continue;
                }
                
                // Nếu là OKDPAY nhưng không có referenceCode thì skip
                if (isOkdpay && !hasReferenceCode) {
                    skippedNoReferenceCode++;
                    log.warn("Skipping OKDPAY transaction {} - no referenceCode", transaction.getTransactionCode());
                    continue;
                }
                
                // Chỉ check transactions cũ hơn 30 giây (tránh check ngay sau khi tạo)
                // Giảm từ 1 phút xuống 30 giây để check nhanh hơn
                if (transaction.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusSeconds(30))) {
                    skippedTooNew++;
                    log.debug("Skipping transaction {} - too new (created: {}, now: {})", 
                            transaction.getTransactionCode(), transaction.getCreatedAt(), java.time.LocalDateTime.now());
                    continue;
                }
                
                // TẠM THỜI: Bỏ giới hạn thời gian để check toàn bộ giao dịch
                // Vì đang lệch giờ, sau sẽ chỉnh lại
                // if (transaction.getCreatedAt().isBefore(java.time.LocalDateTime.now().minusHours(24))) {
                //     skippedTooOld++;
                //     log.debug("Skipping transaction {} - too old (created: {}, now: {})", 
                //             transaction.getTransactionCode(), transaction.getCreatedAt(), java.time.LocalDateTime.now());
                //     continue;
                // }
                
                try {
                    checkedCount++;
                    log.info("🔍 Checking transaction: {}, ReferenceCode: {}, CreatedAt: {}, GatewayType: {}, IsAutoDeposit: {}", 
                            transaction.getTransactionCode(), referenceCode, transaction.getCreatedAt(), 
                            transaction.getGatewayType(), transaction.getIsAutoDeposit());
                    
                    // Query order status từ OKDPAY
                    OkdpayService.OkdpayQueryOrderResponse queryResponse = okdpayService.queryOrder(referenceCode);
                    
                    log.info("📥 OKDPAY Response for transaction {}: status={}, refCode={}, msg={}, transactionId={}, amount={}", 
                            transaction.getTransactionCode(), queryResponse.getStatus(), queryResponse.getRefCode(), 
                            queryResponse.getMsg(), queryResponse.getTransactionId(), queryResponse.getAmount());
                    
                    if (!"success".equalsIgnoreCase(queryResponse.getStatus())) {
                        log.warn("⚠️ Query order failed for transaction {}: {}", transaction.getTransactionCode(), queryResponse.getMsg());
                        continue;
                    }
                    
                    // Check refCode: 2 = đã thanh toán
                    String refCode = queryResponse.getRefCode();
                    if ("2".equals(refCode)) {
                        log.info("✅ Transaction {} is PAID (refCode=2). Amount: {}, TransactionId: {}. Processing payment success...", 
                                transaction.getTransactionCode(), queryResponse.getAmount(), queryResponse.getTransactionId());
                        
                        // Gọi handlePaymentSuccess để cộng điểm
                        okdpayCallbackController.processPaymentSuccess(
                                transaction,
                                queryResponse.getTransactionId(),
                                queryResponse.getAmount(),
                                queryResponse.getSuccessTime(),
                                queryResponse.getRefMsg()
                        );
                        successCount++;
                        log.info("✅ Successfully processed transaction {} - points should be added", transaction.getTransactionCode());
                    } else if ("3".equals(refCode)) {
                        log.info("❌ Transaction {} is cancelled (refCode=3)", transaction.getTransactionCode());
                        okdpayCallbackController.handlePaymentCancelled(transaction, queryResponse.getRefMsg());
                    } else if ("4".equals(refCode)) {
                        log.info("🔄 Transaction {} is refunded (refCode=4)", transaction.getTransactionCode());
                        okdpayCallbackController.handlePaymentRefunded(transaction, queryResponse.getRefMsg());
                    } else {
                        log.info("⏳ Transaction {} still pending (refCode={}, expected 2 for paid)", 
                                transaction.getTransactionCode(), refCode);
                    }
                    
                } catch (Exception e) {
                    log.error("❌ Error checking transaction {}: {}", transaction.getTransactionCode(), e.getMessage(), e);
                }
            }
            
            // Log tổng kết
            log.info("✅ Checked {} pending OKDPAY transactions, {} processed successfully", checkedCount, successCount);
            if (skippedNotAutoDeposit > 0 || skippedTooNew > 0 || skippedTooOld > 0 || skippedNoReferenceCode > 0) {
                log.info("Skipped: {} not auto deposit, {} too new, {} too old, {} no referenceCode", 
                        skippedNotAutoDeposit, skippedTooNew, skippedTooOld, skippedNoReferenceCode);
            }
            
        } catch (Exception ex) {
            log.error("❌ Error while checking pending OKDPAY transactions", ex);
        }
    }
}
