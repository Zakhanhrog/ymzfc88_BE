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
    private static final String PROVINCE_API_TEMPLATE = "https://www.vnlottery.net/api/front/open/lottery/history/list/game?limitNum=5&gameCode=%s";
    
    // Game code mapping: province name → 2 chữ đầu tên tỉnh
    // VD: "Cà Mau" → "cama", "Thừa Thiên Huế" → "thth"
    private static final Map<String, String> PROVINCE_GAME_CODE_MAP = new HashMap<>();
    static {
        // Miền Trung
        PROVINCE_GAME_CODE_MAP.put("phuyen", "phye");
        PROVINCE_GAME_CODE_MAP.put("thuathienhue", "thth");
        PROVINCE_GAME_CODE_MAP.put("daklak", "dakl");
        PROVINCE_GAME_CODE_MAP.put("quangnam", "quna");
        PROVINCE_GAME_CODE_MAP.put("danang", "dana");
        PROVINCE_GAME_CODE_MAP.put("khanhhoa", "khho");
        PROVINCE_GAME_CODE_MAP.put("binhdinh", "bidi");
        PROVINCE_GAME_CODE_MAP.put("quangbinh", "qubi");
        PROVINCE_GAME_CODE_MAP.put("quangtri", "qutr");
        PROVINCE_GAME_CODE_MAP.put("gialai", "gila");
        PROVINCE_GAME_CODE_MAP.put("ninhthuan", "nith");
        PROVINCE_GAME_CODE_MAP.put("daknong", "dano");
        PROVINCE_GAME_CODE_MAP.put("quangngai", "qung");
        PROVINCE_GAME_CODE_MAP.put("kontum", "kont");
        
        // Miền Nam
        PROVINCE_GAME_CODE_MAP.put("camau", "cama");
        PROVINCE_GAME_CODE_MAP.put("dongthap", "doth");
        PROVINCE_GAME_CODE_MAP.put("hcm", "hcm");
        PROVINCE_GAME_CODE_MAP.put("baclieu", "bacl");
        PROVINCE_GAME_CODE_MAP.put("bentre", "bent");
        PROVINCE_GAME_CODE_MAP.put("vungtau", "vuta");
        PROVINCE_GAME_CODE_MAP.put("cantho", "cath");
        PROVINCE_GAME_CODE_MAP.put("dongnai", "dona");                                  
        PROVINCE_GAME_CODE_MAP.put("soctrang", "sotr");
        PROVINCE_GAME_CODE_MAP.put("angiang", "angi");
        PROVINCE_GAME_CODE_MAP.put("binhthuan", "bith");
        PROVINCE_GAME_CODE_MAP.put("tayninh", "tayn");
        PROVINCE_GAME_CODE_MAP.put("binhduong", "bidu");
        PROVINCE_GAME_CODE_MAP.put("travinh", "trvi");
        PROVINCE_GAME_CODE_MAP.put("vinhlong", "vilo");
        PROVINCE_GAME_CODE_MAP.put("binhphuoc", "biph");
        PROVINCE_GAME_CODE_MAP.put("haugiang", "haug");
        PROVINCE_GAME_CODE_MAP.put("longan", "loan");
        PROVINCE_GAME_CODE_MAP.put("dalat", "dala");
        PROVINCE_GAME_CODE_MAP.put("kiengiang", "kigi");
        PROVINCE_GAME_CODE_MAP.put("tiengiang", "tigi");
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
     * @param province: phuyen, thuathienhue, gialai, khanhhoa, camau, hcm, ... (31 tỉnh)
     */
    public VnLotteryIssue fetchLatestProvince(String province) {
        try {
            String gameCode = PROVINCE_GAME_CODE_MAP.get(province);
            if (gameCode == null) {
                log.error("❌ PROVINCE NOT FOUND: {} not in PROVINCE_GAME_CODE_MAP. Available: {}", 
                    province, PROVINCE_GAME_CODE_MAP.keySet());
                throw new RuntimeException("Invalid province: " + province + ". Please check PROVINCE_GAME_CODE_MAP.");
            }
            
            String apiUrl = String.format(PROVINCE_API_TEMPLATE, gameCode);
            log.info("🌐 [DEBUG] Calling VnLottery API for province: {} (gameCode: {})", province, gameCode);
            log.info("🔗 [DEBUG] API URL: {}", apiUrl);
            
            // API trả về format mới: limitNum=5&gameCode=xxx
            VnLotteryMienBacResponse response = restTemplate.getForObject(
                apiUrl,
                VnLotteryMienBacResponse.class
            );
            
            if (response == null || !Boolean.TRUE.equals(response.getSuccess())) {
                throw new RuntimeException("API returned unsuccessful response for " + province);
            }
            
            if (response.getT() == null || response.getT().getIssueList() == null 
                    || response.getT().getIssueList().isEmpty()) {
                throw new RuntimeException("No issue data in response for " + province);
            }
            
            VnLotteryIssue latestIssue = response.getT().getIssueList().get(0);
            log.info("✅ [DEBUG] Fetched {} result for date: {} (issue: {})", 
                province, latestIssue.getTurnNum(), latestIssue.getIssue());
            log.info("🎯 [DEBUG] Result data: {}", latestIssue.getResult());
            
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

