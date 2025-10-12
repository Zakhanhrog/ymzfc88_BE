package com.xsecret.service.bet.checker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsecret.entity.Bet;
import com.xsecret.entity.LotteryResult;
import com.xsecret.service.LotteryResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provider lấy kết quả xổ số từ database
 * Thay thế MockLotteryResultProvider
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseLotteryResultProvider implements LotteryResultProvider {
    
    private final LotteryResultService lotteryResultService;
    private final ObjectMapper objectMapper;
    
    // Context của bet hiện tại (để biết lấy kết quả nào)
    private Bet currentBet;
    private LotteryResult cachedResult;
    
    /**
     * Set context để provider biết lấy kết quả nào
     */
    public void setContext(Bet bet) {
        this.currentBet = bet;
        this.cachedResult = null; // Clear cache
        loadResult();
    }
    
    /**
     * Load kết quả từ database
     */
    private void loadResult() {
        if (currentBet == null) {
            return;
        }
        
        try {
            String region = currentBet.getRegion();
            String province = currentBet.getProvince();
            LocalDate drawDate = LocalDate.parse(currentBet.getResultDate(), DateTimeFormatter.ISO_LOCAL_DATE);
            
            cachedResult = lotteryResultService.getPublishedResultForBetCheck(region, province, drawDate);
            
            if (cachedResult == null) {
                log.error("Không tìm thấy kết quả xổ số published: region={}, province={}, drawDate={}", 
                        region, province, drawDate);
            }
        } catch (Exception e) {
            log.error("Lỗi khi load kết quả xổ số: bet_id={}, error={}", currentBet.getId(), e.getMessage());
            cachedResult = null;
        }
    }
    
    @Override
    public List<String> getLotteryResults() {
        if (cachedResult == null) {
            return List.of();
        }
        
        try {
            Map<String, Object> resultsMap = objectMapper.readValue(cachedResult.getResults(), Map.class);
            List<String> allNumbers = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : resultsMap.entrySet()) {
                Object value = entry.getValue();
                
                if (value instanceof String) {
                    allNumbers.add((String) value);
                } else if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    for (Object item : list) {
                        if (item instanceof String) {
                            allNumbers.add((String) item);
                        }
                    }
                }
            }
            
            return allNumbers;
            
        } catch (Exception e) {
            log.error("Lỗi parse JSON kết quả xổ số: {}", e.getMessage());
            return List.of();
        }
    }
    
    @Override
    public String getDacBietNumber() {
        if (cachedResult == null) {
            return null;
        }
        
        try {
            Map<String, Object> resultsMap = objectMapper.readValue(cachedResult.getResults(), Map.class);
            Object dacBiet = resultsMap.get("dac-biet");
            return dacBiet != null ? dacBiet.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public String getGiaiNhatNumber() {
        if (cachedResult == null) {
            return null;
        }
        
        try {
            Map<String, Object> resultsMap = objectMapper.readValue(cachedResult.getResults(), Map.class);
            Object giaiNhat = resultsMap.get("giai-nhat");
            return giaiNhat != null ? giaiNhat.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public String getGiai8Number() {
        if (cachedResult == null) {
            return null;
        }
        
        try {
            Map<String, Object> resultsMap = objectMapper.readValue(cachedResult.getResults(), Map.class);
            Object giai8 = resultsMap.get("giai-tam");
            
            if (giai8 instanceof List) {
                List<?> list = (List<?>) giai8;
                return !list.isEmpty() ? list.get(0).toString() : null;
            } else if (giai8 instanceof String) {
                return (String) giai8;
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public String getGiai7Number() {
        if (cachedResult == null) {
            return null;
        }
        
        try {
            Map<String, Object> resultsMap = objectMapper.readValue(cachedResult.getResults(), Map.class);
            Object giai7 = resultsMap.get("giai-bay");
            
            if (giai7 instanceof List) {
                List<?> list = (List<?>) giai7;
                return !list.isEmpty() ? list.get(0).toString() : null;
            } else if (giai7 instanceof String) {
                return (String) giai7;
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public List<String> getGiai7Numbers() {
        if (cachedResult == null) {
            return List.of();
        }
        
        try {
            Map<String, Object> resultsMap = objectMapper.readValue(cachedResult.getResults(), Map.class);
            Object giai7 = resultsMap.get("giai-bay");
            
            if (giai7 instanceof List) {
                List<?> list = (List<?>) giai7;
                return list.stream()
                        .map(Object::toString)
                        .toList();
            } else if (giai7 instanceof String) {
                return List.of((String) giai7);
            }
            
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
    
    @Override
    public List<String> getGiai6Numbers() {
        if (cachedResult == null) {
            return List.of();
        }
        
        try {
            Map<String, Object> resultsMap = objectMapper.readValue(cachedResult.getResults(), Map.class);
            Object giai6 = resultsMap.get("giai-sau");
            
            if (giai6 instanceof List) {
                List<?> list = (List<?>) giai6;
                return list.stream()
                        .map(Object::toString)
                        .toList();
            } else if (giai6 instanceof String) {
                return List.of((String) giai6);
            }
            
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
    
    @Override
    public List<String> getGiai8Numbers() {
        if (cachedResult == null) {
            return List.of();
        }
        
        try {
            Map<String, Object> resultsMap = objectMapper.readValue(cachedResult.getResults(), Map.class);
            Object giai8 = resultsMap.get("giai-tam");
            
            if (giai8 instanceof List) {
                List<?> list = (List<?>) giai8;
                return list.stream()
                        .map(Object::toString)
                        .toList();
            } else if (giai8 instanceof String) {
                return List.of((String) giai8);
            }
            
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
}

