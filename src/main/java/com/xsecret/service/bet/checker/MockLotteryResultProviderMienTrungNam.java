package com.xsecret.service.bet.checker;

import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Mock provider cho Miền Trung Nam - sẽ thay thế bằng API thật sau
 */
@Component("mienTrungNamLotteryResultProvider")
public class MockLotteryResultProviderMienTrungNam implements LotteryResultProvider {
    
    @Override
    public List<String> getLotteryResults() {
        // Dữ liệu mẫu từ ảnh Miền Trung Nam (8 giải)
        return List.of(
            "042293", // Giải đặc biệt - 2 số cuối: 93, 3 số cuối: 293, 4 số cuối: 2293, 5 số cuối: 42293
            "02518",  // Giải nhất - 2 số cuối: 18, 3 số cuối: 518, 4 số cuối: 2518
            "49226",  // Giải nhì - 2 số cuối: 26, 3 số cuối: 226, 4 số cuối: 9226
            "03856",  // Giải ba - 2 số cuối: 56, 3 số cuối: 856, 4 số cuối: 3856
            "04216",  // Giải ba - 2 số cuối: 16, 3 số cuối: 216, 4 số cuối: 4216
            "00810",  // Giải tư - 2 số cuối: 10, 3 số cuối: 810, 4 số cuối: 0810
            "02321",  // Giải tư - 2 số cuối: 21, 3 số cuối: 321, 4 số cuối: 2321
            "00681",  // Giải tư - 2 số cuối: 81, 3 số cuối: 681, 4 số cuối: 0681
            "51728",  // Giải tư - 2 số cuối: 28, 3 số cuối: 728, 4 số cuối: 1728
            "24507",  // Giải tư - 2 số cuối: 07, 3 số cuối: 507, 4 số cuối: 4507
            "58068",  // Giải tư - 2 số cuối: 68, 3 số cuối: 068, 4 số cuối: 8068
            "96136",  // Giải tư - 2 số cuối: 36, 3 số cuối: 136, 4 số cuối: 6136
            "8877",   // Giải năm - 2 số cuối: 77, 3 số cuối: 877, 4 số cuối: 8877
            "5934",   // Giải sáu - 2 số cuối: 34, 3 số cuối: 934, 4 số cuối: 5934
            "7442",   // Giải sáu - 2 số cuối: 42, 3 số cuối: 442, 4 số cuối: 7442
            "3430",   // Giải sáu - 2 số cuối: 30, 3 số cuối: 430, 4 số cuối: 3430
            "884",    // Giải bảy - 2 số cuối: 84, 3 số cuối: 884
            "40"      // Giải tám - 2 số cuối: 40
        );
    }
    
    @Override
    public String getDacBietNumber() {
        List<String> results = getLotteryResults();
        // Giải đặc biệt là số đầu tiên (index 0): "042293"
        if (results.size() > 0) {
            return results.get(0);
        }
        return null;
    }
    
    @Override
    public String getGiaiNhatNumber() {
        List<String> results = getLotteryResults();
        // Giải nhất là số thứ 2 (index 1): "02518"
        if (results.size() > 1) {
            return results.get(1);
        }
        return null;
    }
    
    @Override
    public String getGiai8Number() {
        // Giải 8 cho Miền Trung Nam
        // Từ ảnh Miền Trung Nam: "40" → 2 số cuối: 40
        return "40";
    }
    
    @Override
    public String getGiai7Number() {
        // Giải 7 cho Miền Trung Nam
        // Từ ảnh Miền Trung Nam: "884" → 3 số cuối: 884
        return "884";
    }
    
    @Override
    public List<String> getGiai7Numbers() {
        // Không áp dụng cho Miền Trung Nam (chỉ có 1 số giải 7)
        // Trả về empty list
        return List.of();
    }
    
    @Override
    public List<String> getGiai6Numbers() {
        // Không áp dụng cho Miền Trung Nam (giải 6 có 3 số riêng biệt)
        // Trả về empty list
        return List.of();
    }
    
    @Override
    public List<String> getGiai8Numbers() {
        // Giải 8 cho Miền Trung Nam chỉ có 1 số
        return List.of("40");
    }
}
