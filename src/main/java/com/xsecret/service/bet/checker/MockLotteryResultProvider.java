package com.xsecret.service.bet.checker;

import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Mock provider - sẽ thay thế bằng API thật sau
 */
@Component("mockLotteryResultProvider")
public class MockLotteryResultProvider implements LotteryResultProvider {
    
    @Override
    public List<String> getLotteryResults() {
        // Dữ liệu mẫu từ ảnh Miền Bắc (7 giải)
        return List.of(
            "00943", // Giải đặc biệt - 2 số cuối: 43, 3 số cuối: 943, 4 số cuối: 0943
            "43213", // Giải nhất - 2 số cuối: 13, 3 số cuối: 213, 4 số cuối: 3213
            "66146", // Giải nhì - 2 số cuối: 46, 3 số cuối: 146, 4 số cuối: 6146
            "15901", // Giải nhì - 2 số cuối: 01, 3 số cuối: 901, 4 số cuối: 5901
            "22906", // Giải ba - 2 số cuối: 06, 3 số cuối: 906, 4 số cuối: 2906
            "04955", // Giải ba - 2 số cuối: 55, 3 số cuối: 955, 4 số cuối: 4955
            "93893", // Giải ba - 2 số cuối: 93, 3 số cuối: 893, 4 số cuối: 3893
            "32538", // Giải ba - 2 số cuối: 38, 3 số cuối: 538, 4 số cuối: 2538
            "25660", // Giải ba - 2 số cuối: 60, 3 số cuối: 660, 4 số cuối: 5660
            "85773", // Giải ba - 2 số cuối: 73, 3 số cuối: 773, 4 số cuối: 5773
            "8964",  // Giải tư - 2 số cuối: 64, 3 số cuối: 964, 4 số cuối: 8964
            "0803",  // Giải tư - 2 số cuối: 03, 3 số cuối: 803, 4 số cuối: 0803
            "4867",  // Giải tư - 2 số cuối: 67, 3 số cuối: 867, 4 số cuối: 4867
            "2405",  // Giải tư - 2 số cuối: 05, 3 số cuối: 405, 4 số cuối: 2405
            "9122",  // Giải năm - 2 số cuối: 22, 3 số cuối: 122, 4 số cuối: 9122
            "6281",  // Giải năm - 2 số cuối: 81, 3 số cuối: 281, 4 số cuối: 6281
            "8813",  // Giải năm - 2 số cuối: 13, 3 số cuối: 813, 4 số cuối: 8813
            "6672",  // Giải năm - 2 số cuối: 72, 3 số cuối: 672, 4 số cuối: 6672
            "8101",  // Giải năm - 2 số cuối: 01, 3 số cuối: 101, 4 số cuối: 8101
            "7293",  // Giải năm - 2 số cuối: 93, 3 số cuối: 293, 4 số cuối: 7293
            "803",   // Giải sáu - 2 số cuối: 03, 3 số cuối: 803
            "301",   // Giải sáu - 2 số cuối: 01, 3 số cuối: 301
            "325",   // Giải sáu - 2 số cuối: 25, 3 số cuối: 325
            "84",    // Giải bảy - 2 số cuối: 84
            "09",    // Giải bảy - 2 số cuối: 09
            "69",    // Giải bảy - 2 số cuối: 69
            "79"     // Giải bảy - 2 số cuối: 79
        );
    }
    
    @Override
    public String getDacBietNumber() {
        List<String> results = getLotteryResults();
        // Giải đặc biệt là số đầu tiên (index 0): "00943"
        if (results.size() > 0) {
            return results.get(0);
        }
        return null;
    }
    
    @Override
    public String getGiaiNhatNumber() {
        List<String> results = getLotteryResults();
        // Giải nhất là số thứ 2 (index 1): "43213"
        if (results.size() > 1) {
            return results.get(1);
        }
        return null;
    }
    
    @Override
    public String getGiai8Number() {
        // Mock giải 8 cho Miền Trung Nam
        // Giải 8 chỉ có 1 số 2 chữ số
        // Từ ảnh Miền Trung Nam: "40" → 2 số cuối: 40
        return "40";
    }
    
    @Override
    public String getGiai7Number() {
        // Mock giải 7 cho Miền Trung Nam
        // Giải 7 chỉ có 1 số 3 chữ số
        // Từ ảnh Miền Trung Nam: "884" → 3 số cuối: 884
        return "884";
    }
    
    @Override
    public List<String> getGiai7Numbers() {
        // Mock giải 7 cho Miền Bắc
        // Giải 7 có 4 số 2 chữ số
        // Lấy 4 số cuối cùng trong danh sách kết quả (indices 23-26 trong getLotteryResults())
        List<String> results = getLotteryResults();
        if (results.size() >= 27) {
            // 4 số giải 7 là 4 số cuối: "84", "09", "69", "79"
            return results.subList(results.size() - 4, results.size());
        }
        return List.of("84", "09", "69", "79"); // Fallback
    }
    
    @Override
    public List<String> getGiai6Numbers() {
        // Mock giải 6 cho Miền Bắc
        // Giải 6 có 3 số 3 chữ số
        // Lấy 3 số trước 4 số giải 7 (indices 20-22 trong getLotteryResults())
        List<String> results = getLotteryResults();
        if (results.size() >= 27) {
            // 3 số giải 6: "803", "301", "325"
            return results.subList(results.size() - 7, results.size() - 4);
        }
        return List.of("803", "301", "325"); // Fallback
    }
    
    @Override
    public List<String> getGiai8Numbers() {
        // Mock: Lấy 1 số từ danh sách kết quả (giả sử là giải 8)
        List<String> results = getLotteryResults();
        if (results.size() >= 27) {
            // 1 số giải 8: "40"
            return List.of("40");
        }
        return List.of("40"); // Fallback
    }
}

