package com.xsecret.dto.request;

import com.xsecret.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequestDto {
    
    @Size(min = 3, max = 20, message = "Tên đăng nhập phải có từ 3-20 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới")
    private String username;
    
    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;
    
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;
    
    private User.Role role;
    
    private User.UserStatus status;

    // Phân loại đại lý (AGENT) – chỉ dùng cho hiển thị / quản lý, không dành cho nhân viên
    private User.StaffRole staffRole;

    private String c2Password;
    
    @Size(max = 10, message = "Mã đại lý không được vượt quá 10 ký tự")
    @Pattern(regexp = "^[A-Z0-9]*$", message = "Mã đại lý chỉ được chứa chữ cái in hoa và số")
    private String referralCode;
}