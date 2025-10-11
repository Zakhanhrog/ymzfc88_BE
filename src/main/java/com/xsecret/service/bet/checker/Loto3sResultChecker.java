package com.xsecret.service.bet.checker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsecret.entity.Bet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Checker cho Loto 3 số
 * GIỮ NGUYÊN 100% logic từ BetService.checkLoto3sResult()
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Loto3sResultChecker {
    
    private final LotteryResultProviderFactory providerFactory;
    private final ObjectMapper objectMapper;
    
    /**
     * Kiểm tra kết quả loto3s: tìm tất cả số trúng và tính tiền thắng cho từng số
     * Check 3 số cuối của kết quả xổ số
     * COPY CHÍNH XÁC từ BetService.checkLoto3sResult()
     */
    public boolean checkResult(Bet bet) {
        try {
            // Parse selected numbers từ JSON
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy kết quả xổ số theo region
            LotteryResultProvider resultProvider = providerFactory.getProvider(bet.getRegion());
            List<String> lotteryResults = resultProvider.getLotteryResults();
            
            // Tìm TẤT CẢ số trúng (không break để đếm được nhiều lần)
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                for (String result : lotteryResults) {
                    if (result.length() >= 3) {
                        String lastThreeDigits = result.substring(result.length() - 3);
                        if (selectedNumber.equals(lastThreeDigits)) {
                            winningNumbers.add(selectedNumber);
                            log.info("Loto3s WIN: Selected {} matches last 3 digits of {}", selectedNumber, result);
                            // KHÔNG BREAK để có thể tìm thấy số trúng nhiều lần
                        }
                    }
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("Loto3s LOSE: No matches found for selected numbers: {}", selectedNumbers);
                return false;
            }
            
            // Lưu danh sách số trúng vào bet
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("Loto3s WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking loto3s result: {}", e.getMessage());
            return false;
        }
    }
    
    private List<String> parseSelectedNumbers(String selectedNumbersJson) {
        try {
            return objectMapper.readValue(selectedNumbersJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.error("Error parsing selected numbers: {}", e.getMessage());
            return List.of();
        }
    }
    
    private String convertToJsonString(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("Error converting list to JSON: {}", e.getMessage());
            return "[]";
        }
    }
}

