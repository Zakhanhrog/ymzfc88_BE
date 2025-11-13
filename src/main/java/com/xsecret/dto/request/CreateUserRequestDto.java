package com.xsecret.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequestDto {
    
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;
    
    @NotBlank(message = "Email không được để trống")
    private String email;
    
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
    
    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;
    
    private String phoneNumber;
    
    @NotNull(message = "Vai trò không được để trống")
    private String role; // USER hoặc ADMIN

    private String staffRole;

    private String referralCode;

    private String invitedByCode;
}