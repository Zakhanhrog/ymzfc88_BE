package com.xsecret.config;

import com.xsecret.entity.StreamConfig;
import com.xsecret.repository.StreamConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StreamConfigDataInitializer implements CommandLineRunner {

    private final StreamConfigRepository repository;

    @Override
    public void run(String... args) {
        // Initialize XocDia stream config
        // Xử lý trường hợp có nhiều kết quả trùng lặp
        List<StreamConfig> xocDiaConfigs = repository.findByGameType(StreamConfig.GameType.XOC_DIA)
                .stream()
                .filter(config -> config.getTableNumber() == null)
                .toList();
        
        if (xocDiaConfigs.isEmpty()) {
            StreamConfig xocDia = StreamConfig.builder()
                    .gameType(StreamConfig.GameType.XOC_DIA)
                    .tableNumber(null)
                    .streamKey("xocdia")
                    .isActive(true)
                    .description("Stream cho game Xóc Đĩa")
                    .build();
            repository.save(xocDia);
            log.info("Initialized default XocDia stream config");
        } else if (xocDiaConfigs.size() > 1) {
            // Xóa các bản ghi trùng lặp, chỉ giữ lại bản ghi đầu tiên
            log.warn("Found {} duplicate XocDia stream configs, removing duplicates...", xocDiaConfigs.size());
            for (int i = 1; i < xocDiaConfigs.size(); i++) {
                repository.delete(xocDiaConfigs.get(i));
            }
            log.info("Removed {} duplicate XocDia stream configs", xocDiaConfigs.size() - 1);
        } else {
            log.info("XocDia stream config already exists");
        }

        // Initialize Sicbo stream config (table 1)
        List<StreamConfig> sicboConfigs = repository.findByGameType(StreamConfig.GameType.SICBO)
                .stream()
                .filter(config -> config.getTableNumber() != null && config.getTableNumber() == 1)
                .toList();
        
        if (sicboConfigs.isEmpty()) {
            StreamConfig sicbo = StreamConfig.builder()
                    .gameType(StreamConfig.GameType.SICBO)
                    .tableNumber(1)
                    .streamKey("sicbo")
                    .isActive(true)
                    .description("Stream cho game Sicbo bàn số 1")
                    .build();
            repository.save(sicbo);
            log.info("Initialized default Sicbo stream config for table 1");
        } else if (sicboConfigs.size() > 1) {
            // Xóa các bản ghi trùng lặp, chỉ giữ lại bản ghi đầu tiên
            log.warn("Found {} duplicate Sicbo stream configs for table 1, removing duplicates...", sicboConfigs.size());
            for (int i = 1; i < sicboConfigs.size(); i++) {
                repository.delete(sicboConfigs.get(i));
            }
            log.info("Removed {} duplicate Sicbo stream configs", sicboConfigs.size() - 1);
        } else {
            log.info("Sicbo stream config for table 1 already exists");
        }
    }
}

