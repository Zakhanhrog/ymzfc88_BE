package com.xsecret.service.bet.checker;

import org.springframework.stereotype.Component;
import java.util.List;

/**
 * DEPRECATED: Mock provider đã bị xóa - chỉ sử dụng DatabaseLotteryResultProvider
 * File này giữ lại để tham khảo cấu trúc dữ liệu nhưng không được sử dụng
 */
@Component("mockLotteryResultProvider")
public class MockLotteryResultProvider implements LotteryResultProvider {
    
    @Override
    public List<String> getLotteryResults() {
        // DEPRECATED: Không sử dụng mock data nữa - chỉ lấy từ database
        throw new UnsupportedOperationException("Mock data đã bị vô hiệu hóa - chỉ sử dụng DatabaseLotteryResultProvider");
    }
    
    @Override
    public String getDacBietNumber() {
        throw new UnsupportedOperationException("Mock data đã bị vô hiệu hóa - chỉ sử dụng DatabaseLotteryResultProvider");
    }
    
    @Override
    public String getGiaiNhatNumber() {
        throw new UnsupportedOperationException("Mock data đã bị vô hiệu hóa - chỉ sử dụng DatabaseLotteryResultProvider");
    }
    
    @Override
    public String getGiai8Number() {
        throw new UnsupportedOperationException("Mock data đã bị vô hiệu hóa - chỉ sử dụng DatabaseLotteryResultProvider");
    }
    
    @Override
    public String getGiai7Number() {
        throw new UnsupportedOperationException("Mock data đã bị vô hiệu hóa - chỉ sử dụng DatabaseLotteryResultProvider");
    }
    
    @Override
    public List<String> getGiai7Numbers() {
        throw new UnsupportedOperationException("Mock data đã bị vô hiệu hóa - chỉ sử dụng DatabaseLotteryResultProvider");
    }
    
    @Override
    public List<String> getGiai6Numbers() {
        throw new UnsupportedOperationException("Mock data đã bị vô hiệu hóa - chỉ sử dụng DatabaseLotteryResultProvider");
    }
    
    @Override
    public List<String> getGiai8Numbers() {
        throw new UnsupportedOperationException("Mock data đã bị vô hiệu hóa - chỉ sử dụng DatabaseLotteryResultProvider");
    }
}

