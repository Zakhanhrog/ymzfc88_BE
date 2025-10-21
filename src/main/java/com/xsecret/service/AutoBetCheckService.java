package com.xsecret.service;

import com.xsecret.event.LotteryResultPublishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


/**
 * Service để tự động check bet sau khi admin publish kết quả mới
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoBetCheckService {
    
    private final BetService betService;
    
    /**
     * Event listener để tự động check bet khi admin publish kết quả mới
     */
    @EventListener
    @Async
    public void handleLotteryResultPublished(LotteryResultPublishedEvent event) {
        log.info("========================================");
        log.info("📢 EVENT RECEIVED: Lottery Result Published!");
        log.info("   ID={}, region={}, province={}, drawDate={}", 
                event.getLotteryResultId(), event.getRegion(), event.getProvince(), event.getDrawDate());
        log.info("========================================");
        
        // Debug: Log thread info
        log.info("🔍 DEBUG Event listener running on thread: {}", Thread.currentThread().getName());
        
        try {
            log.info("⏰ Waiting 10 seconds before checking bets...");
            Thread.sleep(10000);
            
            log.info("🚀 Starting auto bet check after admin publish for date: {}", event.getDrawDate());
            
            // Check bet cho ngày cụ thể
            betService.checkBetResultsForDate(event.getDrawDate());
            
            log.info("✅ Auto bet check completed successfully for date: {}", event.getDrawDate());
            
        } catch (InterruptedException e) {
            log.error("❌ Auto bet check was interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("❌ Error during auto bet check after publish", e);
        }
    }
    
    /**
     * Check bet cho một ngày cụ thể (dùng khi admin publish kết quả cho ngày trước)
     */
    @Async
    public void scheduleBetCheckForSpecificDate(String drawDateStr) {
        log.info("Scheduling auto bet check for specific date: {}", drawDateStr);
        
        try {
            // Đợi 5 giây
            Thread.sleep(5000);
            
            log.info("Starting auto bet check for date: {}", drawDateStr);
            
            // Check bet cho ngày cụ thể
            betService.checkBetResultsForDate(drawDateStr);
            
            log.info("Auto bet check for date {} completed successfully", drawDateStr);
            
        } catch (InterruptedException e) {
            log.error("Auto bet check was interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error during auto bet check for date {}", drawDateStr, e);
        }
    }
}
