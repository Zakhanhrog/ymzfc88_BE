package com.xsecret.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class XocDiaBetRequest {

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
        @Positive(message = "Số điểm cược phải lớn hơn 0")
        private Long amount;
    }
}


