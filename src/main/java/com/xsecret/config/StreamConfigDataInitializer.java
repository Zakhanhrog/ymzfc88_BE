package com.xsecret.config;

import com.xsecret.entity.StreamConfig;
import com.xsecret.repository.StreamConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StreamConfigDataInitializer implements CommandLineRunner {

    private final StreamConfigRepository repository;

    @Override
    public void run(String... args) {
        // Initialize XocDia stream config
        if (repository.findByGameTypeAndTableNumberIsNull(StreamConfig.GameType.XOC_DIA).isEmpty()) {
            StreamConfig xocDia = StreamConfig.builder()
                    .gameType(StreamConfig.GameType.XOC_DIA)
                    .tableNumber(null)
                    .streamKey("xocdia")
                    .isActive(true)
                    .description("Stream cho game Xóc Đĩa")
                    .build();
            repository.save(xocDia);
            log.info("Initialized default XocDia stream config");
        }

        // Initialize Sicbo stream config (table 1)
        if (repository.findByGameTypeAndTableNumber(StreamConfig.GameType.SICBO, 1).isEmpty()) {
            StreamConfig sicbo = StreamConfig.builder()
                    .gameType(StreamConfig.GameType.SICBO)
                    .tableNumber(1)
                    .streamKey("sicbo")
                    .isActive(true)
                    .description("Stream cho game Sicbo bàn số 1")
                    .build();
            repository.save(sicbo);
            log.info("Initialized default Sicbo stream config for table 1");
        }
    }
}

