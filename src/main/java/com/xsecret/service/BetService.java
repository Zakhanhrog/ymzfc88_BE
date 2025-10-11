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
                "Hiện tại hỗ trợ: loto2s/loto-2-so, loto3s/loto-3s, loto4s/loto-4s, loto-xien-2 (cả 2 miền), loto-xien-3 (cả 2 miền), loto-xien-4 (cả 2 miền), 3s-dac-biet (cả 2 miền), 4s-dac-biet (cả 2 miền), " +
                "giai-nhat (chỉ Miền Bắc), 3s-giai-nhat (chỉ Miền Bắc), dac-biet (cả 2 miền), dau-dac-biet (cả 2 miền)");
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
                || "loto-xien-2".equals(bet.getBetType())
                || "loto-xien-3".equals(bet.getBetType())
                || "loto-xien-4".equals(bet.getBetType())
                || "3s-dac-biet".equals(bet.getBetType())
                || "4s-dac-biet".equals(bet.getBetType())
                || "giai-nhat".equals(bet.getBetType())
                || "3s-giai-nhat".equals(bet.getBetType())
                || "dac-biet".equals(bet.getBetType())
                || "dau-dac-biet".equals(bet.getBetType())) {
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
                
                log.info("Loto (2s/3s/4s/xien-2/xien-3/xien-4/3s-dac-biet/4s-dac-biet/giai-nhat/3s-giai-nhat/dac-biet/dau-dac-biet) win calculation with bonus: betType={}, total selected numbers: {}, total win amount: {} points", 
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
        
        // Loto xiên 2: check cặp số, cả 2 phải trúng
        if ("loto-xien-2".equals(bet.getBetType())) {
            return checkLotoXien2Result(bet);
        }
        
        // Loto xiên 3: check cụm 3 số, cả 3 phải trúng
        if ("loto-xien-3".equals(bet.getBetType())) {
            return checkLotoXien3Result(bet);
        }
        
        // Loto xiên 4: check cụm 4 số, cả 4 phải trúng
        if ("loto-xien-4".equals(bet.getBetType())) {
            return checkLotoXien4Result(bet);
        }
        
        // 3s đặc biệt: CHỈ check 3 số cuối của giải đặc biệt
        if ("3s-dac-biet".equals(bet.getBetType())) {
            return check3sDacBietResult(bet);
        }
        
        // 4s đặc biệt: CHỈ check 4 số cuối của giải đặc biệt
        if ("4s-dac-biet".equals(bet.getBetType())) {
            return check4sDacBietResult(bet);
        }
        
        // Giải nhất: CHỈ check 2 số cuối giải nhất
        if ("giai-nhat".equals(bet.getBetType())) {
            return checkGiaiNhatResult(bet);
        }
        
        // 3D Giải nhất: CHỈ check 3 số cuối giải nhất
        if ("3s-giai-nhat".equals(bet.getBetType())) {
            return check3sGiaiNhatResult(bet);
        }
        
        // Đặc biệt: CHỈ check 2 số cuối giải đặc biệt
        if ("dac-biet".equals(bet.getBetType())) {
            return checkDacBietResult(bet);
        }
        
        // Đầu đặc biệt: CHỈ check 2 số ĐẦU giải đặc biệt
        if ("dau-dac-biet".equals(bet.getBetType())) {
            return checkDauDacBietResult(bet);
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
     * Kiểm tra kết quả Loto xiên 2: check cặp số, cả 2 số trong cặp phải trúng
     * Format selectedNumbers: ["12,23", "34,56"] (mỗi cặp là 1 string)
     */
    private boolean checkLotoXien2Result(Bet bet) {
        try {
            // Parse selected pairs từ JSON
            List<String> selectedPairs = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy kết quả xổ số
            List<String> lotteryResults = getMockLotteryResults();
            
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
     */
    private boolean checkLotoXien3Result(Bet bet) {
        try {
            // Parse selected groups từ JSON
            List<String> selectedGroups = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy kết quả xổ số
            List<String> lotteryResults = getMockLotteryResults();
            
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
     */
    private boolean checkLotoXien4Result(Bet bet) {
        try {
            // Parse selected groups từ JSON
            List<String> selectedGroups = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy kết quả xổ số
            List<String> lotteryResults = getMockLotteryResults();
            
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
    
    /**
     * Kiểm tra kết quả 3s đặc biệt: CHỈ check 3 số cuối của giải đặc biệt (không phải tất cả giải)
     * Chọn số từ 000-999 nhưng chỉ so với 3 số cuối giải đặc biệt
     */
    private boolean check3sDacBietResult(Bet bet) {
        try {
            // Parse selected numbers từ JSON
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy giải đặc biệt từ mock kết quả xổ số
            String dacBietNumber = getDacBietNumber();
            if (dacBietNumber == null || dacBietNumber.length() < 3) {
                log.warn("3s đặc biệt: Không tìm thấy giải đặc biệt hoặc giải đặc biệt quá ngắn: {}", dacBietNumber);
                return false;
            }
            
            // Lấy 3 số cuối của giải đặc biệt
            String lastThreeDigits = dacBietNumber.substring(dacBietNumber.length() - 3);
            log.info("3s đặc biệt: Giải đặc biệt = {}, 3 số cuối = {}", dacBietNumber, lastThreeDigits);
            
            // Tìm TẤT CẢ số trúng
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
            
            // Lưu danh sách số trúng vào bet
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("3s đặc biệt WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking 3s đặc biệt result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Kiểm tra kết quả 4s đặc biệt: CHỈ check 4 số cuối của giải đặc biệt (không phải tất cả giải)
     * Chọn số từ 0000-9999 nhưng chỉ so với 4 số cuối giải đặc biệt
     */
    private boolean check4sDacBietResult(Bet bet) {
        try {
            // Parse selected numbers từ JSON
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy giải đặc biệt từ mock kết quả xổ số
            String dacBietNumber = getDacBietNumber();
            if (dacBietNumber == null || dacBietNumber.length() < 4) {
                log.warn("4s đặc biệt: Không tìm thấy giải đặc biệt hoặc giải đặc biệt quá ngắn: {}", dacBietNumber);
                return false;
            }
            
            // Lấy 4 số cuối của giải đặc biệt
            String lastFourDigits = dacBietNumber.substring(dacBietNumber.length() - 4);
            log.info("4s đặc biệt: Giải đặc biệt = {}, 4 số cuối = {}", dacBietNumber, lastFourDigits);
            
            // Tìm TẤT CẢ số trúng
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
            
            // Lưu danh sách số trúng vào bet
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("4s đặc biệt WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking 4s đặc biệt result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Kiểm tra kết quả 3D Giải nhất: CHỈ check 3 số cuối của giải nhất (không phải tất cả giải)
     * Chọn số từ 000-999 nhưng chỉ so với 3 số cuối giải nhất
     */
    private boolean check3sGiaiNhatResult(Bet bet) {
        try {
            // Parse selected numbers từ JSON
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy giải nhất từ mock kết quả xổ số
            String giaiNhatNumber = getGiaiNhatNumber();
            
            if (giaiNhatNumber == null || giaiNhatNumber.length() < 3) {
                log.error("Không tìm thấy giải nhất hoặc giải nhất không hợp lệ (cần ít nhất 3 chữ số)");
                return false;
            }
            
            // Lấy 3 số cuối của giải nhất
            String lastThreeDigits = giaiNhatNumber.substring(giaiNhatNumber.length() - 3);
            
            // Tìm TẤT CẢ số trúng trong selected numbers
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
            
            // Lưu danh sách số trúng vào bet
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("3s-giai-nhat WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking 3s-giai-nhat result: {}", e.getMessage());
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
        // Giải đặc biệt là số đầu tiên (index 0): "12346"
        if (results.size() > 0) {
            return results.get(0);
        }
        return null;
    }
    
    /**
     * Kiểm tra kết quả Đầu Đặc biệt: CHỈ check 2 số ĐẦU của giải đặc biệt (không phải 2 số cuối)
     * Chọn số như loto2s (00-99) nhưng chỉ so với 2 số ĐẦU giải đặc biệt
     */
    private boolean checkDauDacBietResult(Bet bet) {
        try {
            // Parse selected numbers từ JSON
            List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
            
            // Lấy giải đặc biệt từ mock kết quả xổ số
            String dacBietNumber = getDacBietNumber();
            
            if (dacBietNumber == null || dacBietNumber.length() < 2) {
                log.error("Không tìm thấy giải đặc biệt hoặc giải đặc biệt không hợp lệ");
                return false;
            }
            
            // Lấy 2 số ĐẦU của giải đặc biệt (khác với dac-biet là 2 số cuối)
            String firstTwoDigits = dacBietNumber.substring(0, 2);
            
            // Tìm TẤT CẢ số trúng trong selected numbers
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
            
            // Lưu danh sách số trúng vào bet
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("Dau-dac-biet WIN: {} winning numbers: {}", winningNumbers.size(), winningNumbers);
            return true;
            
        } catch (Exception e) {
            log.error("Error checking dau-dac-biet result: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Mock kết quả xổ số (thay thế bằng API thật sau)
     * Hỗ trợ test: loto 2s (2 số cuối), loto 3s (3 số cuối), loto 4s (4 số cuối), 
     *             giải nhất (2 số cuối giải nhất), đặc biệt (2 số cuối đặc biệt), đầu đặc biệt (2 số đầu đặc biệt)
     */
    private List<String> getMockLotteryResults() {
        // Mock kết quả theo XSMB Thứ 6 10/10/2025:
        // - Giải đặc biệt: 01640 → 2 số đầu: 01, 2 số cuối: 40
        // - Giải nhất: 54778 → 2 số cuối: 78, 3 số cuối: 778
        return List.of(
            "01640", // Giải đặc biệt - 2 số đầu: 01 ⭐, 2 số cuối: 40, 3 số cuối: 640, 4 số cuối: 1640
            "54778", // Giải nhất - 2 số cuối: 78, 3 số cuối: 778, 4 số cuối: 4778 ⭐ CHỈ SỐ NÀY CHO GIAI-NHAT
            "58480", // Giải nhì - 2 số cuối: 80, 3 số cuối: 480, 4 số cuối: 8480
            "54921", // Giải nhì - 2 số cuối: 21, 3 số cuối: 921, 4 số cuối: 4921
            "50749", // Giải ba - 2 số cuối: 49, 3 số cuối: 749, 4 số cuối: 0749
            "94670", // Giải ba - 2 số cuối: 70, 3 số cuối: 670, 4 số cuối: 4670
            "56818", // Giải ba - 2 số cuối: 18, 3 số cuối: 818, 4 số cuối: 6818
            "51058", // Giải ba - 2 số cuối: 58, 3 số cuối: 058, 4 số cuối: 1058
            "03833", // Giải ba - 2 số cuối: 33, 3 số cuối: 833, 4 số cuối: 3833
            "71888", // Giải ba - 2 số cuối: 88, 3 số cuối: 888, 4 số cuối: 1888
            "8299",  // Giải tư - 2 số cuối: 99, 3 số cuối: 299, 4 số cuối: 8299
            "6500",  // Giải tư - 2 số cuối: 00, 3 số cuối: 500, 4 số cuối: 6500
            "7568",  // Giải tư - 2 số cuối: 68, 3 số cuối: 568, 4 số cuối: 7568
            "0321",  // Giải tư - 2 số cuối: 21, 3 số cuối: 321, 4 số cuối: 0321
            "2625",  // Giải năm - 2 số cuối: 25, 3 số cuối: 625, 4 số cuối: 2625
            "5349",  // Giải năm - 2 số cuối: 49, 3 số cuối: 349, 4 số cuối: 5349
            "0601",  // Giải năm - 2 số cuối: 01, 3 số cuối: 601, 4 số cuối: 0601
            "2158",  // Giải năm - 2 số cuối: 58, 3 số cuối: 158, 4 số cuối: 2158
            "8746",  // Giải năm - 2 số cuối: 46, 3 số cuối: 746, 4 số cuối: 8746
            "0990",  // Giải năm - 2 số cuối: 90, 3 số cuối: 990, 4 số cuối: 0990
            "034",   // Giải sáu - 2 số cuối: 34, 3 số cuối: 034
            "005",   // Giải sáu - 2 số cuối: 05, 3 số cuối: 005
            "095",   // Giải sáu - 2 số cuối: 95, 3 số cuối: 095
            "41",    // Giải bảy - 2 số cuối: 41
            "71",    // Giải bảy - 2 số cuối: 71
            "90",    // Giải bảy - 2 số cuối: 90
            "42"     // Giải bảy - 2 số cuối: 42
        );
        
        // KẾT QUẢ TRÚNG LOTO 2 SỐ (CHỈ TÍNH 2 SỐ CUỐI):
        // - Số 40: trúng 1 lần (01640→40)
        // - Số 78: trúng 1 lần (54778→78)
        // - Số 80: trúng 1 lần (58480→80)
        // - Số 21: trúng 2 lần (54921→21, 0321→21)
        // - Số 49: trúng 2 lần (50749→49, 5349→49)
        // - Số 70: trúng 1 lần (94670→70)
        // - Số 18: trúng 1 lần (56818→18)
        // - Số 58: trúng 2 lần (51058→58, 2158→58)
        // - Số 33: trúng 1 lần (03833→33)
        // - Số 88: trúng 1 lần (71888→88)
        // - Số 99: trúng 1 lần (8299→99)
        // - Số 00: trúng 1 lần (6500→00)
        // - Số 68: trúng 1 lần (7568→68)
        // - Số 25: trúng 1 lần (2625→25)
        // - Số 01: trúng 1 lần (0601→01)
        // - Số 46: trúng 1 lần (8746→46)
        // - Số 90: trúng 2 lần (0990→90, 90→90)
        // - Số 34: trúng 1 lần (034→34)
        // - Số 05: trúng 1 lần (005→05)
        // - Số 95: trúng 1 lần (095→95)
        // - Số 41: trúng 1 lần (41→41)
        // - Số 71: trúng 1 lần (71→71)
        // - Số 42: trúng 1 lần (42→42)
        
        // KẾT QUẢ TRÚNG LOTO 3 SỐ (CHỈ TÍNH 3 SỐ CUỐI):
        // - Số 640: trúng 1 lần (01640→640)
        // - Số 778: trúng 1 lần (54778→778)
        // - Số 480: trúng 1 lần (58480→480)
        // - Số 921: trúng 1 lần (54921→921)
        // - Số 749: trúng 1 lần (50749→749)
        // - Số 670: trúng 1 lần (94670→670)
        // - Số 818: trúng 1 lần (56818→818)
        // - Số 058: trúng 1 lần (51058→058)
        // - Số 833: trúng 1 lần (03833→833)
        // - Số 888: trúng 1 lần (71888→888)
        // - Số 299: trúng 1 lần (8299→299)
        // - Số 500: trúng 1 lần (6500→500)
        // - Số 568: trúng 1 lần (7568→568)
        // - Số 321: trúng 1 lần (0321→321)
        // - Số 625: trúng 1 lần (2625→625)
        // - Số 349: trúng 1 lần (5349→349)
        // - Số 601: trúng 1 lần (0601→601)
        // - Số 158: trúng 1 lần (2158→158)
        // - Số 746: trúng 1 lần (8746→746)
        // - Số 990: trúng 1 lần (0990→990)
        // - Số 034: trúng 1 lần (034→034)
        // - Số 005: trúng 1 lần (005→005)
        // - Số 095: trúng 1 lần (095→095)
        
        // KẾT QUẢ TRÚNG LOTO 4 SỐ (CHỈ TÍNH 4 SỐ CUỐI):
        // - Số 1640: trúng 1 lần (01640→1640)
        // - Số 4778: trúng 1 lần (54778→4778)
        // - Số 8480: trúng 1 lần (58480→8480)
        // - Số 4921: trúng 1 lần (54921→4921)
        // - Số 0749: trúng 1 lần (50749→0749)
        // - Số 4670: trúng 1 lần (94670→4670)
        // - Số 6818: trúng 1 lần (56818→6818)
        // - Số 1058: trúng 1 lần (51058→1058)
        // - Số 3833: trúng 1 lần (03833→3833)
        // - Số 1888: trúng 1 lần (71888→1888)
        // - Số 8299: trúng 1 lần (8299→8299)
        // - Số 6500: trúng 1 lần (6500→6500)
        // - Số 7568: trúng 1 lần (7568→7568)
        // - Số 0321: trúng 1 lần (0321→0321)
        // - Số 2625: trúng 1 lần (2625→2625)
        // - Số 5349: trúng 1 lần (5349→5349)
        // - Số 0601: trúng 1 lần (0601→0601)
        // - Số 2158: trúng 1 lần (2158→2158)
        // - Số 8746: trúng 1 lần (8746→8746)
        // - Số 0990: trúng 1 lần (0990→0990)
        
        // KẾT QUẢ GIẢI NHẤT (CHỈ CHECK 2 SỐ CUỐI GIẢI NHẤT):
        // - Giải nhất: 54778 → 2 số cuối: 78
        // - Chọn số 78 → TRÚNG ✅
        
        // KẾT QUẢ 3D GIẢI NHẤT (CHỈ CHECK 3 SỐ CUỐI GIẢI NHẤT):
        // - Giải nhất: 54778 → 3 số cuối: 778
        // - Chọn số 778 → TRÚNG ✅
        
        // KẾT QUẢ ĐẶC BIỆT (CHỈ CHECK 2 SỐ CUỐI GIẢI ĐẶC BIỆT):
        // - Giải đặc biệt: 01640 → 2 số cuối: 40
        // - Chọn số 40 → TRÚNG ✅
        
        // KẾT QUẢ ĐẦU ĐẶC BIỆT (CHỈ CHECK 2 SỐ ĐẦU GIẢI ĐẶC BIỆT):
        // - Giải đặc biệt: 01640 → 2 số đầu: 01
        // - Chọn số 01 → TRÚNG ✅
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
     * - Loto xiên 2: loto-xien-2 (CẢ 2 miền) - chọn cặp số, cả 2 phải trúng
     * - Loto xiên 3: loto-xien-3 (CẢ 2 miền) - chọn cụm 3 số, cả 3 phải trúng
     * - Loto xiên 4: loto-xien-4 (CẢ 2 miền) - chọn cụm 4 số, cả 4 phải trúng
     * - 3s đặc biệt: 3s-dac-biet (CẢ 2 miền) - chọn 3 số, chỉ so với 3 số cuối giải đặc biệt
     * - 4s đặc biệt: 4s-dac-biet (CẢ 2 miền) - chọn 4 số, chỉ so với 4 số cuối giải đặc biệt
     * - Giải nhất: giai-nhat (CHỈ Miền Bắc) - 2 số cuối giải nhất
     * - 3D Giải nhất: 3s-giai-nhat (CHỈ Miền Bắc) - 3 số cuối giải nhất
     * - Đặc biệt: dac-biet (CẢ 2 miền)
     * - Đầu đặc biệt: dau-dac-biet (CẢ 2 miền)
     */
    private boolean isSupportedBetType(String betType) {
        return "loto2s".equals(betType) || "loto-2-so".equals(betType) 
            || "loto3s".equals(betType) || "loto-3s".equals(betType)
            || "loto4s".equals(betType) || "loto-4s".equals(betType)
            || "loto-xien-2".equals(betType)
            || "loto-xien-3".equals(betType)
            || "loto-xien-4".equals(betType)
            || "3s-dac-biet".equals(betType)
            || "4s-dac-biet".equals(betType)
            || "giai-nhat".equals(betType)
            || "3s-giai-nhat".equals(betType)
            || "dac-biet".equals(betType)
            || "dau-dac-biet".equals(betType);
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
