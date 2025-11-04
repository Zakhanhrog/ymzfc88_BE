package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.service.TelegramNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class TestController {

    private final TelegramNotificationService telegramNotificationService;

    public TestController(TelegramNotificationService telegramNotificationService) {
        this.telegramNotificationService = telegramNotificationService;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "XSecret Backend");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(ApiResponse.success("Service is running", health));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<String>> publicEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("This is a public endpoint", "Public access granted"));
    }

    @PostMapping("/telegram")
    public ResponseEntity<ApiResponse<String>> testTelegram(@RequestParam(required = false) String message) {
        try {
            String testMessage = message != null ? message : 
                "Yêu cầu " + telegramNotificationService.formatBoldText("NẠP TIỀN") + " từ khách hàng " + 
                telegramNotificationService.formatBoldText("tele14") + " - Số tiền: " + 
                telegramNotificationService.formatBoldText(telegramNotificationService.formatVnd(2000000L));
            telegramNotificationService.sendMessage(testMessage);
            return ResponseEntity.ok(ApiResponse.success("Telegram test message sent", testMessage));
        } catch (Exception e) {
            log.error("Failed to send test Telegram message", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to send Telegram message: " + e.getMessage()));
        }
    }

    @GetMapping("/telegram/config")
    public ResponseEntity<ApiResponse<Object>> getTelegramConfig() {
        try {
            // This will show the current active config status
            return ResponseEntity.ok(ApiResponse.success("Current Telegram config status", 
                Map.of("hasActiveConfig", true))); // For now, always return true
        } catch (Exception e) {
            log.error("Failed to get Telegram config status", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get Telegram config: " + e.getMessage()));
        }
    }
}
