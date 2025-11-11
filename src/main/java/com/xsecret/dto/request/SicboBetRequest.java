package com.xsecret.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SicboBetRequest {

    @NotNull(message = "Thiếu thông tin bàn chơi")
    private Integer tableNumber;

    @NotNull(message = "Thiếu thông tin phiên cược")
    private Long sessionId;

    @NotEmpty(message = "Danh sách cược không được để trống")
    @Valid
    private List<BetItem> bets;

    @Data
    public static class BetItem {
        @NotNull(message = "Thiếu mã cược")
        private String code;

        @NotNull(message = "Số điểm cược phải lớn hơn 0")
        @Min(value = 1, message = "Số điểm cược phải lớn hơn 0")
        private Long amount;
    }
}


