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
                "Hiện tại chỉ hỗ trợ: loto2s, loto-2-so (Loto 2 số) và loto3s, loto-3s (Loto 3 số)");
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
                || "loto3s".equals(bet.getBetType()) || "loto-3s".equals(bet.getBetType())) {
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
                
                log.info("Loto (2s/3s) win calculation with bonus: betType={}, total selected numbers: {}, total win amount: {} points", 
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
        // Loto 2 số: check 2 số cuối
        if ("loto2s".equals(bet.getBetType()) || "loto-2-so".equals(bet.getBetType())) {
            return checkLoto2sResult(bet);
        }
        
        // Loto 3 số: check 3 số cuối
        if ("loto3s".equals(bet.getBetType()) || "loto-3s".equals(bet.getBetType())) {
            return checkLoto3sResult(bet);
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
     * Mock kết quả xổ số (thay thế bằng API thật sau)
     * Hỗ trợ test cả loto 2 số (2 số cuối) và loto 3 số (3 số cuối)
     */
    private List<String> getMockLotteryResults() {
        // Mock kết quả có:
        // - Số 45 (2 số cuối) xuất hiện 2 lần
        // - Số 88 (2 số cuối) xuất hiện 2 lần
        // - Số 345 (3 số cuối) xuất hiện 2 lần
        // - Số 488 (3 số cuối) xuất hiện 2 lần
        return List.of(
            "12345", // Giải đặc biệt - 2 số cuối: 45, 3 số cuối: 345
            "67845", // Giải nhất - 2 số cuối: 45, 3 số cuối: 845
            "23488", // Giải nhì - 2 số cuối: 88, 3 số cuối: 488
            "34567", // Giải ba - 2 số cuối: 67, 3 số cuối: 567
            "78988", // Giải tư - 2 số cuối: 88, 3 số cuối: 988
            "56789", // Giải năm - 2 số cuối: 89, 3 số cuối: 789
            "67890", // Giải sáu - 2 số cuối: 90, 3 số cuối: 890
            "11223", // Giải bảy - 2 số cuối: 23, 3 số cuối: 223
            "99345", // Giải phụ - 2 số cuối: 45, 3 số cuối: 345 (số 345 trúng lần 2)
            "88488"  // Giải phụ - 2 số cuối: 88, 3 số cuối: 488 (số 488 trúng lần 2)
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
     */
    private boolean isSupportedBetType(String betType) {
        return "loto2s".equals(betType) || "loto-2-so".equals(betType) 
            || "loto3s".equals(betType) || "loto-3s".equals(betType);
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
