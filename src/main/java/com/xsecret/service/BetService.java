package com.xsecret.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsecret.dto.request.BetRequest;
import com.xsecret.dto.response.BetResponse;
import com.xsecret.dto.response.BetStatisticsResponse;
import com.xsecret.entity.Bet;
import com.xsecret.entity.User;
import com.xsecret.repository.BetRepository;
import com.xsecret.repository.UserRepository;
import com.xsecret.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetService {

    private final BetRepository betRepository;
    private final UserRepository userRepository;
    private final PointService pointService;
    private final ObjectMapper objectMapper;

    /**
     * Đặt cược mới
     */
    @Transactional
    public BetResponse placeBet(BetRequest request, Long userId) {
        log.info("User {} placing bet: region={}, betType={}, numbers={}, amount={}", 
                userId, request.getRegion(), request.getBetType(), request.getSelectedNumbers(), request.getBetAmount());

        // Kiểm tra loại cược được hỗ trợ
        if (!isSupportedBetType(request.getBetType())) {
            throw new RuntimeException("Loại cược '" + request.getBetType() + "' chưa được hỗ trợ. " +
                "Hiện tại hỗ trợ: loto2s/loto-2-so, loto3s/loto-3s, loto4s/loto-4s, giai-nhat (chỉ Miền Bắc), dac-biet (cả 2 miền)");
        }

        // Lấy thông tin user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Tính toán - sử dụng điểm để đặt cược
        BigDecimal betPoints = request.getBetAmount(); // Tổng số điểm cược (chia đều cho các số)
        List<String> selectedNumbers = parseSelectedNumbers(convertToJsonString(request.getSelectedNumbers()));
        int selectedCount = selectedNumbers.size();
        
        // Tiền đặt cược = số điểm × đơn giá × số lượng số
        // Ví dụ: 10 điểm × 27 × 3 số = 810
        BigDecimal totalBetAmount = betPoints.multiply(request.getPricePerPoint()).multiply(BigDecimal.valueOf(selectedCount));
        
        // Tiền thắng nếu tất cả trúng = số điểm × tỷ lệ × số lượng số
        // Ví dụ: 10 điểm × 99 × 3 số = 2,970
        BigDecimal potentialWin = betPoints.multiply(request.getOdds()).multiply(BigDecimal.valueOf(selectedCount));
        BigDecimal potentialProfit = potentialWin.subtract(totalBetAmount); // Chỉ tính tiền lãi

        // Log bet calculation
        log.info("Bet calculation - betPoints: {}, selectedCount: {}, totalBetAmount: {}, userPoints: {}, pricePerPoint: {}, potentialWin: {}", 
                betPoints, selectedCount, totalBetAmount, user.getPoints(), request.getPricePerPoint(), potentialWin);

        // Kiểm tra số điểm có đủ không (trừ tổng tiền đặt cược)
        long pointsToDeductLong = totalBetAmount.longValue();
        if (user.getPoints() < pointsToDeductLong) {
            throw new RuntimeException("Số điểm không đủ để đặt cược. Cần: " + pointsToDeductLong + " điểm, hiện có: " + user.getPoints() + " điểm");
        }

        // Trừ điểm từ tài khoản user
        long pointsBefore = user.getPoints();
        long pointsAfter = pointsBefore - pointsToDeductLong;
        user.setPoints(pointsAfter);
        userRepository.save(user);
        
        // Tạo transaction record để lưu lịch sử (không cần gọi PointService vì đã trừ điểm trực tiếp)
        // pointService.subtractPoints(user, betAmount, 
        //     com.xsecret.entity.PointTransaction.PointTransactionType.BET_PLACED,
        //     "Đặt cược: " + betAmount + " điểm", "BET", null, null);
        
        log.info("Deducted {} points from user {}. Before: {}, After: {}", pointsToDeductLong, userId, pointsBefore, pointsAfter);

        // Tạo bet record
        Bet bet = Bet.builder()
                .user(user)
                .region(request.getRegion())
                .betType(request.getBetType())
                .selectedNumbers(convertToJsonString(request.getSelectedNumbers()))
                .betAmount(betPoints) // Tổng số điểm cược (chia đều cho các số)
                .pricePerPoint(request.getPricePerPoint()) // Đơn giá 1 điểm
                .totalAmount(totalBetAmount) // Tổng tiền cược (điểm)
                .odds(request.getOdds())
                .potentialWin(potentialWin) // Tổng tiền có thể nhận (gốc + lãi)
                .status(Bet.BetStatus.PENDING)
                .resultDate(getCurrentDateString())
                .build();

        Bet savedBet = betRepository.save(bet);
        log.info("Bet placed successfully with ID: {}", savedBet.getId());

        return BetResponse.fromEntity(savedBet);
    }

    /**
     * Lấy danh sách bet của user
     */
    @Transactional(readOnly = true)
    public Page<BetResponse> getUserBets(Long userId, Pageable pageable) {
        return betRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(BetResponse::fromEntity);
    }

    /**
     * Lấy bet theo ID
     */
    @Transactional(readOnly = true)
    public BetResponse getBetById(Long betId, Long userId) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bet với ID: " + betId));

        // Kiểm tra quyền truy cập
        if (!bet.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem bet này");
        }

        return BetResponse.fromEntity(bet);
    }

    /**
     * Lấy bet gần đây của user
     */
    @Transactional(readOnly = true)
    public List<BetResponse> getRecentBets(Long userId, int limit) {
        return betRepository.findRecentBetsByUserId(userId, Pageable.ofSize(limit))
                .stream()
                .map(BetResponse::fromEntity)
                .toList();
    }

    /**
     * Kiểm tra kết quả bet (sau 5 giây - mock)
     */
    @Transactional
    public void checkBetResults() {
        String currentDate = getCurrentDateString();
        List<Bet> pendingBets = betRepository.findPendingBetsToCheck(currentDate);

        log.info("Checking results for {} pending bets", pendingBets.size());

        for (Bet bet : pendingBets) {
            try {
                checkBetResult(bet);
            } catch (Exception e) {
                log.error("Error checking result for bet {}: {}", bet.getId(), e.getMessage());
            }
        }
    }

    /**
     * Kiểm tra kết quả cho 1 bet cụ thể
     * LOGIC CUỐI CÙNG: 
     * 1. Đặt cược: Trừ toàn bộ tiền cược
     * 2. Thắng cược: Cộng CHỈ tiền lãi của số trúng (thua mất luôn, không hoàn vốn)
     */
    @Transactional
    public void checkBetResult(Bet bet) {
        log.info("Checking result for bet {}: {} - {}", bet.getId(), bet.getBetType(), bet.getSelectedNumbers());

        // Mock logic - giả lập kiểm tra kết quả
        boolean isWin = simulateBetResult(bet);
        
        bet.setIsWin(isWin);
        bet.setResultCheckedAt(LocalDateTime.now());

            if (isWin) {
            bet.setStatus(Bet.BetStatus.WON);
            
            // Tính tiền thắng: cộng TOÀN BỘ tiền thắng (bao gồm cả vốn)
            BigDecimal winAmount;
            if ("loto2s".equals(bet.getBetType()) || "loto-2-so".equals(bet.getBetType()) 
                || "loto3s".equals(bet.getBetType()) || "loto-3s".equals(bet.getBetType())
                || "loto4s".equals(bet.getBetType()) || "loto-4s".equals(bet.getBetType())
                || "giai-nhat".equals(bet.getBetType())
                || "dac-biet".equals(bet.getBetType())) {
                // Cho loto2s: tính tiền thắng dựa trên số lượng số trúng
                List<String> winningNumbers = parseSelectedNumbers(bet.getWinningNumbers());
                int winningCount = winningNumbers.size();
                
                // Logic mới: số điểm × tỷ lệ × số trúng + bonus cho trúng nhiều lần
                // Ví dụ: 10 điểm × 99 × 2 số trúng = 1,980
                BigDecimal totalBetPoints = bet.getBetAmount(); // Số điểm cược (10)
                List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
                
                // Đếm số lần mỗi số được chọn trúng trong kết quả
                BigDecimal totalWinAmount = BigDecimal.ZERO;
                
                for (String selectedNumber : selectedNumbers) {
                    // Đếm số lần số này xuất hiện trong winningNumbers
                    long winCount = winningNumbers.stream().filter(wn -> wn.equals(selectedNumber)).count();
                    
                    if (winCount > 0) {
                        // Lần đầu: tiền thắng đầy đủ (đã bao gồm trừ gốc logic)
                        BigDecimal baseWin = totalBetPoints.multiply(bet.getOdds());
                        totalWinAmount = totalWinAmount.add(baseWin);
                        
                        // Từ lần thứ 2 trở đi: chỉ cộng thêm lãi (không trừ gốc)
                        if (winCount > 1) {
                            BigDecimal bonusWin = totalBetPoints.multiply(bet.getOdds()).multiply(BigDecimal.valueOf(winCount - 1));
                            totalWinAmount = totalWinAmount.add(bonusWin);
                            
                            log.info("Bonus win for number {} (won {} times): base={}, bonus={}", 
                                    selectedNumber, winCount, baseWin, bonusWin);
                        }
                    }
                }
                
                winAmount = totalWinAmount;
                
                log.info("Loto (2s/3s/4s/giai-nhat/dac-biet) win calculation with bonus: betType={}, total selected numbers: {}, total win amount: {} points", 
                        bet.getBetType(), selectedNumbers.size(), totalWinAmount);
            } else {
                // Các loại khác: chỉ cộng tiền lãi (trừ vốn vì đã bị trừ khi đặt cược)
                winAmount = bet.getPotentialWin().subtract(bet.getTotalAmount()); // Chỉ lãi, không bao gồm vốn
            }
            
            bet.setWinAmount(winAmount);
            
            // Cộng tiền LÃI vào tài khoản (thua là mất luôn, không hoàn vốn)
            User user = bet.getUser();
            BigDecimal profitPoints = winAmount; // Chỉ là tiền lãi
            
            long pointsBefore = user.getPoints();
            
            // Cộng tiền lãi vào tài khoản:
            // 1. Cộng điểm lãi vào user.points
            // 2. Cập nhật UserPoint entity
            // 3. Tạo PointTransaction record
            pointService.addPoints(user, profitPoints, 
                com.xsecret.entity.PointTransaction.PointTransactionType.BET_WIN,
                "Thắng cược (chỉ lãi): " + winAmount + " điểm", "BET", bet.getId(), null);
            
            long pointsAfter = user.getPoints();
            log.info("Bet {} WON! Profit amount: {} points (thua mất luôn, không hoàn vốn). User points: {} -> {}", 
                    bet.getId(), winAmount, pointsBefore, pointsAfter);
        } else {
            bet.setStatus(Bet.BetStatus.LOST);
            bet.setWinAmount(BigDecimal.ZERO);
            log.info("Bet {} LOST", bet.getId());
        }

        betRepository.save(bet);
    }

    /**
     * Logic kiểm tra kết quả
     */
    private boolean simulateBetResult(Bet bet) {
        // Loto 2 số: check 2 số cuối tất cả giải
        if ("loto2s".equals(bet.getBetType()) || "loto-2-so".equals(bet.getBetType())) {
            return checkLoto2sResult(bet);
        }
        
        // Loto 3 số: check 3 số cuối tất cả giải
        if ("loto3s".equals(bet.getBetType()) || "loto-3s".equals(bet.getBetType())) {
            return checkLoto3sResult(bet);
        }
        
        // Loto 4 số: check 4 số cuối tất cả giải
        if ("loto4s".equals(bet.getBetType()) || "loto-4s".equals(bet.getBetType())) {
            return checkLoto4sResult(bet);
        }
        
        // Giải nhất: CHỈ check 2 số cuối giải nhất
        if ("giai-nhat".equals(bet.getBetType())) {
            return checkGiaiNhatResult(bet);
        }
        
        // Đặc biệt: CHỈ check 2 số cuối giải đặc biệt
        if ("dac-biet".equals(bet.getBetType())) {
            return checkDacBietResult(bet);
        }
        
        // Mock: 10% cơ hội thắng cho các loại khác
        return Math.random() < 0.1;
    }
    
    /**
     * Kiểm tra kết quả loto2s: tìm tất cả số trúng và tính tiền thắng cho từng số
     */
    private boolean checkLoto2sResult(Bet bet) {
        try {
            // Parse selected numbers từ JSON
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Mock kết quả xổ số (thay thế bằng API thật sau)
            List<String> lotteryResults = getMockLotteryResults();
            
            // Tìm TẤT CẢ số trúng (không break để đếm được nhiều lần)
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                for (String result : lotteryResults) {
                    if (result.length() >= 2) {
                        String lastTwoDigits = result.substring(result.length() - 2);
                        if (selectedNumber.equals(lastTwoDigits)) {
                            winningNumbers.add(selectedNumber);
                            log.info("Loto2s WIN: Selected {} matches last 2 digits of {}", selectedNumber, result);
                            // KHÔNG BREAK để có thể tìm thấy số trúng nhiều lần
                        }
                    }
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("Loto2s LOSE: No matches found for selected numbers: {}", selectedNumbers);
                return false;
            }
            
            // Lưu danh sách số trúng vào bet
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("Loto2s WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking loto2s result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Kiểm tra kết quả loto3s: tìm tất cả số trúng và tính tiền thắng cho từng số
     * Check 3 số cuối của kết quả xổ số
     */
    private boolean checkLoto3sResult(Bet bet) {
        try {
            // Parse selected numbers từ JSON
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Mock kết quả xổ số (thay thế bằng API thật sau)
            List<String> lotteryResults = getMockLotteryResults();
            
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
    
    /**
     * Kiểm tra kết quả loto4s: tìm tất cả số trúng và tính tiền thắng cho từng số
     * Check 4 số cuối của kết quả xổ số
     */
    private boolean checkLoto4sResult(Bet bet) {
        try {
            // Parse selected numbers từ JSON
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Mock kết quả xổ số (thay thế bằng API thật sau)
            List<String> lotteryResults = getMockLotteryResults();
            
            // Tìm TẤT CẢ số trúng (không break để đếm được nhiều lần)
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                for (String result : lotteryResults) {
                    if (result.length() >= 4) {
                        String lastFourDigits = result.substring(result.length() - 4);
                        if (selectedNumber.equals(lastFourDigits)) {
                            winningNumbers.add(selectedNumber);
                            log.info("Loto4s WIN: Selected {} matches last 4 digits of {}", selectedNumber, result);
                            // KHÔNG BREAK để có thể tìm thấy số trúng nhiều lần
                        }
                    }
                }
            }
            
            if (winningNumbers.isEmpty()) {
                log.info("Loto4s LOSE: No matches found for selected numbers: {}", selectedNumbers);
                return false;
            }
            
            // Lưu danh sách số trúng vào bet
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("Loto4s WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking loto4s result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Kiểm tra kết quả Giải nhất: CHỈ check 2 số cuối của giải nhất (không phải tất cả giải)
     * Chọn số như loto2s (00-99) nhưng chỉ so với giải nhất
     */
    private boolean checkGiaiNhatResult(Bet bet) {
        try {
            // Parse selected numbers từ JSON
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy giải nhất từ mock kết quả xổ số
            String giaiNhatNumber = getGiaiNhatNumber();
            
            if (giaiNhatNumber == null || giaiNhatNumber.length() < 2) {
                log.error("Không tìm thấy giải nhất hoặc giải nhất không hợp lệ");
                return false;
            }
            
            // Lấy 2 số cuối của giải nhất
            String lastTwoDigits = giaiNhatNumber.substring(giaiNhatNumber.length() - 2);
            
            // Tìm TẤT CẢ số trúng trong selected numbers
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
            
            // Lưu danh sách số trúng vào bet
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("Giai-nhat WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking giai-nhat result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Kiểm tra kết quả Đặc biệt: CHỈ check 2 số cuối của giải đặc biệt (không phải tất cả giải)
     * Chọn số như loto2s (00-99) nhưng chỉ so với giải đặc biệt
     */
    private boolean checkDacBietResult(Bet bet) {
        try {
            // Parse selected numbers từ JSON
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy giải đặc biệt từ mock kết quả xổ số
            String dacBietNumber = getDacBietNumber();
            
            if (dacBietNumber == null || dacBietNumber.length() < 2) {
                log.error("Không tìm thấy giải đặc biệt hoặc giải đặc biệt không hợp lệ");
                return false;
            }
            
            // Lấy 2 số cuối của giải đặc biệt
            String lastTwoDigits = dacBietNumber.substring(dacBietNumber.length() - 2);
            
            // Tìm TẤT CẢ số trúng trong selected numbers
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
            
            // Lưu danh sách số trúng vào bet
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("Dac-biet WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking dac-biet result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Lấy số giải nhất từ kết quả xổ số
     * Mock: Giải nhất là số thứ 2 trong danh sách (index 1)
     */
    private String getGiaiNhatNumber() {
        List<String> results = getMockLotteryResults();
        // Giải nhất là số thứ 2 (index 1): "67845"
        if (results.size() > 1) {
            return results.get(1);
        }
        return null;
    }
    
    /**
     * Lấy số giải đặc biệt từ kết quả xổ số
     * Mock: Giải đặc biệt là số đầu tiên trong danh sách (index 0)
     */
    private String getDacBietNumber() {
        List<String> results = getMockLotteryResults();
        // Giải đặc biệt là số đầu tiên (index 0): "12345"
        if (results.size() > 0) {
            return results.get(0);
        }
        return null;
    }
    
    /**
     * Mock kết quả xổ số (thay thế bằng API thật sau)
     * Hỗ trợ test cả loto 2 số (2 số cuối), loto 3 số (3 số cuối), loto 4 số (4 số cuối), giải nhất
     */
    private List<String> getMockLotteryResults() {
        // Mock kết quả có:
        // - Số 45 (2 số cuối) xuất hiện 2 lần
        // - Số 88 (2 số cuối) xuất hiện 2 lần
        // - Số 345 (3 số cuối) xuất hiện 2 lần
        // - Số 488 (3 số cuối) xuất hiện 2 lần
        // - Số 2345 (4 số cuối) xuất hiện 2 lần
        // - Số 3488 (4 số cuối) xuất hiện 2 lần
        // - Giải nhất: 67845 (2 số cuối: 45)
        return List.of(
            "12346", // Giải đặc biệt - 2 số cuối: 45, 3 số cuối: 345, 4 số cuối: 2345
            "67845", // Giải nhất - 2 số cuối: 45, 3 số cuối: 845, 4 số cuối: 7845 ⭐ CHỈ SỐ NÀY CHO GIAI-NHAT
            "23488", // Giải nhì - 2 số cuối: 88, 3 số cuối: 488, 4 số cuối: 3488
            "34567", // Giải ba - 2 số cuối: 67, 3 số cuối: 567, 4 số cuối: 4567
            "78988", // Giải tư - 2 số cuối: 88, 3 số cuối: 988, 4 số cuối: 8988
            "56789", // Giải năm - 2 số cuối: 89, 3 số cuối: 789, 4 số cuối: 6789
            "67890", // Giải sáu - 2 số cuối: 90, 3 số cuối: 890, 4 số cuối: 7890
            "11223", // Giải bảy - 2 số cuối: 23, 3 số cuối: 223, 4 số cuối: 1223
            "99345", // Giải phụ 1 - 2 số cuối: 45, 3 số cuối: 345, 4 số cuối: 9345
            "88488", // Giải phụ 2 - 2 số cuối: 88, 3 số cuối: 488, 4 số cuối: 8488
            "112345", // Giải phụ 3 - 4 số cuối: 2345 (số 2345 trúng lần 2)
            "223488"  // Giải phụ 4 - 4 số cuối: 3488 (số 3488 trúng lần 2)
        );
        
        // KẾT QUẢ TRÚNG LOTO 2 SỐ (CHỈ TÍNH 2 SỐ CUỐI):
        // - Số 45: trúng 2 lần (12345→45, 67845→45)
        // - Số 88: trúng 2 lần (23488→88, 78988→88)  
        // - Số 67: trúng 1 lần (34567→67)
        // - Số 89: trúng 1 lần (56789→89)
        // - Số 90: trúng 1 lần (67890→90)
        // - Số 23: trúng 1 lần (11223→23)
        
        // KẾT QUẢ TRÚNG LOTO 3 SỐ (CHỈ TÍNH 3 SỐ CUỐI):
        // - Số 345: trúng 2 lần (12345→345, 99345→345)
        // - Số 488: trúng 2 lần (23488→488, 88488→488)
        // - Số 845: trúng 1 lần (67845→845)
        // - Số 567: trúng 1 lần (34567→567)
        // - Số 988: trúng 1 lần (78988→988)
        // - Số 789: trúng 1 lần (56789→789)
        // - Số 890: trúng 1 lần (67890→890)
        // - Số 223: trúng 1 lần (11223→223)
        
        // KẾT QUẢ TRÚNG LOTO 4 SỐ (CHỈ TÍNH 4 SỐ CUỐI):
        // - Số 2345: trúng 2 lần (12345→2345, 112345→2345)
        // - Số 3488: trúng 2 lần (23488→3488, 223488→3488)
        // - Số 7845: trúng 1 lần (67845→7845)
        // - Số 4567: trúng 1 lần (34567→4567)
        // - Số 8988: trúng 1 lần (78988→8988)
        // - Số 6789: trúng 1 lần (56789→6789)
        // - Số 7890: trúng 1 lần (67890→7890)
        // - Số 1223: trúng 1 lần (11223→1223)
        // - Số 9345: trúng 1 lần (99345→9345)
        // - Số 8488: trúng 1 lần (88488→8488)
    }
    
    /**
     * Parse selected numbers từ JSON string
     */
    private List<String> parseSelectedNumbers(String selectedNumbersJson) {
        try {
            return objectMapper.readValue(selectedNumbersJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.error("Error parsing selected numbers: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy thống kê bet của user
     */
    @Transactional(readOnly = true)
    public BetStatisticsResponse getUserBetStatistics(Long userId) {
        long totalBetsCount = betRepository.countByUserId(userId);
        long wonBetsCount = betRepository.countByUserIdAndIsWinTrue(userId);
        double totalBetAmountSum = betRepository.getTotalBetAmountByUserId(userId);
        double totalWinAmountSum = betRepository.getTotalWinAmountByUserId(userId);

        return BetStatisticsResponse.builder()
                .totalBets(totalBetsCount)
                .wonBets(wonBetsCount)
                .lostBets(totalBetsCount - wonBetsCount)
                .winRate(totalBetsCount > 0 ? (double) wonBetsCount / totalBetsCount * 100 : 0)
                .totalBetAmount(totalBetAmountSum)
                .totalWinAmount(totalWinAmountSum)
                .netProfit(totalWinAmountSum - totalBetAmountSum)
                .build();
    }

    /**
     * Hủy bet - CHỨC NĂNG ĐÃ BỊ VÔ HIỆU HÓA
     * Đặt cược rồi thì không được hủy để tránh xung đột logic
     */
    @Transactional
    public BetResponse cancelBet(Long betId, Long userId) {
        throw new RuntimeException("Chức năng hủy cược đã bị vô hiệu hóa. Một khi đã đặt cược thì không thể hủy.");
    }

    private String convertToJsonString(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Error converting list to JSON: {}", e.getMessage());
            return "[]";
        }
    }

    private String getCurrentDateString() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * Kiểm tra loại cược được hỗ trợ
     * Hỗ trợ:
     * - Loto 2 số: loto2s (Miền Bắc), loto-2-so (Miền Trung Nam)
     * - Loto 3 số: loto3s (Miền Bắc), loto-3s (Miền Trung Nam)
     * - Loto 4 số: loto4s (Miền Bắc), loto-4s (Miền Trung Nam)
     * - Giải nhất: giai-nhat (CHỈ Miền Bắc)
     * - Đặc biệt: dac-biet (CẢ 2 miền)
     */
    private boolean isSupportedBetType(String betType) {
        return "loto2s".equals(betType) || "loto-2-so".equals(betType) 
            || "loto3s".equals(betType) || "loto-3s".equals(betType)
            || "loto4s".equals(betType) || "loto-4s".equals(betType)
            || "giai-nhat".equals(betType)
            || "dac-biet".equals(betType);
    }

    /**
     * Kiểm tra kết quả cho 1 bet cụ thể (public method để frontend gọi)
     */
    @Transactional
    public BetResponse checkSingleBetResult(Long betId, Long userId) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bet với ID: " + betId));

        // Kiểm tra quyền truy cập
        if (!bet.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem bet này");
        }

        // Nếu đã có kết quả rồi thì trả về
        if (bet.getStatus() != Bet.BetStatus.PENDING) {
            return BetResponse.fromEntity(bet);
        }

        // Kiểm tra kết quả
        checkBetResult(bet);
        
        // Refresh bet để lấy dữ liệu mới nhất
        bet = betRepository.findById(betId).orElse(bet);
        return BetResponse.fromEntity(bet);
    }

    /**
     * Đánh dấu bet đã xem kết quả (dismiss)
     */
    @Transactional
    public BetResponse dismissBetResult(Long betId, Long userId) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bet với ID: " + betId));

        // Kiểm tra quyền truy cập
        if (!bet.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền thao tác bet này");
        }

        // Chỉ cho phép dismiss bet đã có kết quả (WON hoặc LOST)
        if (bet.getStatus() == Bet.BetStatus.PENDING) {
            throw new RuntimeException("Không thể đóng bet chưa có kết quả");
        }

        log.info("User {} dismissed bet {} with status {}", userId, betId, bet.getStatus());
        return BetResponse.fromEntity(bet);
    }

    /**
     * Lấy thông tin user với số điểm mới nhất
     */
    @Transactional(readOnly = true)
    public User getUserWithCurrentPoints(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
    }
}
