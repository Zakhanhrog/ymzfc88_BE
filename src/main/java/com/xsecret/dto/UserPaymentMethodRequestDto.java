package com.xsecret.dto;

import com.xsecret.entity.PaymentMethod;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPaymentMethodRequestDto {
    
    @NotBlank(message = "Tên phương thức không được để trống")
    @Size(max = 255, message = "Tên phương thức không được vượt quá 255 ký tự")
    private String name;
    
    @NotNull(message = "Loại phương thức không được để trống")
    private PaymentMethod.PaymentType type;
    
    @NotBlank(message = "Số tài khoản không được để trống")
    @Size(max = 50, message = "Số tài khoản không được vượt quá 50 ký tự")
    private String accountNumber;
    
    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    @Size(max = 255, message = "Tên chủ tài khoản không được vượt quá 255 ký tự")
    private String accountName;
    
    @Size(max = 20, message = "Mã ngân hàng không được vượt quá 20 ký tự")
    private String bankCode;
    
    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String note;
}