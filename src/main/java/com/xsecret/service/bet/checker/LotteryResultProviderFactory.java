package com.xsecret.service.bet.checker;

import org.springframework.stereotype.Component;

/**
 * Factory để lấy LotteryResultProvider phù hợp theo region
 */
@Component
public class LotteryResultProviderFactory {
    
    private final DatabaseLotteryResultProvider databaseProvider;
    
    public LotteryResultProviderFactory(DatabaseLotteryResultProvider databaseProvider) {
        this.databaseProvider = databaseProvider;
    }
    
    /**
     * Lấy provider phù hợp theo region
     * @param region "mienBac" hoặc "mienTrungNam"
     * @return DatabaseLotteryResultProvider (dùng chung cho cả 2 region)
     */
    public LotteryResultProvider getProvider(String region) {
        // Dùng database provider cho tất cả regions
        // Provider sẽ tự động lấy kết quả theo region/province từ database
        return databaseProvider;
    }
}
