package com.xsecret.service.bet.checker;

import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Mock provider - sẽ thay thế bằng API thật sau
 */
@Component
public class MockLotteryResultProvider implements LotteryResultProvider {
    
    @Override
    public List<String> getLotteryResults() {
        // GIỮ NGUYÊN logic mock từ BetService.java
        return List.of(
            "01640", // Giải đặc biệt - 2 số đầu: 01 ⭐, 2 số cuối: 40, 3 số cuối: 640, 4 số cuối: 1640
            "54778", // Giải nhất - 2 số cuối: 78, 3 số cuối: 778, 4 số cuối: 4778 ⭐ CHỈ SỐ NÀY CHO GIAI-NHAT
            "58480", // Giải nhì - 2 số cuối: 80, 3 số cuối: 480, 4 số cuối: 8480
            "54921", // Giải nhì - 2 số cuối: 21, 3 số cuối: 921, 4 số cuối: 4921
            "50749", // Giải ba - 2 số cuối: 49, 3 số cuối: 749, 4 số cuối: 0749
            "94670", // Giải ba - 2 số cuối: 70, 3 số cuối: 670, 4 số cuối: 4670
            "56818", // Giải ba - 2 số cuối: 18, 3 số cuối: 818, 4 số cuối: 6818
            "51058", // Giải ba - 2 số cuối: 58, 3 số cuối: 058, 4 số cuối: 1058
            "03833", // Giải ba - 2 số cuối: 33, 3 số cuối: 833, 4 số cuối: 3833
            "71888", // Giải ba - 2 số cuối: 88, 3 số cuối: 888, 4 số cuối: 1888
            "8299",  // Giải tư - 2 số cuối: 99, 3 số cuối: 299, 4 số cuối: 8299
            "6500",  // Giải tư - 2 số cuối: 00, 3 số cuối: 500, 4 số cuối: 6500
            "7568",  // Giải tư - 2 số cuối: 68, 3 số cuối: 568, 4 số cuối: 7568
            "0321",  // Giải tư - 2 số cuối: 21, 3 số cuối: 321, 4 số cuối: 0321
            "2625",  // Giải năm - 2 số cuối: 25, 3 số cuối: 625, 4 số cuối: 2625
            "5349",  // Giải năm - 2 số cuối: 49, 3 số cuối: 349, 4 số cuối: 5349
            "0601",  // Giải năm - 2 số cuối: 01, 3 số cuối: 601, 4 số cuối: 0601
            "2158",  // Giải năm - 2 số cuối: 58, 3 số cuối: 158, 4 số cuối: 2158
            "8746",  // Giải năm - 2 số cuối: 46, 3 số cuối: 746, 4 số cuối: 8746
            "0990",  // Giải năm - 2 số cuối: 90, 3 số cuối: 990, 4 số cuối: 0990
            "034",   // Giải sáu - 2 số cuối: 34, 3 số cuối: 034
            "005",   // Giải sáu - 2 số cuối: 05, 3 số cuối: 005
            "095",   // Giải sáu - 2 số cuối: 95, 3 số cuối: 095
            "41",    // Giải bảy - 2 số cuối: 41
            "71",    // Giải bảy - 2 số cuối: 71
            "90",    // Giải bảy - 2 số cuối: 90
            "42"     // Giải bảy - 2 số cuối: 42
        );
    }
    
    @Override
    public String getDacBietNumber() {
        List<String> results = getLotteryResults();
        // Giải đặc biệt là số đầu tiên (index 0): "01640"
        if (results.size() > 0) {
            return results.get(0);
        }
        return null;
    }
    
    @Override
    public String getGiaiNhatNumber() {
        List<String> results = getLotteryResults();
        // Giải nhất là số thứ 2 (index 1): "54778"
        if (results.size() > 1) {
            return results.get(1);
        }
        return null;
    }
    
    @Override
    public String getGiai8Number() {
        // Mock giải 8 cho Miền Trung Nam
        // Giải 8 chỉ có 1 số 2 chữ số
        // Ví dụ: "13" → 2 số cuối: 13
        return "13";
    }
    
    @Override
    public String getGiai7Number() {
        // Mock giải 7 cho Miền Trung Nam
        // Giải 7 chỉ có 1 số 3 chữ số
        // Ví dụ: "138" → 3 số cuối: 138
        return "138";
    }
    
    @Override
    public List<String> getGiai7Numbers() {
        // Mock giải 7 cho Miền Bắc
        // Giải 7 có 4 số 2 chữ số
        // Lấy 4 số cuối cùng trong danh sách kết quả (indices 39-42 trong getLotteryResults())
        List<String> results = getLotteryResults();
        if (results.size() >= 27) {
            // 4 số giải 7 là 4 số cuối: "41", "71", "90", "42"
            return results.subList(results.size() - 4, results.size());
        }
        return List.of("41", "71", "90", "42"); // Fallback
    }
    
    @Override
    public List<String> getGiai6Numbers() {
        // Mock giải 6 cho Miền Bắc
        // Giải 6 có 3 số 3 chữ số
        // Lấy 3 số trước 4 số giải 7 (indices 36-38 trong getLotteryResults())
        List<String> results = getLotteryResults();
        if (results.size() >= 27) {
            // 3 số giải 6: "034", "005", "095"
            return results.subList(results.size() - 7, results.size() - 4);
        }
        return List.of("034", "005", "095"); // Fallback
    }
}

