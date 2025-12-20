package com.xsecret.service;

import com.xsecret.entity.PaymentMethod;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper để map PaymentType sang OKDPAY channel code và min amount
 */
public class OkdpayChannelMapper {
    
    // Mapping PaymentType -> ChannelCode
    private static final Map<PaymentMethod.PaymentType, String> TYPE_TO_CHANNEL = new HashMap<>();
    
    // Mapping ChannelCode -> MinAmount (VNĐ)
    private static final Map<String, BigDecimal> CHANNEL_TO_MIN_AMOUNT = new HashMap<>();
    
    // Mapping ChannelCode -> MaxAmount (VNĐ)
    private static final Map<String, BigDecimal> CHANNEL_TO_MAX_AMOUNT = new HashMap<>();
    
    static {
        // Mapping PaymentType -> ChannelCode (theo mẫu autobank.js)
        // Mẫu: type == 'momo' ? 1002 : (type == 'zalopay' ? 1005 : 1003)
        TYPE_TO_CHANNEL.put(PaymentMethod.PaymentType.MOMO, "1002");      // Momo → 1002 (theo mẫu)
        TYPE_TO_CHANNEL.put(PaymentMethod.PaymentType.ZALO_PAY, "1005");  // ZaloPay → 1005 (theo mẫu)
        TYPE_TO_CHANNEL.put(PaymentMethod.PaymentType.BANK, "1003");      // Bank → 1003 (mặc định theo mẫu)
        TYPE_TO_CHANNEL.put(PaymentMethod.PaymentType.VIET_QR, "1003");   // VietQR → 1003 (mặc định theo mẫu)
        
        // Mapping ChannelCode -> MinAmount (VNĐ)
        // Theo tài liệu OKDPAY chính thức:
        CHANNEL_TO_MIN_AMOUNT.put("1001", BigDecimal.valueOf(50000));  // 5万 = 50,000 VND
        CHANNEL_TO_MIN_AMOUNT.put("1002", BigDecimal.valueOf(50000));  // 5万 = 50,000 VND (không dùng trong code)
        CHANNEL_TO_MIN_AMOUNT.put("1003", BigDecimal.valueOf(50000));  // 5万 = 50,000 VND
        CHANNEL_TO_MIN_AMOUNT.put("1004", BigDecimal.valueOf(10000));  // 1万 = 10,000 VND
        CHANNEL_TO_MIN_AMOUNT.put("1005", BigDecimal.valueOf(50000));  // 5万 = 50,000 VND
        
        // Mapping ChannelCode -> MaxAmount (VNĐ)
        // Theo tài liệu OKDPAY chính thức:
        CHANNEL_TO_MAX_AMOUNT.put("1001", BigDecimal.valueOf(3000000000L));  // 3亿 = 3,000,000,000 VND (3 tỷ)
        CHANNEL_TO_MAX_AMOUNT.put("1002", BigDecimal.valueOf(500000000L));  // 5000万 = 500,000,000 VND (500 triệu) - không dùng
        CHANNEL_TO_MAX_AMOUNT.put("1003", BigDecimal.valueOf(3000000000L));  // 3亿 = 3,000,000,000 VND (3 tỷ)
        CHANNEL_TO_MAX_AMOUNT.put("1004", BigDecimal.valueOf(100000000L));  // 1000万 = 100,000,000 VND (100 triệu)
        CHANNEL_TO_MAX_AMOUNT.put("1005", BigDecimal.valueOf(500000000L));  // 5000万 = 500,000,000 VND (500 triệu)
    }
    
    /**
     * Lấy channel code tương ứng với PaymentType
     * @return channel code hoặc null nếu không có mapping
     */
    public static String getChannelCodeForType(PaymentMethod.PaymentType type) {
        return TYPE_TO_CHANNEL.get(type);
    }
    
    /**
     * Lấy số tiền tối thiểu cho channel code
     * @return min amount hoặc null nếu không có mapping
     */
    public static BigDecimal getMinAmountForChannel(String channelCode) {
        return CHANNEL_TO_MIN_AMOUNT.get(channelCode);
    }
    
    /**
     * Lấy số tiền tối đa cho channel code
     * @return max amount hoặc null nếu không có mapping
     */
    public static BigDecimal getMaxAmountForChannel(String channelCode) {
        return CHANNEL_TO_MAX_AMOUNT.get(channelCode);
    }
    
    /**
     * Kiểm tra xem PaymentType có hỗ trợ auto deposit không
     */
    public static boolean supportsAutoDeposit(PaymentMethod.PaymentType type) {
        return TYPE_TO_CHANNEL.containsKey(type);
    }
    
    /**
     * Lấy tất cả các channel codes có sẵn
     */
    public static Map<String, BigDecimal> getAllChannels() {
        return new HashMap<>(CHANNEL_TO_MIN_AMOUNT);
    }
    
    /**
     * Lấy tên mô tả cho channel code
     */
    public static String getChannelName(String channelCode) {
        switch (channelCode) {
            case "1001": return "银行卡转账 (Chuyển khoản ngân hàng)";
            case "1002": return "Momo";
            case "1003": return "网银扫码 (Quét mã ngân hàng)";
            case "1004": return "MomoPay原生 (MomoPay Native)";
            case "1005": return "ZALO";
            default: return "Channel" + channelCode;
        }
    }
    
    /**
     * Kiểm tra xem channel code có phải là kênh chuyển thẻ (转卡通道) không
     * Theo tài liệu: "转卡通道需填写" submitname (打款人)
     * Kênh chuyển thẻ: 1001 (银行卡转账), 1003 (网银扫码)
     */
    public static boolean requiresSubmitName(String channelCode) {
        return "1001".equals(channelCode) || "1003".equals(channelCode);
    }
}

