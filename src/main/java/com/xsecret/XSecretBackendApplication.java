package com.xsecret;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class XSecretBackendApplication {

    private static final String VN_TIMEZONE = "Asia/Ho_Chi_Minh";

    static {
        // Set timezone TRƯỚC KHI Spring Boot khởi động
        // Đảm bảo tất cả components dùng đúng timezone từ đầu
        try {
            // Force set system property TRƯỚC
            System.setProperty("user.timezone", VN_TIMEZONE);
            
            // Set default timezone
            TimeZone.setDefault(TimeZone.getTimeZone(VN_TIMEZONE));
            
            // Force set ZoneId system default (Java 8+)
            System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
            
            // Verify
            ZoneId systemZone = ZoneId.systemDefault();
            if (!VN_TIMEZONE.equals(systemZone.toString())) {
                System.err.println("⚠️ WARNING: System timezone is " + systemZone + " but expected " + VN_TIMEZONE);
                System.err.println("⚠️ Please set JVM parameter: -Duser.timezone=Asia/Ho_Chi_Minh");
            }
        } catch (Exception e) {
            System.err.println("❌ Error setting timezone: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Double check timezone trước khi start Spring
        TimeZone.setDefault(TimeZone.getTimeZone(VN_TIMEZONE));
        System.setProperty("user.timezone", VN_TIMEZONE);
        
        // Log timezone info
        ZoneId currentZone = ZoneId.systemDefault();
        System.out.println("🌏 Application starting with timezone: " + currentZone);
        System.out.println("🌏 System property user.timezone: " + System.getProperty("user.timezone"));
        System.out.println("🌏 TimeZone.getDefault(): " + TimeZone.getDefault().getID());
        
        SpringApplication.run(XSecretBackendApplication.class, args);
    }

}
