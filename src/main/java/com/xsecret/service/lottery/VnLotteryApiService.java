package com.xsecret.service.lottery;

import com.xsecret.dto.vnlottery.VnLotteryIssue;
import com.xsecret.dto.vnlottery.VnLotteryMienBacResponse;
import com.xsecret.dto.vnlottery.VnLotteryProvinceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service gọi API VnLottery để lấy kết quả xổ số
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VnLotteryApiService {
    
    private final RestTemplate restTemplate; // Inject từ RestTemplateConfig
    
    // API URLs
    private static final String MIEN_BAC_API = "https://www.vnlottery.net/api/front/open/lottery/history/list/game?limitNum=1&gameCode=miba";
    private static final String PROVINCE_API_TEMPLATE = "https://www.vnlottery.net/api/front/wl/open/lottery/recent/list/games?gameCodes=%s";
    
    // Game code mapping
    private static final Map<String, String> PROVINCE_GAME_CODE_MAP = new HashMap<>();
    static {
        PROVINCE_GAME_CODE_MAP.put("gialai", "gila");
        PROVINCE_GAME_CODE_MAP.put("ninhthuan", "nith");
        PROVINCE_GAME_CODE_MAP.put("binhduong", "bidu");
        PROVINCE_GAME_CODE_MAP.put("travinh", "trvi");
        PROVINCE_GAME_CODE_MAP.put("vinhlong", "vilo");
    }
    
    /**
     * Lấy kết quả mới nhất Miền Bắc
     */
    public VnLotteryIssue fetchLatestMienBac() {
        try {
            log.info("🌐 Calling VnLottery API for Miền Bắc...");
            
            VnLotteryMienBacResponse response = restTemplate.getForObject(
                MIEN_BAC_API, 
                VnLotteryMienBacResponse.class
            );
            
            if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
                throw new RuntimeException("API returned unsuccessful response");
            }
            
            if (response.getT() == null || response.getT().getIssueList() == null 
                    || response.getT().getIssueList().isEmpty()) {
                throw new RuntimeException("No issue data in response");
            }
            
            VnLotteryIssue latestIssue = response.getT().getIssueList().get(0);
            log.info("✅ Fetched Miền Bắc result for date: {}", latestIssue.getTurnNum());
            
            return latestIssue;
            
        } catch (Exception e) {
            log.error("❌ Error fetching Miền Bắc from VnLottery API: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch Miền Bắc lottery result: " + e.getMessage());
        }
    }
    
    /**
     * Lấy kết quả mới nhất của 1 tỉnh
     * @param province: gialai, ninhthuan, binhduong, travinh, vinhlong
     */
    public VnLotteryIssue fetchLatestProvince(String province) {
        try {
            String gameCode = PROVINCE_GAME_CODE_MAP.get(province);
            if (gameCode == null) {
                throw new RuntimeException("Invalid province: " + province);
            }
            
            String apiUrl = String.format(PROVINCE_API_TEMPLATE, gameCode);
            log.info("🌐 Calling VnLottery API for province: {} ({})", province, gameCode);
            
            VnLotteryProvinceResponse response = restTemplate.getForObject(
                apiUrl,
                VnLotteryProvinceResponse.class
            );
            
            if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
                throw new RuntimeException("API returned unsuccessful response");
            }
            
            if (response.getRows() == null || response.getRows().isEmpty()) {
                throw new RuntimeException("No rows data in response");
            }
            
            // Lấy province data đầu tiên (vì chỉ request 1 gameCode)
            VnLotteryProvinceResponse.ProvinceData provinceData = response.getRows().get(0);
            
            if (provinceData.getIssueList() == null || provinceData.getIssueList().isEmpty()) {
                throw new RuntimeException("No issue data for province: " + province);
            }
            
            VnLotteryIssue latestIssue = provinceData.getIssueList().get(0);
            log.info("✅ Fetched {} result for date: {}", province, latestIssue.getTurnNum());
            
            return latestIssue;
            
        } catch (Exception e) {
            log.error("❌ Error fetching {} from VnLottery API: {}", province, e.getMessage());
            throw new RuntimeException("Failed to fetch " + province + " lottery result: " + e.getMessage());
        }
    }
    
    /**
     * Lấy kết quả tất cả 5 tỉnh Miền Trung/Nam
     */
    public Map<String, VnLotteryIssue> fetchAllProvinces() {
        Map<String, VnLotteryIssue> results = new HashMap<>();
        
        for (String province : PROVINCE_GAME_CODE_MAP.keySet()) {
            try {
                VnLotteryIssue issue = fetchLatestProvince(province);
                results.put(province, issue);
            } catch (Exception e) {
                log.error("❌ Failed to fetch province {}: {}", province, e.getMessage());
                // Continue with other provinces
            }
        }
        
        return results;
    }
}

