package com.xsecret.dto.vnlottery;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO cho mỗi kỳ quay trong VnLottery API
 */
@Data
public class VnLotteryIssue {
    
    @JsonProperty("turnNum")
    private String turnNum; // "13/10/2025"
    
    @JsonProperty("openTime")
    private String openTime; // "2025-10-13 18:15:00"
    
    @JsonProperty("detail")
    private String detail; // JSON array string: "[\"56708\",\"28309\",...]"
    
    @JsonProperty("status")
    private Integer status;
    
    // Additional methods for compatibility
    public String getIssue() {
        return turnNum; // Use turnNum as issue
    }
    
    public String getResult() {
        return detail; // Use detail as result
    }
}

