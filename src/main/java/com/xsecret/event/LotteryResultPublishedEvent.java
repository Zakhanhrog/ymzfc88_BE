package com.xsecret.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event được publish khi admin publish kết quả xổ số
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LotteryResultPublishedEvent {
    private Long lotteryResultId;
    private String region;
    private String province;
    private String drawDate;
}
