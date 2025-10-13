package com.xsecret.dto.vnlottery;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO cho API response Miền Trung/Nam (provinces)
 * https://www.vnlottery.net/api/front/wl/open/lottery/recent/list/games?gameCodes=gila,nith
 */
@Data
public class VnLotteryProvinceResponse {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("msg")
    private String msg;
    
    @JsonProperty("code")
    private Integer code;
    
    @JsonProperty("rows")
    private List<ProvinceData> rows;
    
    @Data
    public static class ProvinceData {
        @JsonProperty("name")
        private String name; // "Gia Lai"
        
        @JsonProperty("code")
        private String code; // "gila"
        
        @JsonProperty("issueList")
        private List<VnLotteryIssue> issueList;
    }
}

