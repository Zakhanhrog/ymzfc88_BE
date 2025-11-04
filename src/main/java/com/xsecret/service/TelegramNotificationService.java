package com.xsecret.service;

import com.xsecret.entity.TelegramConfig;
import com.xsecret.repository.TelegramConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationService {

    private final TelegramConfigRepository telegramConfigRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        Optional<TelegramConfig> activeConfig = telegramConfigRepository.findFirstByEnabledTrueOrderByCreatedAtDesc();
        if (activeConfig.isPresent()) {
            TelegramConfig config = activeConfig.get();
            log.info("TelegramNotificationService initialized with active config ID: {}, BotToken configured: {}, ChatId: {}", 
                    config.getId(), config.getBotToken() != null && !config.getBotToken().isBlank(), config.getChatId());
        } else {
            log.info("TelegramNotificationService initialized - no active config found");
        }
    }

    public void sendMessage(String text) {
        log.info("TelegramNotificationService.sendMessage called with text: {}", text);
        
        Optional<TelegramConfig> activeConfig = telegramConfigRepository.findFirstByEnabledTrueOrderByCreatedAtDesc();
        if (activeConfig.isEmpty()) {
            log.warn("No active Telegram config found; skip send");
            return;
        }

        TelegramConfig config = activeConfig.get();
        if (!config.getEnabled()) {
            log.warn("Telegram notifications disabled in config");
            return;
        }

        String botToken = config.getBotToken();
        String chatId = config.getChatId();
        
        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            log.warn("Telegram not configured; skip send. botToken: {}, chatId: {}", 
                    botToken != null ? "***" : "null", chatId);
            return;
        }

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        log.info("Sending Telegram message to URL: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        // Convert chat_id to Long if it's a numeric string
        try {
            Long chatIdLong = Long.parseLong(chatId);
            body.put("chat_id", chatIdLong);
        } catch (NumberFormatException e) {
            body.put("chat_id", chatId);
        }
        body.put("text", text);
        body.put("parse_mode", "HTML");         
        body.put("disable_web_page_preview", true);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            var response = restTemplate.postForEntity(url, request, String.class);
            log.info("Telegram message sent successfully. Response: {}", response.getBody());
        } catch (RestClientException ex) {
            log.error("Failed to send Telegram message: {}", ex.getMessage(), ex);
        }
    }

    public String formatVnd(long amount) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(amount) + "đ";
    }
    
    public String formatVnd(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(amount.longValue()) + "đ";
    }
    
    public String formatBoldText(String text) {
        return "<b>" + escapeHtml(text) + "</b>";
    }

    public String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
}


