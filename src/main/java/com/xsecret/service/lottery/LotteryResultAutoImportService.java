package com.xsecret.service.lottery;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsecret.dto.request.LotteryResultRequest;
import com.xsecret.dto.vnlottery.VnLotteryIssue;
import com.xsecret.service.LotteryResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Service tự động import kết quả từ VnLottery vào database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LotteryResultAutoImportService {
    
    private final VnLotteryApiService vnLotteryApiService;
    private final LotteryResultService lotteryResultService;
    private final ObjectMapper objectMapper;
    
    // Timezone Vietnam
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    
    // Date formatter cho turnNum: "13/10/2025" -> "2025-10-13"
    private static final DateTimeFormatter TURN_NUM_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    /**
     * Tự động import kết quả Miền Bắc
     */
    public void autoImportMienBac() {
        try {
            log.info("🔄 Starting auto import for Miền Bắc...");
            
            // 1. Fetch từ API
            VnLotteryIssue issue = vnLotteryApiService.fetchLatestMienBac();
            
            // 2. Parse và tạo request
            LotteryResultRequest request = buildMienBacRequest(issue);
            
            // 3. Insert vào DB
            insertToDatabase(request, "Miền Bắc");
            
        } catch (RuntimeException e) {
            // Nếu là lỗi "chưa có kết quả hôm nay" thì chỉ log warning, không throw exception
            if (e.getMessage() != null && e.getMessage().contains("không phải ngày hôm nay")) {
                log.warn("⚠️ Miền Bắc: {}", e.getMessage());
                return; // Skip import, không throw exception
            }
            
            // Các lỗi khác thì vẫn throw
            log.error("❌ Auto import Miền Bắc failed: {}", e.getMessage());
            throw new RuntimeException("Auto import Miền Bắc failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ Auto import Miền Bắc failed: {}", e.getMessage());
            throw new RuntimeException("Auto import Miền Bắc failed: " + e.getMessage());
        }
    }
    
    /**
     * Tự động import kết quả 1 tỉnh
     */
    public void autoImportProvince(String province) {
        try {
            log.info("🔄 Starting auto import for province: {}", province);
            
            // 1. Fetch từ API
            VnLotteryIssue issue = vnLotteryApiService.fetchLatestProvince(province);
            
            // 2. Parse và tạo request
            LotteryResultRequest request = buildProvinceRequest(issue, province);
            
            // 3. Insert vào DB
            insertToDatabase(request, province);
            
        } catch (RuntimeException e) {
            // Nếu là lỗi "chưa có kết quả hôm nay" thì chỉ log warning, không throw exception
            if (e.getMessage() != null && e.getMessage().contains("không phải ngày hôm nay")) {
                log.warn("⚠️ {}: {}", province, e.getMessage());
                return; // Skip import, không throw exception
            }
            
            // Các lỗi khác thì vẫn throw
            log.error("❌ Auto import {} failed: {}", province, e.getMessage());
            throw new RuntimeException("Auto import " + province + " failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ Auto import {} failed: {}", province, e.getMessage());
            throw new RuntimeException("Auto import " + province + " failed: " + e.getMessage());
        }
    }
    
    /**
     * Tự động import tất cả 5 tỉnh
     */
    public void autoImportAllProvinces() {
        List<String> provinces = Arrays.asList("gialai", "ninhthuan", "binhduong", "travinh", "vinhlong");
        
        int successCount = 0;
        int skipCount = 0;
        
        for (String province : provinces) {
            try {
                autoImportProvince(province);
                successCount++;
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("không phải ngày hôm nay")) {
                    skipCount++;
                    log.warn("⚠️ Skipped {}: {}", province, e.getMessage());
                } else {
                    log.error("❌ Failed to import {}: {}", province, e.getMessage());
                }
                // Continue with other provinces
            }
        }
        
        log.info("📊 Import summary: {} success, {} skipped (no result today), {} failed", 
                successCount, skipCount, provinces.size() - successCount - skipCount);
    }
    
    /**
     * Build request cho Miền Bắc
     */
    private LotteryResultRequest buildMienBacRequest(VnLotteryIssue issue) throws Exception {
        // Validate turnNum
        if (issue.getTurnNum() == null || issue.getTurnNum().trim().isEmpty()) {
            throw new RuntimeException("API trả về turnNum null hoặc rỗng");
        }
        
        // Parse turnNum: "13/10/2025" -> "2025-10-13"
        LocalDate drawDate;
        try {
            drawDate = LocalDate.parse(issue.getTurnNum(), TURN_NUM_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid date format from API: " + issue.getTurnNum() + ". Expected: dd/MM/yyyy");
        }
        
        // ⚠️ QUAN TRỌNG: Chỉ import kết quả của ngày hôm nay (theo giờ VN)
        LocalDate today = LocalDate.now(VN_ZONE);
        if (!drawDate.equals(today)) {
            throw new RuntimeException(String.format(
                "Miền Bắc kết quả không phải ngày hôm nay. API trả về ngày: %s, hôm nay (VN): %s. " +
                "Chờ đến khi có kết quả ngày %s.", 
                drawDate, today, today));
        }
        
        String drawDateStr = drawDate.format(ISO_DATE_FORMATTER);
        
        // Parse detail array
        String resultsJson = parseMienBacDetail(issue.getDetail());
        
        return LotteryResultRequest.builder()
                .region("mienBac")
                .province(null)
                .drawDate(drawDateStr)
                .results(resultsJson)
                .status("PUBLISHED") // Auto publish
                .build();
    }
    
    /**
     * Build request cho tỉnh Miền Trung/Nam
     */
    private LotteryResultRequest buildProvinceRequest(VnLotteryIssue issue, String province) throws Exception {
        // Validate turnNum
        if (issue.getTurnNum() == null || issue.getTurnNum().trim().isEmpty()) {
            throw new RuntimeException("API trả về turnNum null hoặc rỗng cho " + province);
        }
        
        // Parse turnNum
        LocalDate drawDate;
        try {
            drawDate = LocalDate.parse(issue.getTurnNum(), TURN_NUM_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Invalid date format from API for " + province + ": " + issue.getTurnNum() + ". Expected: dd/MM/yyyy");
        }
        
        // ⚠️ QUAN TRỌNG: Chỉ import kết quả của ngày hôm nay (theo giờ VN)
        LocalDate today = LocalDate.now(VN_ZONE);
        if (!drawDate.equals(today)) {
            throw new RuntimeException(String.format(
                "%s kết quả không phải ngày hôm nay. API trả về ngày: %s, hôm nay (VN): %s. " +
                "Chờ đến khi có kết quả ngày %s.", 
                province, drawDate, today, today));
        }
        
        String drawDateStr = drawDate.format(ISO_DATE_FORMATTER);
        
        // Parse detail array
        String resultsJson = parseProvinceDetail(issue.getDetail());
        
        return LotteryResultRequest.builder()
                .region("mienTrungNam")
                .province(province)
                .drawDate(drawDateStr)
                .results(resultsJson)
                .status("PUBLISHED") // Auto publish
                .build();
    }
    
    /**
     * Parse detail array Miền Bắc thành JSON format
     * Input: "[\"56708\",\"28309\",\"14066,54388\",\"02034,80922,00829,76262,57800,06839\",\"6613,3765,1875,7381\",\"2577,2808,3600,1919,4560,2403\",\"039,767,147\",\"25,42,72,87\"]"
     * Output: {"dac-biet":"56708","giai-nhat":"28309","giai-nhi":["14066","54388"],...}
     */
    private String parseMienBacDetail(String detailStr) throws Exception {
        // Validate input
        if (detailStr == null || detailStr.trim().isEmpty()) {
            throw new RuntimeException("API trả về detail null hoặc rỗng cho Miền Bắc");
        }
        
        // Parse JSON array string
        List<String> detailArray = objectMapper.readValue(detailStr, new TypeReference<List<String>>() {});
        
        if (detailArray == null || detailArray.size() != 8) {
            throw new RuntimeException("Invalid Miền Bắc detail array size: " + 
                (detailArray == null ? "null" : detailArray.size()) + ". Expected: 8");
        }
        
        Map<String, Object> results = new LinkedHashMap<>();
        
        // Index 0: Đặc biệt
        results.put("dac-biet", detailArray.get(0));
        
        // Index 1: Giải nhất
        results.put("giai-nhat", detailArray.get(1));
        
        // Index 2: Giải nhì (2 số)
        results.put("giai-nhi", splitNumbers(detailArray.get(2)));
        
        // Index 3: Giải ba (6 số)
        results.put("giai-ba", splitNumbers(detailArray.get(3)));
        
        // Index 4: Giải tư (4 số)
        results.put("giai-tu", splitNumbers(detailArray.get(4)));
        
        // Index 5: Giải năm (6 số)
        results.put("giai-nam", splitNumbers(detailArray.get(5)));
        
        // Index 6: Giải sáu (3 số)
        results.put("giai-sau", splitNumbers(detailArray.get(6)));
        
        // Index 7: Giải bảy (4 số)
        results.put("giai-bay", splitNumbers(detailArray.get(7)));
        
        return objectMapper.writeValueAsString(results);
    }
    
    /**
     * Parse detail array Miền Trung/Nam thành JSON format
     * Input: "[\"179313\",\"90990\",\"73722\",\"74575,08379\",\"74116,41034,22817,21311,52968,52665,71554\",\"1353\",\"7701,9382,5690\",\"878\",\"15\"]"
     * Output: {"dac-biet":"179313","giai-nhat":"90990","giai-nhi":"73722","giai-ba":["74575","08379"],...,"giai-tam":"15"}
     */
    private String parseProvinceDetail(String detailStr) throws Exception {
        // Validate input
        if (detailStr == null || detailStr.trim().isEmpty()) {
            throw new RuntimeException("API trả về detail null hoặc rỗng cho tỉnh");
        }
        
        // Parse JSON array string
        List<String> detailArray = objectMapper.readValue(detailStr, new TypeReference<List<String>>() {});
        
        if (detailArray == null || detailArray.size() != 9) {
            throw new RuntimeException("Invalid Province detail array size: " + 
                (detailArray == null ? "null" : detailArray.size()) + ". Expected: 9");
        }
        
        Map<String, Object> results = new LinkedHashMap<>();
        
        // Index 0: Đặc biệt
        results.put("dac-biet", detailArray.get(0));
        
        // Index 1: Giải nhất
        results.put("giai-nhat", detailArray.get(1));
        
        // Index 2: Giải nhì (1 số, nhưng có thể có nhiều nên dùng array)
        String giaiNhi = detailArray.get(2);
        results.put("giai-nhi", giaiNhi.contains(",") ? splitNumbers(giaiNhi) : Arrays.asList(giaiNhi));
        
        // Index 3: Giải ba (2 số)
        results.put("giai-ba", splitNumbers(detailArray.get(3)));
        
        // Index 4: Giải tư (7 số)
        results.put("giai-tu", splitNumbers(detailArray.get(4)));
        
        // Index 5: Giải năm (1 số, nhưng dùng array)
        String giaiNam = detailArray.get(5);
        results.put("giai-nam", giaiNam.contains(",") ? splitNumbers(giaiNam) : Arrays.asList(giaiNam));
        
        // Index 6: Giải sáu (3 số)
        results.put("giai-sau", splitNumbers(detailArray.get(6)));
        
        // Index 7: Giải bảy (1 số, nhưng dùng array)
        String giaiBay = detailArray.get(7);
        results.put("giai-bay", giaiBay.contains(",") ? splitNumbers(giaiBay) : Arrays.asList(giaiBay));
        
        // Index 8: Giải tám
        String giaiTam = detailArray.get(8);
        results.put("giai-tam", giaiTam.contains(",") ? splitNumbers(giaiTam) : giaiTam);
        
        return objectMapper.writeValueAsString(results);
    }
    
    /**
     * Split comma-separated numbers: "66146,15901" -> ["66146", "15901"]
     */
    private List<String> splitNumbers(String numbers) {
        if (numbers == null || numbers.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(numbers.split(","));
    }
    
    /**
     * Insert vào database
     */
    private void insertToDatabase(LotteryResultRequest request, String name) {
        try {
            lotteryResultService.createLotteryResult(request);
            log.info("✅ Successfully imported {} result for date: {}", name, request.getDrawDate());
        } catch (RuntimeException e) {
            // Check if it's a duplicate error
            if (e.getMessage() != null && e.getMessage().contains("đã tồn tại")) {
                log.warn("⚠️ {} result for {} already exists, skipping", name, request.getDrawDate());
            } else {
                throw e;
            }
        }
    }
}

