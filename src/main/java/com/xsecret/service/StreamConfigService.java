package com.xsecret.service;

import com.xsecret.dto.request.StreamConfigRequest;
import com.xsecret.dto.response.StreamConfigResponse;
import com.xsecret.entity.StreamConfig;
import com.xsecret.repository.StreamConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StreamConfigService {
    
    private final StreamConfigRepository streamConfigRepository;
    
    /**
     * Tạo stream config mới
     */
    public StreamConfigResponse createStreamConfig(StreamConfigRequest request) {
        log.info("Creating stream config for game: {}, table: {}", request.getGameType(), request.getTableNumber());
        
        // Check if config already exists for this game and table (unique constraint)
        if (request.getTableNumber() != null) {
            streamConfigRepository.findByGameTypeAndTableNumber(request.getGameType(), request.getTableNumber())
                    .ifPresent(config -> {
                        throw new IllegalArgumentException("Đã tồn tại stream config cho " + request.getGameType() + " bàn " + request.getTableNumber());
                    });
        } else {
            streamConfigRepository.findByGameTypeAndTableNumberIsNull(request.getGameType())
                    .ifPresent(config -> {
                        throw new IllegalArgumentException("Đã tồn tại stream config cho " + request.getGameType());
                    });
        }
        
        StreamConfig config = StreamConfig.builder()
                .gameType(request.getGameType())
                .tableNumber(request.getTableNumber())
                .streamKey(request.getStreamKey().trim())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .build();
        
        try {
            StreamConfig saved = streamConfigRepository.save(config);
            log.info("Stream config created successfully with id: {}", saved.getId());
            return StreamConfigResponse.fromEntity(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("Database constraint violation when creating stream config: {}", e.getMessage());
            throw new IllegalArgumentException("Không thể tạo: đã tồn tại cấu hình trùng lặp (game type + bàn số)");
        }
    }
    
    /**
     * Lấy tất cả stream configs
     */
    @Transactional(readOnly = true)
    public List<StreamConfigResponse> getAllStreamConfigs() {
        return streamConfigRepository.findAll().stream()
                .map(StreamConfigResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy stream config theo ID
     */
    @Transactional(readOnly = true)
    public StreamConfigResponse getStreamConfigById(Long id) {
        StreamConfig config = streamConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stream config not found with id: " + id));
        return StreamConfigResponse.fromEntity(config);
    }
    
    /**
     * Lấy stream config theo game type và table number
     */
    @Transactional(readOnly = true)
    public StreamConfigResponse getStreamConfigByGameAndTable(StreamConfig.GameType gameType, Integer tableNumber) {
        StreamConfig config;
        if (tableNumber != null) {
            config = streamConfigRepository.findByGameTypeAndTableNumber(gameType, tableNumber)
                    .orElseThrow(() -> new RuntimeException("Stream config not found for " + gameType + " table " + tableNumber));
        } else {
            config = streamConfigRepository.findByGameTypeAndTableNumberIsNull(gameType)
                    .orElseThrow(() -> new RuntimeException("Stream config not found for " + gameType));
        }
        return StreamConfigResponse.fromEntity(config);
    }
    
    /**
     * Lấy stream configs theo game type
     */
    @Transactional(readOnly = true)
    public List<StreamConfigResponse> getStreamConfigsByGameType(StreamConfig.GameType gameType) {
        return streamConfigRepository.findByGameType(gameType).stream()
                .map(StreamConfigResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy stream configs đang active
     */
    @Transactional(readOnly = true)
    public List<StreamConfigResponse> getActiveStreamConfigs() {
        return streamConfigRepository.findByIsActiveTrue().stream()
                .map(StreamConfigResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Cập nhật stream config
     */
    public StreamConfigResponse updateStreamConfig(Long id, StreamConfigRequest request) {
        log.info("Updating stream config with id: {}, new game type: {}, table: {}", 
                id, request.getGameType(), request.getTableNumber());
        
        StreamConfig config = streamConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy stream config với id: " + id));
        
        // Check if config already exists for this game and table (excluding current config)
        // Only check if game type or table number is being changed
        boolean gameTypeChanged = !config.getGameType().equals(request.getGameType());
        boolean tableNumberChanged = (config.getTableNumber() == null && request.getTableNumber() != null) ||
                                     (config.getTableNumber() != null && !config.getTableNumber().equals(request.getTableNumber()));
        
        if (gameTypeChanged || tableNumberChanged) {
            if (request.getTableNumber() != null) {
                streamConfigRepository.findByGameTypeAndTableNumber(request.getGameType(), request.getTableNumber())
                        .ifPresent(existingConfig -> {
                            if (!existingConfig.getId().equals(id)) {
                                throw new IllegalArgumentException("Đã tồn tại stream config cho " + request.getGameType() + " bàn " + request.getTableNumber());
                            }
                        });
            } else {
                streamConfigRepository.findByGameTypeAndTableNumberIsNull(request.getGameType())
                        .ifPresent(existingConfig -> {
                            if (!existingConfig.getId().equals(id)) {
                                throw new IllegalArgumentException("Đã tồn tại stream config cho " + request.getGameType());
                            }
                        });
            }
        }
        
        // Update fields
        config.setGameType(request.getGameType());
        config.setTableNumber(request.getTableNumber());
        config.setStreamKey(request.getStreamKey().trim());
        config.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        config.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        
        try {
            StreamConfig updated = streamConfigRepository.save(config);
            log.info("Stream config updated successfully with id: {}", updated.getId());
            return StreamConfigResponse.fromEntity(updated);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("Database constraint violation when updating stream config: {}", e.getMessage());
            throw new IllegalArgumentException("Không thể cập nhật: đã tồn tại cấu hình trùng lặp (game type + bàn số)");
        }
    }
    
    /**
     * Xóa stream config
     */
    public void deleteStreamConfig(Long id) {
        log.info("Deleting stream config with id: {}", id);
        streamConfigRepository.deleteById(id);
    }
    
    /**
     * Toggle trạng thái pause live cho stream
     */
    public StreamConfigResponse toggleLivePause(StreamConfig.GameType gameType, Integer tableNumber) {
        StreamConfig config;
        if (tableNumber != null) {
            config = streamConfigRepository.findByGameTypeAndTableNumber(gameType, tableNumber)
                    .orElseThrow(() -> new RuntimeException("Stream config not found for " + gameType + " table " + tableNumber));
        } else {
            config = streamConfigRepository.findByGameTypeAndTableNumberIsNull(gameType)
                    .orElseThrow(() -> new RuntimeException("Stream config not found for " + gameType));
        }
        
        boolean newPauseState = !(config.getIsLivePaused() != null ? config.getIsLivePaused() : false);
        config.setIsLivePaused(newPauseState);
        
        StreamConfig saved = streamConfigRepository.save(config);
        log.info("Toggled live pause for {} {} to {}", gameType, tableNumber != null ? "table " + tableNumber : "", newPauseState);
        return StreamConfigResponse.fromEntity(saved);
    }
    
    /**
     * Toggle trạng thái ended live cho stream
     */
    public StreamConfigResponse toggleLiveEnded(StreamConfig.GameType gameType, Integer tableNumber) {
        StreamConfig config;
        if (tableNumber != null) {
            config = streamConfigRepository.findByGameTypeAndTableNumber(gameType, tableNumber)
                    .orElseThrow(() -> new RuntimeException("Stream config not found for " + gameType + " table " + tableNumber));
        } else {
            config = streamConfigRepository.findByGameTypeAndTableNumberIsNull(gameType)
                    .orElseThrow(() -> new RuntimeException("Stream config not found for " + gameType));
        }
        
        boolean newEndedState = !(config.getIsLiveEnded() != null ? config.getIsLiveEnded() : false);
        config.setIsLiveEnded(newEndedState);
        
        StreamConfig saved = streamConfigRepository.save(config);
        log.info("Toggled live ended for {} {} to {}", gameType, tableNumber != null ? "table " + tableNumber : "", newEndedState);
        return StreamConfigResponse.fromEntity(saved);
    }
}

