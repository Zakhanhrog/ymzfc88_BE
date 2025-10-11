package com.xsecret.service.bet.checker;

import java.util.List;

/**
 * Interface cung cấp kết quả xổ số
 * Sau này sẽ implement để lấy từ API thật theo từng vùng
 */
public interface LotteryResultProvider {
    
    /**
     * Lấy danh sách kết quả xổ số
     * @return List các số kết quả (ví dụ: "01640", "54778", ...)
     */
    List<String> getLotteryResults();
    
    /**
     * Lấy số giải đặc biệt
     * @return Số giải đặc biệt (ví dụ: "01640")
     */
    String getDacBietNumber();
    
    /**
     * Lấy số giải nhất
     * @return Số giải nhất (ví dụ: "54778")
     */
    String getGiaiNhatNumber();
    
    /**
     * Lấy số giải 8 (chỉ Miền Trung Nam)
     * @return Số giải 8 (ví dụ: "13", "01", "45")
     */
    String getGiai8Number();
    
    /**
     * Lấy số giải 7 (chỉ Miền Trung Nam)
     * @return Số giải 7 (ví dụ: "138", "001", "999")
     */
    String getGiai7Number();
}

