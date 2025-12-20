package com.xsecret.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
    import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

/**
 * Configuration để đảm bảo timezone của ứng dụng luôn là Asia/Ho_Chi_Minh
 * Set timezone TRƯỚC khi Spring Boot khởi động để đảm bảo tất cả components dùng đúng timezone
 */
@Component
@Slf4j
@Order(1) // Chạy sớm nhất
public class TimezoneConfig implements ApplicationListener<ContextRefreshedEvent> {

    private static final String VN_TIMEZONE = "Asia/Ho_Chi_Minh";
    
    // Static block để set timezone NGAY KHI CLASS ĐƯỢC LOAD (trước cả @PostConstruct)
    static {
        // Set default timezone cho toàn bộ JVM NGAY LẬP TỨC
        try {
            System.setProperty("user.timezone", VN_TIMEZONE);
            TimeZone.setDefault(TimeZone.getTimeZone(VN_TIMEZONE));
        } catch (Exception e) {
            System.err.println("❌ Error in static block setting timezone: " + e.getMessage());
        }
    }

    @PostConstruct
    public void init() {
        // Force set timezone lại (backup check)
        System.setProperty("user.timezone", VN_TIMEZONE);
        TimeZone.setDefault(TimeZone.getTimeZone(VN_TIMEZONE));
        
        logTimezoneInfo();
    }

    @Override
    public void onApplicationEvent(@org.springframework.lang.NonNull ContextRefreshedEvent event) {
        // Force set lại khi context refreshed (đảm bảo chắc chắn)
        System.setProperty("user.timezone", VN_TIMEZONE);
        TimeZone.setDefault(TimeZone.getTimeZone(VN_TIMEZONE));
        
        log.info("🔄 Context refreshed - Timezone re-verified");
        logTimezoneInfo();
    }

    private void logTimezoneInfo() {
        // Log để confirm timezone đã được set
        ZoneId currentZone = ZoneId.systemDefault();
        String systemTimezone = System.getProperty("user.timezone");
        TimeZone defaultTZ = TimeZone.getDefault();
        
        log.info("🌏 ========== TIMEZONE CONFIGURATION ==========");
        log.info("🌏 Target timezone: {}", VN_TIMEZONE);
        log.info("🌏 System default ZoneId: {}", currentZone);
        log.info("🌏 System property user.timezone: {}", systemTimezone);
        log.info("🌏 TimeZone.getDefault(): {} (ID: {})", defaultTZ.getDisplayName(), defaultTZ.getID());
        log.info("🌏 TimeZone offset: {} hours", defaultTZ.getRawOffset() / (1000 * 60 * 60));
        
        // Log current time để verify
        java.time.LocalDateTime nowSystem = java.time.LocalDateTime.now();
        java.time.LocalDateTime nowVN = java.time.LocalDateTime.now(ZoneId.of(VN_TIMEZONE));
        ZonedDateTime nowZoned = ZonedDateTime.now(ZoneId.of(VN_TIMEZONE));
        
        log.info("🕐 Current time (system default): {}", nowSystem);
        log.info("🕐 Current time (VN explicit): {}", nowVN);
        log.info("🕐 Current time (VN zoned): {}", nowZoned);
        
        // Verify timezone đúng chưa
        boolean isCorrect = VN_TIMEZONE.equals(currentZone.toString()) || 
                           "Asia/Ho_Chi_Minh".equals(currentZone.toString()) ||
                           VN_TIMEZONE.equals(defaultTZ.getID());
        
        if (!isCorrect) {
            log.error("❌❌❌ CRITICAL: System timezone ({}) không khớp với target timezone ({})!", 
                    currentZone, VN_TIMEZONE);
            log.error("❌ TimeZone.getDefault().getID(): {}", defaultTZ.getID());
            log.error("❌ VUI LÒNG SET JVM PARAMETER TRONG AAPANEL: -Duser.timezone=Asia/Ho_Chi_Minh");
            log.error("❌ Hoặc trong startup command thêm: java -Duser.timezone=Asia/Ho_Chi_Minh -jar ...");
        } else {
            log.info("✅ Timezone configuration OK!");
        }
        log.info("🌏 ============================================");
    }
}
