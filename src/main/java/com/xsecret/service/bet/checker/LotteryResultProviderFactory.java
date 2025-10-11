package com.xsecret.service.bet.checker;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Factory để lấy LotteryResultProvider phù hợp theo region
 */
@Component
public class LotteryResultProviderFactory {
    
    private final LotteryResultProvider mienBacProvider;
    private final LotteryResultProvider mienTrungNamProvider;
    
    public LotteryResultProviderFactory(
            @Qualifier("mockLotteryResultProvider") LotteryResultProvider mienBacProvider,
            @Qualifier("mienTrungNamLotteryResultProvider") LotteryResultProvider mienTrungNamProvider) {
        this.mienBacProvider = mienBacProvider;
        this.mienTrungNamProvider = mienTrungNamProvider;
    }
    
    /**
     * Lấy provider phù hợp theo region
     * @param region "mienBac" hoặc "mienTrungNam"
     * @return LotteryResultProvider tương ứng
     */
    public LotteryResultProvider getProvider(String region) {
        if ("mienBac".equals(region)) {
            return mienBacProvider;
        } else if ("mienTrungNam".equals(region)) {
            return mienTrungNamProvider;
        } else {
            // Default về Miền Bắc nếu không xác định được
            return mienBacProvider;
        }
    }
}
