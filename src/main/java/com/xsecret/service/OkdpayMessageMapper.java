package com.xsecret.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapper để chuyển đổi message tiếng Trung từ OKDPAY sang tiếng Việt
 */
public class OkdpayMessageMapper {
    
    private static final Map<String, String> MESSAGE_MAP = new HashMap<>();
    
    static {
        // Lỗi hệ thống bận
        MESSAGE_MAP.put("系统繁忙", "Hệ thống thanh toán đang bận. Vui lòng thử lại sau ít phút hoặc chọn phương thức khác.");
        MESSAGE_MAP.put("请稍后再试", "Hệ thống thanh toán đang bận. Vui lòng thử lại sau ít phút hoặc chọn phương thức khác.");
        MESSAGE_MAP.put("系统繁忙,请稍后再试", "Hệ thống thanh toán đang bận. Vui lòng thử lại sau ít phút hoặc chọn phương thức khác.");
        
        // Lỗi channel bảo trì
        MESSAGE_MAP.put("通道维护中", "Kênh thanh toán đang bảo trì. Vui lòng thử lại sau hoặc chọn phương thức khác.");
        MESSAGE_MAP.put("通道维护中3", "Kênh thanh toán đang bảo trì. Vui lòng thử lại sau hoặc chọn phương thức khác.");
        
        // Lỗi validation
        MESSAGE_MAP.put("商户订单号校验错误", "Lỗi xác thực đơn hàng. Vui lòng thử lại.");
        MESSAGE_MAP.put("订单号", "Lỗi xác thực đơn hàng. Vui lòng thử lại.");
        
        // Lỗi khác
        MESSAGE_MAP.put("error", "Lỗi thanh toán. Vui lòng thử lại sau hoặc chọn phương thức khác.");
    }
    
    /**
     * Chuyển đổi message tiếng Trung sang tiếng Việt
     */
    public static String translateMessage(String chineseMessage) {
        if (chineseMessage == null || chineseMessage.trim().isEmpty()) {
            return "Lỗi thanh toán. Vui lòng thử lại sau hoặc chọn phương thức khác.";
        }
        
        // Kiểm tra exact match trước
        if (MESSAGE_MAP.containsKey(chineseMessage)) {
            return MESSAGE_MAP.get(chineseMessage);
        }
        
        // Kiểm tra contains (cho các message có thêm thông tin)
        for (Map.Entry<String, String> entry : MESSAGE_MAP.entrySet()) {
            if (chineseMessage.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // Nếu không tìm thấy, trả về message mặc định
        return "Hệ thống thanh toán đang bận. Vui lòng thử lại sau ít phút hoặc chọn phương thức khác.";
    }
    
    /**
     * Kiểm tra xem message có phải là lỗi bận (có thể retry) không
     */
    public static boolean isRetryableError(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        return message.contains("系统繁忙") || message.contains("请稍后再试");
    }
}

