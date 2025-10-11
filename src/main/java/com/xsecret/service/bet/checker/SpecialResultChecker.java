package com.xsecret.service.bet.checker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsecret.entity.Bet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Checker cho các loại đặc biệt
 * - 3s đặc biệt, 4s đặc biệt
 * - Đặc biệt (2s cuối), Đầu đặc biệt (2s đầu)
 * - Giải nhất, 3s giải nhất
 * GIỮ NGUYÊN 100% logic từ BetService
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpecialResultChecker {
    
    private final LotteryResultProvider resultProvider;
    private final ObjectMapper objectMapper;
    
    /**
     * 3s đặc biệt: CHỈ check 3 số cuối của giải đặc biệt (không phải tất cả giải)
     * COPY CHÍNH XÁC từ BetService.check3sDacBietResult()
     */
    public boolean check3sDacBietResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            String dacBietNumber = resultProvider.getDacBietNumber();
            
            if (dacBietNumber == null || dacBietNumber.length() < 3) {
                log.warn("3s đặc biệt: Không tìm thấy giải đặc biệt hoặc giải đặc biệt quá ngắn: {}", dacBietNumber);
                return false;
            }
            
            String lastThreeDigits = dacBietNumber.substring(dacBietNumber.length() - 3);
            log.info("3s đặc biệt: Giải đặc biệt = {}, 3 số cuối = {}", dacBietNumber, lastThreeDigits);
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                if (selectedNumber.equals(lastThreeDigits)) {
                    winningNumbers.add(selectedNumber);
                    log.info("3s đặc biệt WIN: {} trùng với 3 số cuối giải đặc biệt {}", selectedNumber, lastThreeDigits);
                } else {
                    log.info("3s đặc biệt LOSE: {} không trùng với 3 số cuối giải đặc biệt {}", selectedNumber, lastThreeDigits);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("3s đặc biệt LOSE: Không có số nào trúng. Selected numbers: {}, 3 số cuối giải đặc biệt: {}", selectedNumbers, lastThreeDigits);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("3s đặc biệt WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking 3s đặc biệt result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 4s đặc biệt: CHỈ check 4 số cuối của giải đặc biệt (không phải tất cả giải)
     * COPY CHÍNH XÁC từ BetService.check4sDacBietResult()
     */
    public boolean check4sDacBietResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            String dacBietNumber = resultProvider.getDacBietNumber();
            
            if (dacBietNumber == null || dacBietNumber.length() < 4) {
                log.warn("4s đặc biệt: Không tìm thấy giải đặc biệt hoặc giải đặc biệt quá ngắn: {}", dacBietNumber);
                return false;
            }
            
            String lastFourDigits = dacBietNumber.substring(dacBietNumber.length() - 4);
            log.info("4s đặc biệt: Giải đặc biệt = {}, 4 số cuối = {}", dacBietNumber, lastFourDigits);
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                if (selectedNumber.equals(lastFourDigits)) {
                    winningNumbers.add(selectedNumber);
                    log.info("4s đặc biệt WIN: {} trùng với 4 số cuối giải đặc biệt {}", selectedNumber, lastFourDigits);
                } else {
                    log.info("4s đặc biệt LOSE: {} không trùng với 4 số cuối giải đặc biệt {}", selectedNumber, lastFourDigits);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("4s đặc biệt LOSE: Không có số nào trúng. Selected numbers: {}, 4 số cuối giải đặc biệt: {}", selectedNumbers, lastFourDigits);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("4s đặc biệt WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking 4s đặc biệt result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Giải nhất: CHỈ check 2 số cuối của giải nhất (không phải tất cả giải)
     * COPY CHÍNH XÁC từ BetService.checkGiaiNhatResult()
     */
    public boolean checkGiaiNhatResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            String giaiNhatNumber = resultProvider.getGiaiNhatNumber();
            
            if (giaiNhatNumber == null || giaiNhatNumber.length() < 2) {
                log.error("Không tìm thấy giải nhất hoặc giải nhất không hợp lệ");
                return false;
            }
            
            String lastTwoDigits = giaiNhatNumber.substring(giaiNhatNumber.length() - 2);
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                if (selectedNumber.equals(lastTwoDigits)) {
                    winningNumbers.add(selectedNumber);
                    log.info("Giai-nhat WIN: Selected {} matches last 2 digits of Giai nhat {}", selectedNumber, giaiNhatNumber);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("Giai-nhat LOSE: No matches found. Selected: {}, Giai nhat last 2 digits: {}", 
                        selectedNumbers, lastTwoDigits);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("Giai-nhat WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking giai-nhat result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 3D Giải nhất: CHỈ check 3 số cuối của giải nhất (không phải tất cả giải)
     * COPY CHÍNH XÁC từ BetService.check3sGiaiNhatResult()
     */
    public boolean check3sGiaiNhatResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            String giaiNhatNumber = resultProvider.getGiaiNhatNumber();
            
            if (giaiNhatNumber == null || giaiNhatNumber.length() < 3) {
                log.error("Không tìm thấy giải nhất hoặc giải nhất không hợp lệ (cần ít nhất 3 chữ số)");
                return false;
            }
            
            String lastThreeDigits = giaiNhatNumber.substring(giaiNhatNumber.length() - 3);
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                if (selectedNumber.equals(lastThreeDigits)) {
                    winningNumbers.add(selectedNumber);
                    log.info("3s-giai-nhat WIN: Selected {} matches last 3 digits of Giai nhat {}", selectedNumber, giaiNhatNumber);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("3s-giai-nhat LOSE: No matches found. Selected: {}, Giai nhat last 3 digits: {}", 
                        selectedNumbers, lastThreeDigits);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("3s-giai-nhat WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking 3s-giai-nhat result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Đặc biệt: CHỈ check 2 số cuối của giải đặc biệt (không phải tất cả giải)
     * COPY CHÍNH XÁC từ BetService.checkDacBietResult()
     */
    public boolean checkDacBietResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            String dacBietNumber = resultProvider.getDacBietNumber();
            
            if (dacBietNumber == null || dacBietNumber.length() < 2) {
                log.error("Không tìm thấy giải đặc biệt hoặc giải đặc biệt không hợp lệ");
                return false;
            }
            
            String lastTwoDigits = dacBietNumber.substring(dacBietNumber.length() - 2);
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                if (selectedNumber.equals(lastTwoDigits)) {
                    winningNumbers.add(selectedNumber);
                    log.info("Dac-biet WIN: Selected {} matches last 2 digits of Dac biet {}", selectedNumber, dacBietNumber);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("Dac-biet LOSE: No matches found. Selected: {}, Dac biet last 2 digits: {}", 
                        selectedNumbers, lastTwoDigits);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("Dac-biet WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking dac-biet result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Đầu Đặc biệt: CHỈ check 2 số ĐẦU của giải đặc biệt (không phải 2 số cuối)
     * COPY CHÍNH XÁC từ BetService.checkDauDacBietResult()
     */
    public boolean checkDauDacBietResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            String dacBietNumber = resultProvider.getDacBietNumber();
            
            if (dacBietNumber == null || dacBietNumber.length() < 2) {
                log.error("Không tìm thấy giải đặc biệt hoặc giải đặc biệt không hợp lệ");
                return false;
            }
            
            // Lấy 2 số ĐẦU của giải đặc biệt (khác với dac-biet là 2 số cuối)
            String firstTwoDigits = dacBietNumber.substring(0, 2);
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                if (selectedNumber.equals(firstTwoDigits)) {
                    winningNumbers.add(selectedNumber);
                    log.info("Dau-dac-biet WIN: Selected {} matches first 2 digits of Dac biet {}", selectedNumber, dacBietNumber);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("Dau-dac-biet LOSE: No matches found. Selected: {}, Dac biet first 2 digits: {}", 
                        selectedNumbers, firstTwoDigits);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("Dau-dac-biet WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking dau-dac-biet result: {}", e.getMessage());
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

