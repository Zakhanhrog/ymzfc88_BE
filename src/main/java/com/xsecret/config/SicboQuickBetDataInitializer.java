package com.xsecret.config;

import com.xsecret.entity.SicboQuickBetConfig;
import com.xsecret.repository.SicboQuickBetConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SicboQuickBetDataInitializer implements CommandLineRunner {

    private final SicboQuickBetConfigRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }

        List<SicboQuickBetConfig> defaults = new ArrayList<>();

        defaults.add(SicboQuickBetConfig.builder()
                .code("sicbo_primary_small")
                .name("Xỉu")
                .payoutMultiplier(BigDecimal.valueOf(0.97))
                .layoutGroup(SicboQuickBetConfig.GROUP_PRIMARY)
                .displayOrder(0)
                .isActive(true)
                .build());

        defaults.add(SicboQuickBetConfig.builder()
                .code("sicbo_primary_big")
                .name("Tài")
                .payoutMultiplier(BigDecimal.valueOf(0.97))
                .layoutGroup(SicboQuickBetConfig.GROUP_PRIMARY)
                .displayOrder(1)
                .isActive(true)
                .build());

        int comboOrder = 0;
        int[] comboFaces = {1, 6, 2, 5, 3, 4};
        for (int face : comboFaces) {
            defaults.add(SicboQuickBetConfig.builder()
                    .code("sicbo_combo_triple_" + face)
                    .name("Bộ ba " + face)
                    .payoutMultiplier(BigDecimal.valueOf(20))
                    .layoutGroup(SicboQuickBetConfig.GROUP_COMBINATION)
                    .displayOrder(comboOrder++)
                    .isActive(true)
                    .build());
        }

        defaults.add(SicboQuickBetConfig.builder()
                .code("sicbo_parity_even")
                .name("Chẵn")
                .payoutMultiplier(BigDecimal.valueOf(0.97))
                .layoutGroup(SicboQuickBetConfig.GROUP_TOTAL_TOP)
                .displayOrder(0)
                .isActive(true)
                .build());

        int[] topTotals = {4, 5, 6, 7, 8, 9, 10};
        int topOrder = 1;
        int[] topPayouts = {30, 18, 14, 12, 8, 6, 6};
        for (int i = 0; i < topTotals.length; i++) {
            defaults.add(SicboQuickBetConfig.builder()
                    .code("sicbo_total_" + topTotals[i])
                    .name("Tổng " + topTotals[i])
                    .payoutMultiplier(BigDecimal.valueOf(topPayouts[i]))
                    .layoutGroup(SicboQuickBetConfig.GROUP_TOTAL_TOP)
                    .displayOrder(topOrder++)
                    .isActive(true)
                    .build());
        }

        defaults.add(SicboQuickBetConfig.builder()
                .code("sicbo_parity_odd")
                .name("Lẻ")
                .payoutMultiplier(BigDecimal.valueOf(0.97))
                .layoutGroup(SicboQuickBetConfig.GROUP_TOTAL_BOTTOM)
                .displayOrder(0)
                .isActive(true)
                .build());

        int[] bottomTotals = {17, 16, 15, 14, 13, 12, 11};
        int[] bottomPayouts = {30, 18, 14, 12, 8, 6, 6};
        int bottomOrder = 1;
        for (int i = 0; i < bottomTotals.length; i++) {
            defaults.add(SicboQuickBetConfig.builder()
                    .code("sicbo_total_" + bottomTotals[i])
                    .name("Tổng " + bottomTotals[i])
                    .payoutMultiplier(BigDecimal.valueOf(bottomPayouts[i]))
                    .layoutGroup(SicboQuickBetConfig.GROUP_TOTAL_BOTTOM)
                    .displayOrder(bottomOrder++)
                    .isActive(true)
                    .build());
        }

        int singleOrder = 0;
        for (int face = 1; face <= 6; face++) {
            defaults.add(SicboQuickBetConfig.builder()
                    .code("sicbo_single_" + face)
                    .name("Một mặt " + face)
                    .payoutMultiplier(BigDecimal.valueOf(0.97))
                    .layoutGroup(SicboQuickBetConfig.GROUP_SINGLE)
                    .displayOrder(singleOrder++)
                    .isActive(true)
                    .build());
        }

        repository.saveAll(defaults);
        log.info("Initialized {} default Sicbo quick bet configurations", defaults.size());
    }
}


