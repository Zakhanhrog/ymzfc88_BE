package com.xsecret.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service để chạy các task tự động theo lịch
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTaskService {

    private final BetService betService;

    /**
     * Tự động check kết quả bet mỗi phút
     * Cron expression: 0 * * * * ? = mỗi phút (giây 0)
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkBetResultsAutomatically() {
        try {
            log.info("🔄 Scheduled task: Starting automatic bet result check...");
            betService.checkBetResults();
            log.info("✅ Scheduled task: Bet result check completed successfully");
        } catch (Exception e) {
            log.error("❌ Scheduled task: Error during automatic bet result check", e);
        }
    }

    /**
     * Log status mỗi 5 phút để theo dõi
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void logSystemStatus() {
        log.info("📊 System Status: Scheduled tasks are running normally");
    }
}
