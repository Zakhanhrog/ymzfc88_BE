package com.xsecret.service.bet.checker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsecret.entity.Bet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Checker cho các loại Xiên (xiên 2, xiên 3, xiên 4)
 * GIỮ NGUYÊN 100% logic từ BetService
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class XienResultChecker {
    
    private final DatabaseLotteryResultProvider databaseProvider;
    private final ObjectMapper objectMapper;
    
    /**
     * Kiểm tra kết quả Loto xiên 2: check cặp số, cả 2 số trong cặp phải trúng
     * Format selectedNumbers: ["12,23", "34,56"] (mỗi cặp là 1 string)
     * COPY CHÍNH XÁC từ BetService.checkLotoXien2Result()
     */
    public boolean checkXien2Result(Bet bet) {
        try {
            // Parse selected pairs từ JSON
            List<String> selectedPairs = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy kết quả xổ số từ database
            databaseProvider.setContext(bet);
            List<String> lotteryResults = databaseProvider.getLotteryResults();
            
            // Tìm TẤT CẢ cặp trúng
            List<String> winningPairs = new ArrayList<>();
            
            for (String pair : selectedPairs) {
                // Parse cặp số: "12,23" -> ["12", "23"]
                String[] numbers = pair.split(",");
                if (numbers.length != 2) {
                    log.warn("Invalid pair format: {}", pair);
                    continue;
                }
                
                String firstNumber = numbers[0].trim();
                String secondNumber = numbers[1].trim();
                
                // Check cả 2 số trong cặp có trúng không
                boolean firstWins = false;
                boolean secondWins = false;
                
                for (String result : lotteryResults) {
                    if (result.length() >= 2) {
                        String lastTwoDigits = result.substring(result.length() - 2);
                        if (firstNumber.equals(lastTwoDigits)) {
                            firstWins = true;
                        }
                        if (secondNumber.equals(lastTwoDigits)) {
                            secondWins = true;
                        }
                    }
                }
                
                // Cả 2 số trong cặp phải trúng mới thắng cặp
                if (firstWins && secondWins) {
                    winningPairs.add(pair);
                    log.info("Loto xiên 2 WIN pair: {} (both {} and {} won)", pair, firstNumber, secondNumber);
                } else {
                    log.info("Loto xiên 2 LOSE pair: {} (first: {}, second: {})", pair, firstWins, secondWins);
                }
            }
            
            if (winningPairs.isEmpty()) {
                log.info("Loto xiên 2 LOSE: No winning pairs found. Selected pairs: {}", selectedPairs);
                return false;
            }
            
            // Lưu danh sách cặp trúng vào bet
            bet.setWinningNumbers(convertToJsonString(winningPairs));
            log.info("Loto xiên 2 WIN: {} winning pairs: {}", winningPairs.size(), winningPairs);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking loto xiên 2 result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Kiểm tra kết quả Loto xiên 3: check cụm 3 số, cả 3 số trong cụm phải trúng
     * Format selectedNumbers: ["12,23,34", "45,56,67"] (mỗi cụm là 1 string)
     * COPY CHÍNH XÁC từ BetService.checkLotoXien3Result()
     */
    public boolean checkXien3Result(Bet bet) {
        try {
            // Parse selected groups từ JSON
            List<String> selectedGroups = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy kết quả xổ số từ database
            databaseProvider.setContext(bet);
            List<String> lotteryResults = databaseProvider.getLotteryResults();
            
            // Tìm TẤT CẢ cụm trúng
            List<String> winningGroups = new ArrayList<>();
            
            for (String group : selectedGroups) {
                // Parse cụm số: "12,23,34" -> ["12", "23", "34"]
                String[] numbers = group.split(",");
                if (numbers.length != 3) {
                    log.warn("Invalid group format: {}", group);
                    continue;
                }
                
                String firstNumber = numbers[0].trim();
                String secondNumber = numbers[1].trim();
                String thirdNumber = numbers[2].trim();
                
                // Check cả 3 số trong cụm có trúng không
                boolean firstWins = false;
                boolean secondWins = false;
                boolean thirdWins = false;
                
                for (String result : lotteryResults) {
                    if (result.length() >= 2) {
                        String lastTwoDigits = result.substring(result.length() - 2);
                        if (firstNumber.equals(lastTwoDigits)) {
                            firstWins = true;
                        }
                        if (secondNumber.equals(lastTwoDigits)) {
                            secondWins = true;
                        }
                        if (thirdNumber.equals(lastTwoDigits)) {
                            thirdWins = true;
                        }
                    }
                }
                
                // Cả 3 số trong cụm phải trúng mới thắng cụm
                if (firstWins && secondWins && thirdWins) {
                    winningGroups.add(group);
                    log.info("Loto xiên 3 WIN group: {} (all three {} {} {} won)", group, firstNumber, secondNumber, thirdNumber);
                } else {
                    log.info("Loto xiên 3 LOSE group: {} (first: {}, second: {}, third: {})", group, firstWins, secondWins, thirdWins);
                }
            }
            
            if (winningGroups.isEmpty()) {
                log.info("Loto xiên 3 LOSE: No winning groups found. Selected groups: {}", selectedGroups);
                return false;
            }
            
            // Lưu danh sách cụm trúng vào bet
            bet.setWinningNumbers(convertToJsonString(winningGroups));
            log.info("Loto xiên 3 WIN: {} winning groups: {}", winningGroups.size(), winningGroups);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking loto xiên 3 result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Kiểm tra kết quả Loto xiên 4: check cụm 4 số, cả 4 số trong cụm phải trúng
     * Format selectedNumbers: ["12,23,34,45", "56,67,78,89"] (mỗi cụm là 1 string)
     * COPY CHÍNH XÁC từ BetService.checkLotoXien4Result()
     */
    public boolean checkXien4Result(Bet bet) {
        try {
            // Parse selected groups từ JSON
            List<String> selectedGroups = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy kết quả xổ số từ database
            databaseProvider.setContext(bet);
            List<String> lotteryResults = databaseProvider.getLotteryResults();
            
            // Tìm TẤT CẢ cụm trúng
            List<String> winningGroups = new ArrayList<>();
            
            for (String group : selectedGroups) {
                // Parse cụm số: "12,23,34,45" -> ["12", "23", "34", "45"]
                String[] numbers = group.split(",");
                if (numbers.length != 4) {
                    log.warn("Invalid group format: {}", group);
                    continue;
                }
                
                String firstNumber = numbers[0].trim();
                String secondNumber = numbers[1].trim();
                String thirdNumber = numbers[2].trim();
                String fourthNumber = numbers[3].trim();
                
                // Check cả 4 số trong cụm có trúng không
                boolean firstWins = false;
                boolean secondWins = false;
                boolean thirdWins = false;
                boolean fourthWins = false;
                
                for (String result : lotteryResults) {
                    if (result.length() >= 2) {
                        String lastTwoDigits = result.substring(result.length() - 2);
                        if (firstNumber.equals(lastTwoDigits)) {
                            firstWins = true;
                        }
                        if (secondNumber.equals(lastTwoDigits)) {
                            secondWins = true;
                        }
                        if (thirdNumber.equals(lastTwoDigits)) {
                            thirdWins = true;
                        }
                        if (fourthNumber.equals(lastTwoDigits)) {
                            fourthWins = true;
                        }
                    }
                }
                
                // Cả 4 số trong cụm phải trúng mới thắng cụm
                if (firstWins && secondWins && thirdWins && fourthWins) {
                    winningGroups.add(group);
                    log.info("Loto xiên 4 WIN group: {} (all four {} {} {} {} won)", group, firstNumber, secondNumber, thirdNumber, fourthNumber);
                } else {
                    log.info("Loto xiên 4 LOSE group: {} (first: {}, second: {}, third: {}, fourth: {})", group, firstWins, secondWins, thirdWins, fourthWins);
                }
            }
            
            if (winningGroups.isEmpty()) {
                log.info("Loto xiên 4 LOSE: No winning groups found. Selected groups: {}", selectedGroups);
                return false;
            }
            
            // Lưu danh sách cụm trúng vào bet
            bet.setWinningNumbers(convertToJsonString(winningGroups));
            log.info("Loto xiên 4 WIN: {} winning groups: {}", winningGroups.size(), winningGroups);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking loto xiên 4 result: {}", e.getMessage());
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

