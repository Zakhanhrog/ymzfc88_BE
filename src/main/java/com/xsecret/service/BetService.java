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
import com.xsecret.service.bet.checker.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetService {

    private final BetRepository betRepository;
    private final UserRepository userRepository;
    private final PointService pointService;
    private final ObjectMapper objectMapper;
    
    // Inject checker classes - GIỮ NGUYÊN 100% logic
    private final Loto2sResultChecker loto2sChecker;
    private final Loto3sResultChecker loto3sChecker;
    private final Loto4sResultChecker loto4sChecker;
    private final XienResultChecker xienChecker;
    private final SpecialResultChecker specialChecker;
    private final TruotResultChecker truotChecker;

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
                "giai-nhat (chỉ Miền Bắc), 3s-giai-nhat (chỉ Miền Bắc), 3s-giai-6 (chỉ Miền Bắc), de-giai-7 (chỉ Miền Bắc), dau-duoi (chỉ Miền Bắc), 3s-dau-duoi (chỉ Miền Bắc), dac-biet (cả 2 miền), dau-dac-biet (cả 2 miền), de-giai-8 (chỉ Miền Trung Nam), dau-duoi-mien-trung-nam (chỉ Miền Trung Nam), 3s-dau-duoi-mien-trung-nam (chỉ Miền Trung Nam), 3s-giai-7 (chỉ Miền Trung Nam), loto-truot-4/8/10 (cả 2 miền)");
        }

        // Lấy thông tin user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Tính toán - sử dụng điểm để đặt cược
        BigDecimal betPoints = request.getBetAmount(); // Tổng số điểm cược (chia đều cho các số)
        List<String> selectedNumbers = parseSelectedNumbers(convertToJsonString(request.getSelectedNumbers()));
        int selectedCount = selectedNumbers.size();
        
        // ĐẶC BIỆT: multiplier cho các loại đặc biệt
        int multiplier = 1;
        if ("de-giai-7".equals(request.getBetType())) {
            multiplier = 4; // Giải 7 có 4 số
        } else if ("3s-giai-6".equals(request.getBetType())) {
            multiplier = 3; // Giải 6 có 3 số
        } else if ("dau-duoi".equals(request.getBetType())) {
            multiplier = 5; // Giải đặc biệt (1) + Giải 7 (4) = 5 số
        } else if ("3s-dau-duoi".equals(request.getBetType())) {
            multiplier = 4; // Giải đặc biệt (1) + Giải 6 (3) = 4 số
        } else if ("dau-duoi-mien-trung-nam".equals(request.getBetType())) {
            multiplier = 2; // Giải đặc biệt (1) + Giải 8 (1) = 2 số
        } else if ("3s-dau-duoi-mien-trung-nam".equals(request.getBetType())) {
            multiplier = 2; // Giải đặc biệt (1) + Giải 7 (1) = 2 số
        }
        
        // Tiền đặt cược = số điểm × đơn giá × số lượng số × multiplier
        // Ví dụ: 10 điểm × 27 × 3 số = 810 (thường)
        // Ví dụ de-giai-7: 10 điểm × 1,000 × 1 số × 4 = 40,000
        // Ví dụ 3s-giai-6: 10 điểm × 1,000 × 1 số × 3 = 30,000
        // Ví dụ dau-duoi: 10 điểm × 1,000 × 1 số × 5 = 50,000
        // Ví dụ 3s-dau-duoi: 10 điểm × 1,000 × 1 số × 4 = 40,000
        // Ví dụ dau-duoi-mien-trung-nam: 10 điểm × 1,000 × 1 số × 2 = 20,000
        BigDecimal totalBetAmount = betPoints.multiply(request.getPricePerPoint())
                                            .multiply(BigDecimal.valueOf(selectedCount))
                                            .multiply(BigDecimal.valueOf(multiplier));
        
        // Tiền thắng nếu tất cả trúng = số điểm × tỷ lệ × số lượng số (KHÔNG × multiplier)
        // Ví dụ: 10 điểm × 99 × 3 số = 2,970 (thường)
        // Ví dụ de-giai-7: 10 điểm × 23 × 1 số = 230 (KHÔNG × 4)
        // Ví dụ 3s-giai-6: 10 điểm × 600 × 1 số = 6,000 (KHÔNG × 3)
        // Ví dụ dau-duoi: 10 điểm × 12 × 1 số = 120 (KHÔNG × 5)
        // Ví dụ 3s-dau-duoi: 10 điểm × 150 × 1 số = 1,500 (KHÔNG × 4)
        // Ví dụ dau-duoi-mien-trung-nam: 10 điểm × 50 × 1 số = 500 (KHÔNG × 2)
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
                || "dau-dac-biet".equals(bet.getBetType())
                || "3s-giai-6".equals(bet.getBetType())
                || "de-giai-7".equals(bet.getBetType())
                || "dau-duoi".equals(bet.getBetType())
                || "3s-dau-duoi".equals(bet.getBetType())
                || "de-giai-8".equals(bet.getBetType())
                || "dau-duoi-mien-trung-nam".equals(bet.getBetType())
                || "3s-giai-7".equals(bet.getBetType())
                || "3s-dau-duoi-mien-trung-nam".equals(bet.getBetType())
                || "loto-truot-4".equals(bet.getBetType())
                || "loto-truot-8".equals(bet.getBetType())
                || "loto-truot-10".equals(bet.getBetType())) {
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
                
                log.info("Loto (2s/3s/4s/xien-2/xien-3/xien-4/3s-dac-biet/4s-dac-biet/giai-nhat/3s-giai-nhat/3s-giai-6/dac-biet/dau-dac-biet/dau-duoi/3s-dau-duoi/de-giai-7/de-giai-8/dau-duoi-mien-trung-nam/3s-giai-7/truot-4/truot-8/truot-10) win calculation with bonus: betType={}, total selected numbers: {}, total win amount: {} points", 
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
     * Logic kiểm tra kết quả - REFACTORED: sử dụng checker classes
     * GIỮ NGUYÊN 100% logic, chỉ gọi checker thay vì method cũ
     */
    private boolean simulateBetResult(Bet bet) {
        // Loto 2 số: check 2 số cuối tất cả giải
        if ("loto2s".equals(bet.getBetType()) || "loto-2-so".equals(bet.getBetType())) {
            return loto2sChecker.checkResult(bet);
        }
        
        // Loto 3 số: check 3 số cuối tất cả giải
        if ("loto3s".equals(bet.getBetType()) || "loto-3s".equals(bet.getBetType())) {
            return loto3sChecker.checkResult(bet);
        }
        
        // Loto 4 số: check 4 số cuối tất cả giải
        if ("loto4s".equals(bet.getBetType()) || "loto-4s".equals(bet.getBetType())) {
            return loto4sChecker.checkResult(bet);
        }
        
        // Loto xiên 2: check cặp số, cả 2 phải trúng
        if ("loto-xien-2".equals(bet.getBetType())) {
            return xienChecker.checkXien2Result(bet);
        }
        
        // Loto xiên 3: check cụm 3 số, cả 3 phải trúng
        if ("loto-xien-3".equals(bet.getBetType())) {
            return xienChecker.checkXien3Result(bet);
        }
        
        // Loto xiên 4: check cụm 4 số, cả 4 phải trúng
        if ("loto-xien-4".equals(bet.getBetType())) {
            return xienChecker.checkXien4Result(bet);
        }
        
        // 3s đặc biệt: CHỈ check 3 số cuối của giải đặc biệt
        if ("3s-dac-biet".equals(bet.getBetType())) {
            return specialChecker.check3sDacBietResult(bet);
        }
        
        // 4s đặc biệt: CHỈ check 4 số cuối của giải đặc biệt
        if ("4s-dac-biet".equals(bet.getBetType())) {
            return specialChecker.check4sDacBietResult(bet);
        }
        
        // Giải nhất: CHỈ check 2 số cuối giải nhất
        if ("giai-nhat".equals(bet.getBetType())) {
            return specialChecker.checkGiaiNhatResult(bet);
        }
        
        // 3D Giải nhất: CHỈ check 3 số cuối giải nhất
        if ("3s-giai-nhat".equals(bet.getBetType())) {
            return specialChecker.check3sGiaiNhatResult(bet);
        }
        
        // 3s giải 6: CHỈ check 3 số cuối của TẤT CẢ 3 số giải 6 (chỉ Miền Bắc)
        if ("3s-giai-6".equals(bet.getBetType())) {
            return specialChecker.check3sGiai6Result(bet);
        }
        
        // Đặc biệt: CHỈ check 2 số cuối giải đặc biệt
        if ("dac-biet".equals(bet.getBetType())) {
            return specialChecker.checkDacBietResult(bet);
        }
        
        // Đầu đặc biệt: CHỈ check 2 số ĐẦU giải đặc biệt
        if ("dau-dac-biet".equals(bet.getBetType())) {
            return specialChecker.checkDauDacBietResult(bet);
        }
        
        // Đầu/đuôi: CHỈ check 2 số cuối giải đặc biệt + TẤT CẢ 4 số giải 7 (chỉ Miền Bắc)
        if ("dau-duoi".equals(bet.getBetType())) {
            return specialChecker.checkDauDuoiResult(bet);
        }
        
        // 3s đầu đuôi: CHỈ check 3 số cuối giải đặc biệt + TẤT CẢ 3 số giải 6 (chỉ Miền Bắc)
        if ("3s-dau-duoi".equals(bet.getBetType())) {
            return specialChecker.check3sDauDuoiResult(bet);
        }
        
        // 3s đầu đuôi Miền Trung Nam: CHỈ check 3 số cuối giải đặc biệt + 1 số giải 7 (chỉ Miền Trung Nam)
        if ("3s-dau-duoi-mien-trung-nam".equals(bet.getBetType())) {
            return specialChecker.check3sDauDuoiMienTrungNamResult(bet);
        }
        
        // Đầu/đuôi Miền Trung Nam: CHỈ check 2 số cuối giải đặc biệt + giải 8 (chỉ Miền Trung Nam)
        if ("dau-duoi-mien-trung-nam".equals(bet.getBetType())) {
            return specialChecker.checkDauDuoiMienTrungNamResult(bet);
        }
        
        // Đề giải 8: CHỈ check 2 số cuối giải 8 (chỉ Miền Trung Nam)
        if ("de-giai-8".equals(bet.getBetType())) {
            return specialChecker.checkDeGiai8Result(bet);
        }
        
        // Đề giải 7: CHỈ check 2 số cuối của TẤT CẢ 4 số giải 7 (chỉ Miền Bắc)
        if ("de-giai-7".equals(bet.getBetType())) {
            return specialChecker.checkDeGiai7Result(bet);
        }
        
        // 3s giải 7: CHỈ check 3 số cuối giải 7 (chỉ Miền Trung Nam)
        if ("3s-giai-7".equals(bet.getBetType())) {
            return specialChecker.check3sGiai7Result(bet);
        }
        
        // Loto trượt 4: CẢ 4 số đều KHÔNG trúng → THẮNG
        if ("loto-truot-4".equals(bet.getBetType())) {
            return truotChecker.checkTruot4Result(bet);
        }
        
        // Loto trượt 8: CẢ 8 số đều KHÔNG trúng → THẮNG
        if ("loto-truot-8".equals(bet.getBetType())) {
            return truotChecker.checkTruot8Result(bet);
        }
        
        // Loto trượt 10: CẢ 10 số đều KHÔNG trúng → THẮNG
        if ("loto-truot-10".equals(bet.getBetType())) {
            return truotChecker.checkTruot10Result(bet);
        }
        
        // Mock: 10% cơ hội thắng cho các loại khác
        return Math.random() < 0.1;
    }
    
    // ==========================================
    // CÁC METHOD CHECK CŨ ĐÃ ĐƯỢC CHUYỂN SANG CHECKER CLASSES
    // Code đã được refactor, xem: com.xsecret.service.bet.checker.*
    // ==========================================
    
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
     * - Đề giải 8: de-giai-8 (CHỈ Miền Trung Nam) - 2 số cuối giải 8
     * - 3s đầu đuôi: 3s-dau-duoi-mien-trung-nam (CHỈ Miền Trung Nam) - 3 số cuối giải đặc biệt + 1 số giải 7
     * - 3s giải 7: 3s-giai-7 (CHỈ Miền Trung Nam) - 3 số cuối giải 7
     * - Loto trượt 4-10: loto-truot-4 đến loto-truot-10 (CẢ 2 miền) - cả N số đều không trúng
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
            || "3s-giai-6".equals(betType)
            || "de-giai-7".equals(betType)
            || "dau-duoi".equals(betType)
            || "3s-dau-duoi".equals(betType)
            || "dac-biet".equals(betType)
            || "dau-dac-biet".equals(betType)
            || "de-giai-8".equals(betType)
            || "dau-duoi-mien-trung-nam".equals(betType)
            || "3s-dau-duoi-mien-trung-nam".equals(betType)
            || "3s-giai-7".equals(betType)
            || "loto-truot-4".equals(betType)
            || "loto-truot-8".equals(betType)
            || "loto-truot-10".equals(betType);
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
    
    // ======================== ADMIN METHODS ========================
    
    /**
     * Admin: Lấy tất cả bet với filter
     */
    @Transactional(readOnly = true)
    public Page<BetResponse> getAllBetsForAdmin(
            String status, String betType, String region, Long userId, 
            String searchTerm, Pageable pageable) {
        
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Tìm kiếm theo username hoặc betId
            return betRepository.searchBets(searchTerm.trim(), pageable)
                    .map(BetResponse::fromEntity);
        }
        
        Bet.BetStatus betStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                betStatus = Bet.BetStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}", status);
            }
        }
        
        return betRepository.findAllBetsWithFilters(
                betStatus, 
                betType, 
                region, 
                userId, 
                pageable)
                .map(BetResponse::fromEntity);
    }
    
    /**
     * Admin: Lấy thống kê bet
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getBetStatisticsForAdmin() {
        long totalBets = betRepository.countAllBets();
        long pendingBets = betRepository.countByStatus(Bet.BetStatus.PENDING);
        long wonBets = betRepository.countByStatus(Bet.BetStatus.WON);
        long lostBets = betRepository.countByStatus(Bet.BetStatus.LOST);
        double totalBetAmount = betRepository.getTotalBetAmount();
        double totalWinAmount = betRepository.getTotalWinAmount();
        
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalBets", totalBets);
        stats.put("pendingBets", pendingBets);
        stats.put("wonBets", wonBets);
        stats.put("lostBets", lostBets);
        stats.put("totalBetAmount", totalBetAmount);
        stats.put("totalWinAmount", totalWinAmount);
        stats.put("netProfit", totalBetAmount - totalWinAmount); // Lợi nhuận của hệ thống
        
        return stats;
    }
    
    /**
     * Admin: Set kết quả xổ số cho bet
     * CHỈ cho phép set khi bet chưa có kết quả (PENDING)
     * Hệ thống sẽ tự động check thắng/thua khi đến thời gian
     */
    @Transactional
    public BetResponse updateBetResult(Long betId, List<String> winningNumbers) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bet với ID: " + betId));
        
        // QUAN TRỌNG: Chỉ cho phép set khi bet chưa có kết quả
        if (bet.getStatus() != Bet.BetStatus.PENDING) {
            throw new RuntimeException("Không thể thay đổi kết quả của bet đã có kết quả. Chỉ có thể thay đổi bet có status = PENDING");
        }
        
        // Admin chỉ set kết quả xổ số, không tự động tính thắng/thua
        if (winningNumbers == null || winningNumbers.isEmpty()) {
            bet.setWinningNumbers("[]");
            log.info("Admin set bet {} with no winning numbers", betId);
        } else {
            bet.setWinningNumbers(convertToJsonString(winningNumbers));
            log.info("Admin set bet {} with winning numbers: {}", betId, winningNumbers);
        }
        
        betRepository.save(bet);
        return BetResponse.fromEntity(bet);
    }
    
    /**
     * Tính tiền thắng cho bet
     */
    private BigDecimal calculateWinAmount(Bet bet, List<String> winningNumbers) {
        List<String> selectedNumbers = parseSelectedNumbers(bet.getSelectedNumbers());
        BigDecimal totalBetPoints = bet.getBetAmount();
        
        if (winningNumbers == null || winningNumbers.isEmpty()) {
            // Nếu không có winning numbers, tính toàn bộ
            return totalBetPoints.multiply(bet.getOdds());
        }
        
        // Tính theo số lượng số trúng
        int winningCount = winningNumbers.size();
        return totalBetPoints.multiply(bet.getOdds()).multiply(BigDecimal.valueOf(winningCount));
    }
    
    /**
     * Admin: Xóa/Hủy bet
     * CHỈ cho phép xóa/hủy khi bet chưa có kết quả (PENDING)
     */
    @Transactional
    public void deleteBet(Long betId) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bet với ID: " + betId));
        
        // QUAN TRỌNG: Chỉ cho phép xóa khi bet chưa có kết quả
        if (bet.getStatus() != Bet.BetStatus.PENDING) {
            throw new RuntimeException("Không thể xóa bet đã có kết quả. Chỉ có thể xóa bet có status = PENDING");
        }
        
        log.info("Admin deleting bet {}", betId);
        
        // Hoàn tiền cho user
        User user = bet.getUser();
        BigDecimal refundAmount = bet.getTotalAmount();
        
        pointService.addPoints(user, refundAmount, 
            com.xsecret.entity.PointTransaction.PointTransactionType.BET_REFUND,
            "Hoàn tiền cược (Admin xóa): " + refundAmount + " điểm", "BET", bet.getId(), null);
        
        // Xóa bet
        betRepository.delete(bet);
        log.info("Bet {} deleted and refunded {} points to user {}", betId, refundAmount, user.getId());
    }
    
    /**
     * Admin: Lấy chi tiết bet
     */
    @Transactional(readOnly = true)
    public BetResponse getBetByIdForAdmin(Long betId) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bet với ID: " + betId));
        return BetResponse.fromEntity(bet);
    }
}
