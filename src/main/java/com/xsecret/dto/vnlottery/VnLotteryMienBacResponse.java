package com.xsecret.dto.vnlottery;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO cho API response Miền Bắc
 * https://www.vnlottery.net/api/front/open/lottery/history/list/game?limitNum=1&gameCode=miba
 */
@Data
public class VnLotteryMienBacResponse {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("msg")
    private String msg;
    
    @JsonProperty("code")
    private Integer code;
    
    @JsonProperty("t")
    private TData t;
    
    @Data
    public static class TData {
        @JsonProperty("name")
        private String name; // "Miền Bắc"
        
        @JsonProperty("code")
        private String code; // "miba"
        
        @JsonProperty("issueList")
        private List<VnLotteryIssue> issueList;
    }
}

