package com.xsecret.service.bet.checker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsecret.entity.Bet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Checker cho Loto Trượt 4
 * Logic: Chọn 4 số, CẢ 4 số đều KHÔNG trúng → THẮNG
 * Nếu có BẤT KỲ 1 số nào trúng → THUA
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TruotResultChecker {
    
    private final LotteryResultProviderFactory providerFactory;
    private final ObjectMapper objectMapper;
    
    /**
     * Kiểm tra kết quả Loto trượt 4
     * Format: ["12,23,34,45", "56,67,78,89"] - mỗi cụm 4 số
     * THẮNG: CẢ 4 số đều KHÔNG trúng (trượt hết)
     */
    public boolean checkTruot4Result(Bet bet) {
        try {
            List<String> selectedGroups = parseSelectedNumbers(bet.getSelectedNumbers());
            LotteryResultProvider resultProvider = providerFactory.getProvider(bet.getRegion());
            List<String> lotteryResults = resultProvider.getLotteryResults();
            
            // Tìm TẤT CẢ cụm THẮNG (cụm mà cả 4 số đều trượt)
            List<String> winningGroups = new ArrayList<>();
            
            for (String group : selectedGroups) {
                String[] numbers = group.split(",");
                if (numbers.length != 4) {
                    log.warn("Invalid group format for truot-4: {}", group);
                    continue;
                }
                
                // Check từng số trong cụm
                boolean hasAnyWin = false; // Có số nào trúng không?
                
                for (String number : numbers) {
                    String trimmedNumber = number.trim();
                    
                    // Check số này có trúng trong kết quả xổ số không
                    for (String result : lotteryResults) {
                        if (result.length() >= 2) {
                            String lastTwoDigits = result.substring(result.length() - 2);
                            if (trimmedNumber.equals(lastTwoDigits)) {
                                hasAnyWin = true;
                                log.info("Loto truot-4: Số {} trong cụm {} TRÚNG → cụm này THUA", trimmedNumber, group);
                                break;
                            }
                        }
                    }
                    
                    if (hasAnyWin) break; // Đã có số trúng → cụm này thua, không cần check tiếp
                }
                
                // Nếu KHÔNG có số nào trúng (cả 4 đều trượt) → cụm này THẮNG
                if (!hasAnyWin) {
                    winningGroups.add(group);
                    log.info("Loto truot-4 WIN: Cụm {} - CẢ 4 số đều TRƯỢT → THẮNG!", group);
                } else {
                    log.info("Loto truot-4 LOSE: Cụm {} - Có số trúng → THUA", group);
                }
            }
            
            if (winningGroups.isEmpty()) {
                log.info("Loto truot-4 LOSE: Không có cụm nào thắng. Selected groups: {}", selectedGroups);
                return false;
            }
            
            // Lưu danh sách cụm thắng
            bet.setWinningNumbers(convertToJsonString(winningGroups));
            log.info("Loto truot-4 WIN: {} winning groups: {}", winningGroups.size(), winningGroups);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking loto truot-4 result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Kiểm tra kết quả Loto trượt 5
     * THẮNG: CẢ 5 số đều KHÔNG trúng
     */
    public boolean checkTruot5Result(Bet bet) {
        return checkTruotNResult(bet, 5);
    }
    
    /**
     * Kiểm tra kết quả Loto trượt 6
     * THẮNG: CẢ 6 số đều KHÔNG trúng
     */
    public boolean checkTruot6Result(Bet bet) {
        return checkTruotNResult(bet, 6);
    }
    
    /**
     * Kiểm tra kết quả Loto trượt 7
     * THẮNG: CẢ 7 số đều KHÔNG trúng
     */
    public boolean checkTruot7Result(Bet bet) {
        return checkTruotNResult(bet, 7);
    }
    
    /**
     * Kiểm tra kết quả Loto trượt 8
     * THẮNG: CẢ 8 số đều KHÔNG trúng
     */
    public boolean checkTruot8Result(Bet bet) {
        return checkTruotNResult(bet, 8);
    }
    
    /**
     * Kiểm tra kết quả Loto trượt 9
     * THẮNG: CẢ 9 số đều KHÔNG trúng
     */
    public boolean checkTruot9Result(Bet bet) {
        return checkTruotNResult(bet, 9);
    }
    
    /**
     * Kiểm tra kết quả Loto trượt 10
     * THẮNG: CẢ 10 số đều KHÔNG trúng
     */
    public boolean checkTruot10Result(Bet bet) {
        return checkTruotNResult(bet, 10);
    }
    
    /**
     * Generic checker cho loto trượt N
     * THẮNG: CẢ N số đều KHÔNG trúng
     */
    private boolean checkTruotNResult(Bet bet, int expectedCount) {
        try {
            List<String> selectedGroups = parseSelectedNumbers(bet.getSelectedNumbers());
            LotteryResultProvider resultProvider = providerFactory.getProvider(bet.getRegion());
            List<String> lotteryResults = resultProvider.getLotteryResults();
            
            List<String> winningGroups = new ArrayList<>();
            
            for (String group : selectedGroups) {
                String[] numbers = group.split(",");
                if (numbers.length != expectedCount) {
                    log.warn("Invalid group format for truot-{}: expected {} numbers, got {}", 
                            expectedCount, expectedCount, numbers.length);
                    continue;
                }
                
                // Check từng số trong cụm
                boolean hasAnyWin = false;
                
                for (String number : numbers) {
                    String trimmedNumber = number.trim();
                    
                    for (String result : lotteryResults) {
                        if (result.length() >= 2) {
                            String lastTwoDigits = result.substring(result.length() - 2);
                            if (trimmedNumber.equals(lastTwoDigits)) {
                                hasAnyWin = true;
                                log.info("Loto truot-{}: Số {} trong cụm {} TRÚNG → cụm này THUA", 
                                        expectedCount, trimmedNumber, group);
                                break;
                            }
                        }
                    }
                    
                    if (hasAnyWin) break;
                }
                
                // Nếu KHÔNG có số nào trúng → THẮNG
                if (!hasAnyWin) {
                    winningGroups.add(group);
                    log.info("Loto truot-{} WIN: Cụm {} - CẢ {} số đều TRƯỢT → THẮNG!", 
                            expectedCount, group, expectedCount);
                } else {
                    log.info("Loto truot-{} LOSE: Cụm {} - Có số trúng → THUA", expectedCount, group);
                }
            }
            
            if (winningGroups.isEmpty()) {
                log.info("Loto truot-{} LOSE: Không có cụm nào thắng", expectedCount);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningGroups));
            log.info("Loto truot-{} WIN: {} winning groups: {}", expectedCount, winningGroups.size(), winningGroups);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking loto truot-{} result: {}", expectedCount, e.getMessage());
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

