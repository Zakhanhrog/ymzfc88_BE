package com.xsecret.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateC2PasswordRequest {

    @NotBlank(message = "Mật khẩu bảo vệ không được để trống")
    private String newPassword;
}

