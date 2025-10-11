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
    
    /**
     * Lấy TẤT CẢ 4 số giải 7 (chỉ Miền Bắc)
     * @return List 4 số giải 7 (ví dụ: ["12", "34", "56", "78"])
     */
    List<String> getGiai7Numbers();
    
    /**
     * Lấy TẤT CẢ 3 số giải 6 (chỉ Miền Bắc)
     * @return List 3 số giải 6 (ví dụ: ["034", "005", "095"])
     */
    List<String> getGiai6Numbers();
    
    /**
     * Lấy TẤT CẢ số giải 8 (chỉ Miền Trung Nam)
     * @return List số giải 8 (ví dụ: ["13"]) - chỉ có 1 số
     */
    List<String> getGiai8Numbers();
}

