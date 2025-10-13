package com.xsecret.service.bet.checker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsecret.entity.Bet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Checker cho Loto 4 số
 * GIỮ NGUYÊN 100% logic từ BetService.checkLoto4sResult()
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Loto4sResultChecker {
    
    private final DatabaseLotteryResultProvider databaseProvider;
    private final ObjectMapper objectMapper;
    
    /**
     * Kiểm tra kết quả loto4s: tìm tất cả số trúng và tính tiền thắng cho từng số
     * Check 4 số cuối của kết quả xổ số
     * COPY CHÍNH XÁC từ BetService.checkLoto4sResult()
     */
    public boolean checkResult(Bet bet) {
        try {
            // Parse selected numbers từ JSON
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy kết quả xổ số từ database
            databaseProvider.setContext(bet);
            List<String> lotteryResults = databaseProvider.getLotteryResults();
            
            // Tìm TẤT CẢ số trúng (không break để đếm được nhiều lần)
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                for (String result : lotteryResults) {
                    if (result.length() >= 4) {
                        String lastFourDigits = result.substring(result.length() - 4);
                        if (selectedNumber.equals(lastFourDigits)) {
                            winningNumbers.add(selectedNumber);
                        }
                    }
                }
            }
            
            if (winningNumbers.isEmpty()) {
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            return true;
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và return false
            log.error("Lỗi check loto4s bet_id={}: {}", bet.getId(), e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Lỗi check loto4s bet_id={}: {}", bet.getId(), e.getMessage());
            return false;
        }
    }
    
    private List<String> parseSelectedNumbers(String selectedNumbersJson) {
        try {
            return objectMapper.readValue(selectedNumbersJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }
    
    private String convertToJsonString(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }
}

