package com.xsecret.service;

import com.xsecret.dto.request.TelegramConfigRequest;
import com.xsecret.dto.response.TelegramConfigResponse;
import com.xsecret.entity.TelegramConfig;
import com.xsecret.repository.TelegramConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TelegramConfigService {

    private final TelegramConfigRepository telegramConfigRepository;

    public TelegramConfigResponse createConfig(TelegramConfigRequest request) {
        // Disable all existing configs
        List<TelegramConfig> existingConfigs = telegramConfigRepository.findAll();
        for (TelegramConfig config : existingConfigs) {
            config.setEnabled(false);
            telegramConfigRepository.save(config);
        }

        // Create new config
        TelegramConfig newConfig = TelegramConfig.builder()
                .botToken(request.getBotToken())
                .chatId(request.getChatId())
                .enabled(request.getEnabled())
                .description(request.getDescription())
                .build();

        TelegramConfig savedConfig = telegramConfigRepository.save(newConfig);
        log.info("Created new Telegram config with ID: {}", savedConfig.getId());

        return TelegramConfigResponse.fromEntity(savedConfig);
    }

    public TelegramConfigResponse updateConfig(Long id, TelegramConfigRequest request) {
        TelegramConfig config = telegramConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Telegram config not found"));

        // If enabling this config, disable all others
        if (request.getEnabled() && !config.getEnabled()) {
            List<TelegramConfig> allConfigs = telegramConfigRepository.findAll();
            for (TelegramConfig c : allConfigs) {
                c.setEnabled(false);
                telegramConfigRepository.save(c);
            }
        }

        config.setBotToken(request.getBotToken());
        config.setChatId(request.getChatId());
        config.setEnabled(request.getEnabled());
        config.setDescription(request.getDescription());

        TelegramConfig updatedConfig = telegramConfigRepository.save(config);
        log.info("Updated Telegram config with ID: {}", updatedConfig.getId());

        return TelegramConfigResponse.fromEntity(updatedConfig);
    }

    @Transactional(readOnly = true)
    public List<TelegramConfigResponse> getAllConfigs() {
        return telegramConfigRepository.findAll().stream()
                .map(TelegramConfigResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TelegramConfigResponse> getActiveConfig() {
        return telegramConfigRepository.findFirstByEnabledTrueOrderByCreatedAtDesc()
                .map(TelegramConfigResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Optional<TelegramConfigResponse> getLatestConfig() {
        return telegramConfigRepository.findFirstByOrderByCreatedAtDesc()
                .map(TelegramConfigResponse::fromEntity);
    }

    public void deleteConfig(Long id) {
        TelegramConfig config = telegramConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Telegram config not found"));

        telegramConfigRepository.delete(config);
        log.info("Deleted Telegram config with ID: {}", id);
    }
}
