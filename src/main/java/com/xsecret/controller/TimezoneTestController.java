package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller để test timezone - chỉ dùng cho debug
 */
@RestController
@RequestMapping("/api/test")
@Slf4j
public class TimezoneTestController {

    @GetMapping("/timezone")
    public ApiResponse<Map<String, Object>> getTimezoneInfo() {
        Map<String, Object> timezoneInfo = new HashMap<>();
        
        // Current time in different timezones
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nowVN = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime nowUTC = LocalDateTime.now(ZoneId.of("UTC"));
        
        timezoneInfo.put("systemDefault", now.toString());
        timezoneInfo.put("vietnam", nowVN.toString());
        timezoneInfo.put("utc", nowUTC.toString());
        
        // Zone info
        timezoneInfo.put("systemZone", ZoneId.systemDefault().toString());
        timezoneInfo.put("vnZone", ZoneId.of("Asia/Ho_Chi_Minh").toString());
        
        // Formatted times
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        timezoneInfo.put("systemFormatted", now.format(formatter));
        timezoneInfo.put("vnFormatted", nowVN.format(formatter));
        timezoneInfo.put("utcFormatted", nowUTC.format(formatter));
        
        log.info("🌏 Timezone test - System: {}, VN: {}, UTC: {}", 
                now.format(formatter), 
                nowVN.format(formatter), 
                nowUTC.format(formatter));
        
        return ApiResponse.success("Timezone information", timezoneInfo);
    }
}
