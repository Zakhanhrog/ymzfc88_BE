package com.xsecret.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetRequest {
    
    @NotBlank(message = "Region không được để trống")
    private String region;
    
    // Province (chỉ cho Miền Trung Nam)
    private String province;
    
    @NotBlank(message = "Bet type không được để trống")
    private String betType;
    
    @NotNull(message = "Selected numbers không được để trống")
    @Size(min = 1, max = 10, message = "Phải chọn ít nhất 1 số và tối đa 10 số")
    private List<String> selectedNumbers;
    
    @NotNull(message = "Bet amount không được để trống")
    @DecimalMin(value = "1.0", message = "Số điểm cược phải lớn hơn 0")
    private BigDecimal betAmount;
    
    @NotNull(message = "Price per point không được để trống")
    @DecimalMin(value = "0.0", message = "Đơn giá không được âm")
    private BigDecimal pricePerPoint;
    
    @NotNull(message = "Odds không được để trống")
    @DecimalMin(value = "1.0", message = "Tỷ lệ cược phải lớn hơn 0")
    private BigDecimal odds;
}
