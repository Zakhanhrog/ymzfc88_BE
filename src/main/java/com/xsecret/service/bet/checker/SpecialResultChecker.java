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
    
    private final DatabaseLotteryResultProvider databaseProvider;
    private final ObjectMapper objectMapper;
    
    /**
     * 3s đặc biệt: CHỈ check 3 số cuối của giải đặc biệt (không phải tất cả giải)
     * COPY CHÍNH XÁC từ BetService.check3sDacBietResult()
     */
    public boolean check3sDacBietResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            databaseProvider.setContext(bet);
            LotteryResultProvider resultProvider = databaseProvider;
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
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và throw lại
            log.error("❌ [DEBUG] Error checking 3s đặc biệt result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw e; // Throw lại để BetService handle
        } catch (Exception e) {
            log.error("❌ [DEBUG] Unexpected error checking 3s đặc biệt result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw new RuntimeException("Unexpected error checking 3s đặc biệt result: " + e.getMessage(), e);
        }
    }
    
    /**
     * 4s đặc biệt: CHỈ check 4 số cuối của giải đặc biệt (không phải tất cả giải)
     * COPY CHÍNH XÁC từ BetService.check4sDacBietResult()
     */
    public boolean check4sDacBietResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            databaseProvider.setContext(bet);
            LotteryResultProvider resultProvider = databaseProvider;
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
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và throw lại
            log.error("❌ [DEBUG] Error checking 4s đặc biệt result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw e; // Throw lại để BetService handle
        } catch (Exception e) {
            log.error("❌ [DEBUG] Unexpected error checking 4s đặc biệt result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw new RuntimeException("Unexpected error checking 4s đặc biệt result: " + e.getMessage(), e);
        }
    }
    
    /**
     * Giải nhất: CHỈ check 2 số cuối của giải nhất (không phải tất cả giải)
     * COPY CHÍNH XÁC từ BetService.checkGiaiNhatResult()
     */
    public boolean checkGiaiNhatResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            databaseProvider.setContext(bet);
            LotteryResultProvider resultProvider = databaseProvider;
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
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và throw lại
            log.error("❌ [DEBUG] Error checking giai-nhat result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw e; // Throw lại để BetService handle
        } catch (Exception e) {
            log.error("❌ [DEBUG] Unexpected error checking giai-nhat result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw new RuntimeException("Unexpected error checking giai-nhat result: " + e.getMessage(), e);
        }
    }
    
    /**
     * 3D Giải nhất: CHỈ check 3 số cuối của giải nhất (không phải tất cả giải)
     * COPY CHÍNH XÁC từ BetService.check3sGiaiNhatResult()
     */
    public boolean check3sGiaiNhatResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            databaseProvider.setContext(bet);
            LotteryResultProvider resultProvider = databaseProvider;
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
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và throw lại
            log.error("❌ [DEBUG] Error checking 3s-giai-nhat result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw e; // Throw lại để BetService handle
        } catch (Exception e) {
            log.error("❌ [DEBUG] Unexpected error checking 3s-giai-nhat result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw new RuntimeException("Unexpected error checking 3s-giai-nhat result: " + e.getMessage(), e);
        }
    }
    
    /**
     * Đặc biệt: CHỈ check 2 số cuối của giải đặc biệt (không phải tất cả giải)
     * COPY CHÍNH XÁC từ BetService.checkDacBietResult()
     */
    public boolean checkDacBietResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            databaseProvider.setContext(bet);
            LotteryResultProvider resultProvider = databaseProvider;
            String dacBietNumber = resultProvider.getDacBietNumber();
            
            if (dacBietNumber == null || dacBietNumber.length() < 2) {
                log.error("❌ [DEBUG] Đặc biệt: Không tìm thấy giải đặc biệt hoặc giải đặc biệt không hợp lệ cho bet_id={}. DacBietNumber: {}", bet.getId(), dacBietNumber);
                throw new RuntimeException("Không tìm thấy giải đặc biệt hoặc giải đặc biệt không hợp lệ");
            }
            
            String lastTwoDigits = dacBietNumber.substring(dacBietNumber.length() - 2);
            log.info("🔍 [DEBUG] Đặc biệt bet_id={}: Giải đặc biệt = {}, 2 số cuối = {}, Selected numbers: {}", 
                    bet.getId(), dacBietNumber, lastTwoDigits, selectedNumbers);
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                if (selectedNumber.equals(lastTwoDigits)) {
                    winningNumbers.add(selectedNumber);
                    log.info("✅ [DEBUG] Đặc biệt WIN bet_id={}: Selected {} matches last 2 digits of Dac biet {}", 
                            bet.getId(), selectedNumber, dacBietNumber);
                } else {
                    log.info("❌ [DEBUG] Đặc biệt LOSE bet_id={}: Selected {} does not match last 2 digits of Dac biet {}", 
                            bet.getId(), selectedNumber, dacBietNumber);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("❌ [DEBUG] Đặc biệt LOSE bet_id={}: No matches found. Selected: {}, Dac biet last 2 digits: {}", 
                        bet.getId(), selectedNumbers, lastTwoDigits);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("✅ [DEBUG] Đặc biệt WIN bet_id={}: {} winning numbers: {}", 
                    bet.getId(), winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và return false
            log.error("❌ [DEBUG] Error checking dac-biet result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw e; // Throw lại để BetService handle
        } catch (Exception e) {
            log.error("❌ [DEBUG] Unexpected error checking dac-biet result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw new RuntimeException("Unexpected error checking dac-biet result: " + e.getMessage(), e);
        }
    }
    
    /**
     * Đầu Đặc biệt: CHỈ check 2 số ĐẦU của giải đặc biệt (không phải 2 số cuối)
     * COPY CHÍNH XÁC từ BetService.checkDauDacBietResult()
     */
    public boolean checkDauDacBietResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            databaseProvider.setContext(bet);
            LotteryResultProvider resultProvider = databaseProvider;
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
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và throw lại
            log.error("❌ [DEBUG] Error checking dau-dac-biet result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw e; // Throw lại để BetService handle
        } catch (Exception e) {
            log.error("❌ [DEBUG] Unexpected error checking dau-dac-biet result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw new RuntimeException("Unexpected error checking dau-dac-biet result: " + e.getMessage(), e);
        }
    }
    
    /**
     * Đầu/đuôi: CHỈ check 2 số cuối của giải đặc biệt và TẤT CẢ 4 số giải 7 (chỉ Miền Bắc)
     * Chọn số như loto2s (00-99) nhưng so với giải đặc biệt + 4 số giải 7
     * Giải đặc biệt: 1 số (ví dụ: "12345" → 2 số cuối: "45")
     * Giải 7: 4 số 2 chữ số (ví dụ: ["12", "34", "56", "78"])
     * Điểm đặt cược × 5 (vì có 5 số), nhưng tiền thắng không × 5
     */
    public boolean checkDauDuoiResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy 2 số cuối của giải đặc biệt
            databaseProvider.setContext(bet);
            LotteryResultProvider resultProvider = databaseProvider;
            String dacBietNumber = resultProvider.getDacBietNumber();
            String dacBietLastTwo = null;
            if (dacBietNumber != null && dacBietNumber.length() >= 2) {
                dacBietLastTwo = dacBietNumber.substring(dacBietNumber.length() - 2);
            }
            
            // Lấy TẤT CẢ 4 số giải 7
            List<String> giai7Numbers = resultProvider.getGiai7Numbers();
            
            if (dacBietLastTwo == null || giai7Numbers == null || giai7Numbers.isEmpty()) {
                log.error("Không tìm thấy giải đặc biệt hoặc giải 7");
                return false;
            }
            
            // Tạo danh sách tất cả số cần so sánh: 2 số cuối giải đặc biệt + 4 số giải 7
            List<String> allTargetNumbers = new ArrayList<>();
            allTargetNumbers.add(dacBietLastTwo);
            allTargetNumbers.addAll(giai7Numbers);
            
            log.info("Dau-duoi: Dac biet last 2 = {}, Giai 7 = {}, All targets = {}", 
                    dacBietLastTwo, giai7Numbers, allTargetNumbers);
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                // Check xem số này có trùng với BẤT KỲ 1 trong 5 số không
                if (allTargetNumbers.contains(selectedNumber)) {
                    winningNumbers.add(selectedNumber);
                    log.info("Dau-duoi WIN: Selected {} matches one of target numbers {}", 
                            selectedNumber, allTargetNumbers);
                } else {
                    log.info("Dau-duoi LOSE: Selected {} does not match any of target numbers {}", 
                            selectedNumber, allTargetNumbers);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("Dau-duoi LOSE: No matches found. Selected: {}, All targets: {}", 
                        selectedNumbers, allTargetNumbers);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("Dau-duoi WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và throw lại
            log.error("❌ [DEBUG] Error checking dau-duoi result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw e; // Throw lại để BetService handle
        } catch (Exception e) {
            log.error("❌ [DEBUG] Unexpected error checking dau-duoi result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw new RuntimeException("Unexpected error checking dau-duoi result: " + e.getMessage(), e);
        }
    }
    
    /**
     * Đầu/đuôi Miền Trung Nam: CHỈ check 2 số cuối của giải đặc biệt và giải 8 (chỉ Miền Trung Nam)
     * Chọn số như loto2s (00-99) nhưng so với giải đặc biệt + giải 8
     * Giải đặc biệt: 1 số (ví dụ: "12345" → 2 số cuối: "45")
     * Giải 8: 1 số 2 chữ số (ví dụ: "34")
     * Điểm đặt cược × 2 (vì có 2 số), nhưng tiền thắng không × 2
     */
    public boolean checkDauDuoiMienTrungNamResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy 2 số cuối của giải đặc biệt
            databaseProvider.setContext(bet);
            LotteryResultProvider resultProvider = databaseProvider;
            String dacBietNumber = resultProvider.getDacBietNumber();
            String dacBietLastTwo = null;
            if (dacBietNumber != null && dacBietNumber.length() >= 2) {
                dacBietLastTwo = dacBietNumber.substring(dacBietNumber.length() - 2);
            }
            
            // Lấy giải 8
            List<String> giai8Numbers = resultProvider.getGiai8Numbers();
            
            if (dacBietLastTwo == null || giai8Numbers == null || giai8Numbers.isEmpty()) {
                log.error("Không tìm thấy giải đặc biệt hoặc giải 8");
                return false;
            }
            
            // Tạo danh sách tất cả số cần so sánh: 2 số cuối giải đặc biệt + giải 8
            List<String> allTargetNumbers = new ArrayList<>();
            allTargetNumbers.add(dacBietLastTwo);
            allTargetNumbers.addAll(giai8Numbers);
            
            log.info("dau-duoi-mien-trung-nam: Dac biet last 2 = {}, Giai 8 = {}, All targets = {}", 
                    dacBietLastTwo, giai8Numbers, allTargetNumbers);
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                // Check xem số này có trùng với BẤT KỲ 1 trong 2 số không
                if (allTargetNumbers.contains(selectedNumber)) {
                    winningNumbers.add(selectedNumber);
                    log.info("dau-duoi-mien-trung-nam WIN: Selected {} matches one of target numbers {}", 
                            selectedNumber, allTargetNumbers);
                } else {
                    log.info("dau-duoi-mien-trung-nam LOSE: Selected {} does not match any of target numbers {}", 
                            selectedNumber, allTargetNumbers);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("dau-duoi-mien-trung-nam LOSE: No matches found. Selected: {}, All targets: {}", 
                        selectedNumbers, allTargetNumbers);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("dau-duoi-mien-trung-nam WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và throw lại
            log.error("❌ [DEBUG] Error checking dau-duoi-mien-trung-nam result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw e; // Throw lại để BetService handle
        } catch (Exception e) {
            log.error("❌ [DEBUG] Unexpected error checking dau-duoi-mien-trung-nam result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw new RuntimeException("Unexpected error checking dau-duoi-mien-trung-nam result: " + e.getMessage(), e);
        }
    }
    
    /**
     * 3s đầu đuôi: CHỈ check 3 số cuối của giải đặc biệt và TẤT CẢ 3 số giải 6 (chỉ Miền Bắc)
     * Chọn số như loto3s (000-999) nhưng so với giải đặc biệt + 3 số giải 6
     * Giải đặc biệt: 1 số (ví dụ: "12345" → 3 số cuối: "345")
     * Giải 6: 3 số 3 chữ số (ví dụ: ["034", "005", "095"])
     * Điểm đặt cược × 4 (vì có 4 số), nhưng tiền thắng không × 4
     */
    public boolean check3sDauDuoiResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy 3 số cuối của giải đặc biệt
            databaseProvider.setContext(bet);
            LotteryResultProvider resultProvider = databaseProvider;
            String dacBietNumber = resultProvider.getDacBietNumber();
            String dacBietLastThree = null;
            if (dacBietNumber != null && dacBietNumber.length() >= 3) {
                dacBietLastThree = dacBietNumber.substring(dacBietNumber.length() - 3);
            }
            
            // Lấy TẤT CẢ 3 số giải 6
            List<String> giai6Numbers = resultProvider.getGiai6Numbers();
            
            if (dacBietLastThree == null || giai6Numbers == null || giai6Numbers.isEmpty()) {
                log.error("Không tìm thấy giải đặc biệt hoặc giải 6");
                return false;
            }
            
            // Tạo danh sách tất cả số cần so sánh: 3 số cuối giải đặc biệt + 3 số giải 6
            List<String> allTargetNumbers = new ArrayList<>();
            allTargetNumbers.add(dacBietLastThree);
            allTargetNumbers.addAll(giai6Numbers);
            
            log.info("3s-dau-duoi: Dac biet last 3 = {}, Giai 6 = {}, All targets = {}", 
                    dacBietLastThree, giai6Numbers, allTargetNumbers);
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                // Check xem số này có trùng với BẤT KỲ 1 trong 4 số không
                if (allTargetNumbers.contains(selectedNumber)) {
                    winningNumbers.add(selectedNumber);
                    log.info("3s-dau-duoi WIN: Selected {} matches one of target numbers {}", 
                            selectedNumber, allTargetNumbers);
                } else {
                    log.info("3s-dau-duoi LOSE: Selected {} does not match any of target numbers {}", 
                            selectedNumber, allTargetNumbers);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("3s-dau-duoi LOSE: No matches found. Selected: {}, All targets: {}", 
                        selectedNumbers, allTargetNumbers);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("3s-dau-duoi WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và throw lại
            log.error("❌ [DEBUG] Error checking 3s-dau-duoi result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw e; // Throw lại để BetService handle
        } catch (Exception e) {
            log.error("❌ [DEBUG] Unexpected error checking 3s-dau-duoi result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw new RuntimeException("Unexpected error checking 3s-dau-duoi result: " + e.getMessage(), e);
        }
    }
    
    /**
     * 3s giải 6: CHỈ check 3 số cuối của TẤT CẢ 3 số giải 6 (chỉ Miền Bắc)
     * Chọn số như loto3s (000-999) nhưng chỉ so với giải 6
     * Giải 6 Miền Bắc có 3 số 3 chữ số (ví dụ: "034", "005", "095")
     * Điểm đặt cược × 3 (vì có 3 số), nhưng tiền thắng không × 3
     */
    public boolean check3sGiai6Result(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            databaseProvider.setContext(bet);
            LotteryResultProvider resultProvider = databaseProvider;
            List<String> giai6Numbers = resultProvider.getGiai6Numbers();
            
            if (giai6Numbers == null || giai6Numbers.isEmpty()) {
                log.error("Không tìm thấy giải 6 hoặc giải 6 không hợp lệ");
                return false;
            }
            
            // Giải 6 Miền Bắc có 3 số, lấy 3 số cuối của mỗi số
            List<String> giai6LastThreeDigits = new ArrayList<>();
            for (String giai6Num : giai6Numbers) {
                if (giai6Num != null && giai6Num.length() >= 3) {
                    String lastThree = giai6Num.substring(giai6Num.length() - 3);
                    giai6LastThreeDigits.add(lastThree);
                }
            }
            
            log.info("3s-giai-6: Giải 6 numbers = {}, last 3 digits = {}", giai6Numbers, giai6LastThreeDigits);
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                // Check xem số này có trùng với BẤT KỲ 1 trong 3 số giải 6 không
                if (giai6LastThreeDigits.contains(selectedNumber)) {
                    winningNumbers.add(selectedNumber);
                    log.info("3s-giai-6 WIN: Selected {} matches one of Giai 6 last 3 digits {}", 
                            selectedNumber, giai6LastThreeDigits);
                } else {
                    log.info("3s-giai-6 LOSE: Selected {} does not match any of Giai 6 last 3 digits {}", 
                            selectedNumber, giai6LastThreeDigits);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("3s-giai-6 LOSE: No matches found. Selected: {}, Giai 6 last 3 digits: {}", 
                        selectedNumbers, giai6LastThreeDigits);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("3s-giai-6 WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và throw lại
            log.error("❌ [DEBUG] Error checking 3s-giai-6 result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw e; // Throw lại để BetService handle
        } catch (Exception e) {
            log.error("❌ [DEBUG] Unexpected error checking 3s-giai-6 result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw new RuntimeException("Unexpected error checking 3s-giai-6 result: " + e.getMessage(), e);
        }
    }
    
    /**
     * Đề giải 8: CHỈ check 2 số cuối của giải 8 (chỉ Miền Trung Nam)
     * Chọn số như loto2s (00-99) nhưng chỉ so với giải 8
     * Giải 8 chỉ có 1 số 2 chữ số (ví dụ: "13")
     */
    public boolean checkDeGiai8Result(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            databaseProvider.setContext(bet);
            LotteryResultProvider resultProvider = databaseProvider;
            String giai8Number = resultProvider.getGiai8Number();
            
            if (giai8Number == null || giai8Number.length() < 2) {
                log.error("Không tìm thấy giải 8 hoặc giải 8 không hợp lệ");
                return false;
            }
            
            // Lấy 2 số cuối của giải 8 (giải 8 thường chỉ có 2 số)
            String lastTwoDigits = giai8Number.length() >= 2 
                ? giai8Number.substring(giai8Number.length() - 2) 
                : giai8Number;
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                if (selectedNumber.equals(lastTwoDigits)) {
                    winningNumbers.add(selectedNumber);
                    log.info("De-giai-8 WIN: Selected {} matches Giai 8 {}", selectedNumber, giai8Number);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("De-giai-8 LOSE: No matches found. Selected: {}, Giai 8: {}", 
                        selectedNumbers, lastTwoDigits);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("De-giai-8 WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và throw lại
            log.error("❌ [DEBUG] Error checking de-giai-8 result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw e; // Throw lại để BetService handle
        } catch (Exception e) {
            log.error("❌ [DEBUG] Unexpected error checking de-giai-8 result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw new RuntimeException("Unexpected error checking de-giai-8 result: " + e.getMessage(), e);
        }
    }
    
    /**
     * Đề giải 7: CHỈ check 2 số cuối của TẤT CẢ 4 số giải 7 (chỉ Miền Bắc)
     * Chọn số như loto2s (00-99) nhưng chỉ so với giải 7
     * Giải 7 Miền Bắc có 4 số (ví dụ: "12", "34", "56", "78")
     * Điểm đặt cược × 4 (vì có 4 số), nhưng tiền thắng không × 4
     */
    public boolean checkDeGiai7Result(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            databaseProvider.setContext(bet);
            LotteryResultProvider resultProvider = databaseProvider;
            List<String> giai7Numbers = resultProvider.getGiai7Numbers();
            
            if (giai7Numbers == null || giai7Numbers.isEmpty()) {
                log.error("Không tìm thấy giải 7 hoặc giải 7 không hợp lệ");
                return false;
            }
            
            // Giải 7 Miền Bắc có 4 số, lấy 2 số cuối của mỗi số
            List<String> giai7LastTwoDigits = new ArrayList<>();
            for (String giai7Num : giai7Numbers) {
                if (giai7Num != null && giai7Num.length() >= 2) {
                    String lastTwo = giai7Num.substring(giai7Num.length() - 2);
                    giai7LastTwoDigits.add(lastTwo);
                }
            }
            
            log.info("De-giai-7: Giải 7 numbers = {}, last 2 digits = {}", giai7Numbers, giai7LastTwoDigits);
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                // Check xem số này có trùng với BẤT KỲ 1 trong 4 số giải 7 không
                if (giai7LastTwoDigits.contains(selectedNumber)) {
                    winningNumbers.add(selectedNumber);
                    log.info("De-giai-7 WIN: Selected {} matches one of Giai 7 last 2 digits {}", 
                            selectedNumber, giai7LastTwoDigits);
                } else {
                    log.info("De-giai-7 LOSE: Selected {} does not match any of Giai 7 last 2 digits {}", 
                            selectedNumber, giai7LastTwoDigits);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("De-giai-7 LOSE: No matches found. Selected: {}, Giai 7 last 2 digits: {}", 
                        selectedNumbers, giai7LastTwoDigits);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("De-giai-7 WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và throw lại
            log.error("❌ [DEBUG] Error checking de-giai-7 result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw e; // Throw lại để BetService handle
        } catch (Exception e) {
            log.error("❌ [DEBUG] Unexpected error checking de-giai-7 result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw new RuntimeException("Unexpected error checking de-giai-7 result: " + e.getMessage(), e);
        }
    }
    
    /**
     * 3s đầu đuôi Miền Trung Nam: CHỈ check 3 số cuối giải đặc biệt + 1 số giải 7 (chỉ Miền Trung Nam)
     * Chọn số như loto3s (000-999) nhưng so với giải đặc biệt + giải 7
     * Giải đặc biệt: 1 số (ví dụ: "01640" → 3 số cuối: "640")
     * Giải 7: 1 số 3 chữ số (ví dụ: "138")
     * Điểm đặt cược × 2 (vì có 2 số), nhưng tiền thắng không × 2
     */
    public boolean check3sDauDuoiMienTrungNamResult(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy 3 số cuối của giải đặc biệt
            databaseProvider.setContext(bet);
            LotteryResultProvider resultProvider = databaseProvider;
            String dacBietNumber = resultProvider.getDacBietNumber();
            String dacBietLastThree = null;
            if (dacBietNumber != null && dacBietNumber.length() >= 3) {
                dacBietLastThree = dacBietNumber.substring(dacBietNumber.length() - 3);
            }
            
            // Lấy 1 số giải 7 (Miền Trung Nam)
            String giai7Number = resultProvider.getGiai7Number();
            
            if (dacBietLastThree == null || giai7Number == null) {
                log.error("Không tìm thấy giải đặc biệt hoặc giải 7");
                return false;
            }
            
            // Tạo danh sách tất cả số cần so sánh: 3 số cuối giải đặc biệt + 1 số giải 7
            List<String> allTargetNumbers = new ArrayList<>();
            allTargetNumbers.add(dacBietLastThree);
            allTargetNumbers.add(giai7Number);
            
            log.info("3s-dau-duoi-mien-trung-nam: Dac biet last 3 = {}, Giai 7 = {}, All targets = {}", 
                    dacBietLastThree, giai7Number, allTargetNumbers);
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                // Check xem số này có trùng với BẤT KỲ 1 trong 2 số không
                if (allTargetNumbers.contains(selectedNumber)) {
                    winningNumbers.add(selectedNumber);
                    log.info("3s-dau-duoi-mien-trung-nam WIN: Selected {} matches one of target numbers {}", 
                            selectedNumber, allTargetNumbers);
                } else {
                    log.info("3s-dau-duoi-mien-trung-nam LOSE: Selected {} does not match any of target numbers {}", 
                            selectedNumber, allTargetNumbers);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("3s-dau-duoi-mien-trung-nam LOSE: No matches found. Selected: {}, All targets: {}", 
                        selectedNumbers, allTargetNumbers);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("3s-dau-duoi-mien-trung-nam WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và throw lại
            log.error("❌ [DEBUG] Error checking 3s-dau-duoi-mien-trung-nam result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw e; // Throw lại để BetService handle
        } catch (Exception e) {
            log.error("❌ [DEBUG] Unexpected error checking 3s-dau-duoi-mien-trung-nam result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw new RuntimeException("Unexpected error checking 3s-dau-duoi-mien-trung-nam result: " + e.getMessage(), e);
        }
    }
    
    /**
     * 3s giải 7: CHỈ check 3 số cuối của giải 7 (chỉ Miền Trung Nam)
     * Chọn số từ 000-999 nhưng chỉ so với giải 7
     * Giải 7 chỉ có 1 số 3 chữ số (ví dụ: "138")
     */
    public boolean check3sGiai7Result(Bet bet) {
        try {
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            databaseProvider.setContext(bet);
            LotteryResultProvider resultProvider = databaseProvider;
            String giai7Number = resultProvider.getGiai7Number();
            
            if (giai7Number == null || giai7Number.length() < 3) {
                log.error("Không tìm thấy giải 7 hoặc giải 7 không hợp lệ (cần ít nhất 3 chữ số)");
                return false;
            }
            
            // Lấy 3 số cuối của giải 7 (giải 7 thường chỉ có 3 số)
            String lastThreeDigits = giai7Number.length() >= 3 
                ? giai7Number.substring(giai7Number.length() - 3) 
                : giai7Number;
            
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                if (selectedNumber.equals(lastThreeDigits)) {
                    winningNumbers.add(selectedNumber);
                    log.info("3s-giai-7 WIN: Selected {} matches Giai 7 {}", selectedNumber, giai7Number);
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("3s-giai-7 LOSE: No matches found. Selected: {}, Giai 7: {}", 
                        selectedNumbers, lastThreeDigits);
                return false;
            }
            
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("3s-giai-7 WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (RuntimeException e) {
            // Nếu lỗi do chưa có kết quả xổ số thì propagate lên BetService để skip
            if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                throw e; // Propagate exception để BetService có thể skip bet
            }
            // Các lỗi khác thì log và throw lại
            log.error("❌ [DEBUG] Error checking 3s-giai-7 result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw e; // Throw lại để BetService handle
        } catch (Exception e) {
            log.error("❌ [DEBUG] Unexpected error checking 3s-giai-7 result for bet_id={}: {}", bet.getId(), e.getMessage());
            throw new RuntimeException("Unexpected error checking 3s-giai-7 result: " + e.getMessage(), e);
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

