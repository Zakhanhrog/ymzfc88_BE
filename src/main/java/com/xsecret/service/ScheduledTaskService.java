package com.xsecret.service;

import com.xsecret.service.lottery.LotteryResultAutoImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
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
            betService.checkBetResults();
            log.info("✅ Scheduled task [19:00]: Backup bet result check completed successfully");
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
                    betService.checkBetResults();
                    log.info("✅ Bet check completed immediately after Miền Bắc import");
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
                    betService.checkBetResults();
                    log.info("✅ Bet check completed immediately after provinces import");
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
}
