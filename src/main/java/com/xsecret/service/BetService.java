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
            throw new RuntimeException("Loại cược '" + request.getBetType() + "' chưa được hỗ trợ. Hiện tại chỉ hỗ trợ 'loto2s'");
        }

        // Lấy thông tin user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Tính toán - sử dụng điểm để đặt cược
        BigDecimal betPoints = request.getBetAmount(); // Số điểm cược (nhận trực tiếp từ frontend)
        List<String> selectedNumbers = parseSelectedNumbers(convertToJsonString(request.getSelectedNumbers()));
        int selectedCount = selectedNumbers.size();
        
        // Tiền đặt cược = số điểm cược × đơn giá × số lô đã chọn
        BigDecimal betAmount = betPoints.multiply(request.getPricePerPoint()).multiply(BigDecimal.valueOf(selectedCount));
        BigDecimal potentialWin = betAmount.multiply(request.getOdds()); // Tiền thắng tiềm năng

        // Log bet calculation
        log.info("Bet calculation - betPoints: {}, selectedCount: {}, betAmount: {}, userPoints: {}, pricePerPoint: {}", 
                betPoints, selectedCount, betAmount, user.getPoints(), request.getPricePerPoint());

        // Kiểm tra số điểm có đủ không (trừ tiền đặt cược)
        long pointsToDeductLong = betAmount.longValue();
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
                .betAmount(betPoints) // Số điểm cược
                .pricePerPoint(request.getPricePerPoint()) // Đơn giá 1 điểm
                .totalAmount(betAmount) // Tổng tiền cược (điểm)
                .odds(request.getOdds())
                .potentialWin(potentialWin) // Tiền thắng tiềm năng (điểm)
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
            
            // Tính tiền thắng: cộng tiền thắng của từng số trúng
            BigDecimal winAmount;
            if ("loto2s".equals(bet.getBetType()) || "loto-2-so".equals(bet.getBetType())) {
                // Cho loto2s: tính tiền thắng dựa trên số lượng số trúng
                List<String> winningNumbers = parseSelectedNumbers(bet.getWinningNumbers());
                int winningCount = winningNumbers.size();
                
                // Tiền thắng = (số điểm cược × đơn giá × tỷ lệ cược) × số lượng số trúng
                // Ví dụ: 1 điểm × 27,000 VND/điểm × 99 × 1 số trúng = 2,673,000 VND
                BigDecimal betPoints = bet.getBetAmount(); // Số điểm cược
                BigDecimal singleBetAmount = betPoints.multiply(bet.getPricePerPoint()); // Tiền cược cho 1 số
                BigDecimal singleWinAmount = singleBetAmount.multiply(bet.getOdds()); // Tiền thắng cho 1 số
                winAmount = singleWinAmount.multiply(BigDecimal.valueOf(winningCount)); // Tổng tiền thắng
                
                log.info("Loto2s win calculation: {} winning numbers, {} points per number, {} VND per win, total: {} VND", 
                        winningCount, betPoints, singleWinAmount, winAmount);
            } else {
                // Các loại khác: giữ nguyên logic cũ
                winAmount = bet.getPotentialWin();
            }
            
            bet.setWinAmount(winAmount);
            
            // Cộng tiền thắng vào tài khoản (đã là điểm)
            User user = bet.getUser();
            BigDecimal winPoints = winAmount; // Đã là điểm rồi, không cần chia 1000
            
            long pointsBefore = user.getPoints();
            long pointsAfter = pointsBefore + winPoints.longValue();
            user.setPoints(pointsAfter);
            userRepository.save(user);
            
            // Cập nhật UserPoint entity để đồng bộ
            pointService.addPoints(user, winPoints, 
                com.xsecret.entity.PointTransaction.PointTransactionType.BET_WIN,
                "Thắng cược: " + winAmount + " điểm", "BET", bet.getId(), null);
            
            log.info("Bet {} WON! Win amount: {} points. User points: {} -> {}", bet.getId(), winAmount, pointsBefore, pointsAfter);
        } else {
            bet.setStatus(Bet.BetStatus.LOST);
            bet.setWinAmount(BigDecimal.ZERO);
            log.info("Bet {} LOST", bet.getId());
        }

        betRepository.save(bet);
    }

    /**
     * Logic kiểm tra kết quả cho loto2s
     */
    private boolean simulateBetResult(Bet bet) {
        if ("loto2s".equals(bet.getBetType()) || "loto-2-so".equals(bet.getBetType())) {
            return checkLoto2sResult(bet);
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
            
            // Tìm tất cả số trúng
            List<String> winningNumbers = new ArrayList<>();
            for (String selectedNumber : selectedNumbers) {
                for (String result : lotteryResults) {
                    if (result.length() >= 2) {
                        String lastTwoDigits = result.substring(result.length() - 2);
                        if (selectedNumber.equals(lastTwoDigits)) {
                            winningNumbers.add(selectedNumber);
                            log.info("Loto2s WIN: Selected {} matches last 2 digits of {}", selectedNumber, result);
                            break; // Chỉ cần tìm thấy 1 lần trùng là đủ
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
     * Mock kết quả xổ số (thay thế bằng API thật sau)
     */
    private List<String> getMockLotteryResults() {
        // Mock kết quả xổ số Miền Bắc
        return List.of(
            "09565", // Giải đặc biệt
            "12345", // Giải nhất
            "23456", // Giải nhì
            "34567", // Giải ba
            "45678", // Giải tư
            "56789", // Giải năm
            "67890", // Giải sáu
            "78901"  // Giải bảy
        );
    }
    
    /**
     * Parse selected numbers từ JSON string
     */
    private List<String> parseSelectedNumbers(String selectedNumbersJson) {
        try {
            return objectMapper.readValue(selectedNumbersJson, List.class);
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
     * Hủy bet (chỉ được hủy khi chưa có kết quả)
     */
    @Transactional
    public BetResponse cancelBet(Long betId, Long userId) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bet với ID: " + betId));

        if (!bet.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy bet này");
        }

        if (bet.getStatus() != Bet.BetStatus.PENDING) {
            throw new RuntimeException("Không thể hủy bet đã có kết quả");
        }

        // Hoàn điểm
        User user = bet.getUser();
        BigDecimal refundAmount = bet.getTotalAmount();
        BigDecimal refundPoints = refundAmount.divide(BigDecimal.valueOf(1000), 0, java.math.RoundingMode.UP);
        
        long pointsBefore = user.getPoints();
        long pointsAfter = pointsBefore + refundPoints.longValue();
        user.setPoints(pointsAfter);
        userRepository.save(user);
        
        // Cập nhật UserPoint entity để đồng bộ
        pointService.addPoints(user, refundPoints, 
            com.xsecret.entity.PointTransaction.PointTransactionType.BET_REFUND,
            "Hoàn cược: " + refundAmount + " VND (" + refundPoints + " điểm)", "BET", bet.getId(), null);
        
        log.info("Refunded {} points to user {}. Points: {} -> {}", refundPoints, userId, pointsBefore, pointsAfter);

        // Cập nhật trạng thái bet
        bet.setStatus(Bet.BetStatus.CANCELLED);
        bet.setUpdatedAt(LocalDateTime.now());
        betRepository.save(bet);

        log.info("Bet {} cancelled by user {}", betId, userId);
        return BetResponse.fromEntity(bet);
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
     */
    private boolean isSupportedBetType(String betType) {
        return "loto2s".equals(betType) || "loto-2-so".equals(betType);
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
     * Lấy thông tin user với số điểm mới nhất
     */
    @Transactional(readOnly = true)
    public User getUserWithCurrentPoints(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));
    }
}
