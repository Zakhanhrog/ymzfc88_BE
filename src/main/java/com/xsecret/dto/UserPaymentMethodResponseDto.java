package com.xsecret.dto;

import com.xsecret.entity.PaymentMethod;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPaymentMethodResponseDto {
    
    private Long id;
    private String name;
    private PaymentMethod.PaymentType type;
    private String accountNumber;
    private String accountName;
    private String bankCode;
    private String note;
    private Boolean isDefault;
    private Boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Helper method để lấy tên loại phương thức
    public String getTypeDisplayName() {
        if (type == null) return "";
        
        switch (type) {
            case BANK:
                return "Ngân hàng";
            case MOMO:
                return "Ví MoMo";
            case ZALO_PAY:
                return "ZaloPay";
            case VIET_QR:
                return "VietQR";
            case USDT:
                return "USDT";
            default:
                return type.toString();
        }
    }
    
    // Helper method để lấy tên hiển thị đầy đủ
    public String getDisplayName() {
        if (PaymentMethod.PaymentType.BANK.equals(type) && bankCode != null) {
            return String.format("%s - %s (%s)", name, bankCode, accountNumber);
        }
        return String.format("%s (%s)", name, accountNumber);
    }
}