package com.xsecret.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Configuration để đảm bảo timezone của ứng dụng luôn là Asia/Ho_Chi_Minh
 */
@Configuration
@Slf4j
public class TimezoneConfig {

    private static final String VN_TIMEZONE = "Asia/Ho_Chi_Minh";

    @PostConstruct
    public void init() {
        // Set default timezone cho toàn bộ JVM
        TimeZone.setDefault(TimeZone.getTimeZone(VN_TIMEZONE));
        
        // Log để confirm timezone đã được set
        ZoneId currentZone = ZoneId.systemDefault();
        log.info("🌏 Application timezone set to: {} (ZoneId: {})", VN_TIMEZONE, currentZone);
        
        // Log current time để verify
        log.info("🕐 Current time: {}", java.time.LocalDateTime.now());
        log.info("🕐 Current time (VN): {}", java.time.LocalDateTime.now(ZoneId.of(VN_TIMEZONE)));
    }
}
