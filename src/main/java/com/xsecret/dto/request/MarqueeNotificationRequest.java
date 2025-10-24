package com.xsecret.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class MarqueeNotificationRequest {
    
    @NotBlank(message = "Nội dung thông báo không được để trống")
    @Size(max = 1000, message = "Nội dung thông báo không được vượt quá 1000 ký tự")
    private String content;
    
    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean isActive;
    
    private Integer displayOrder;
    
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Màu chữ phải là mã hex hợp lệ")
    private String textColor;
    
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "Màu nền phải là mã hex hợp lệ")
    private String backgroundColor;
    
    private Integer fontSize;
    
    private Integer speed;
}
