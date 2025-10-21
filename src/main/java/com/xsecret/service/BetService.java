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
import com.xsecret.service.bet.checker.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        log.info("User {} placing bet: region={}, province={}, betType={}, numbers={}, amount={}", 
                userId, request.getRegion(), request.getProvince(), request.getBetType(), request.getSelectedNumbers(), request.getBetAmount());

        // Kiểm tra thời gian khóa cược theo vùng miền
        checkBettingTimeLimit(request.getRegion(), request.getProvince());

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
                .province(request.getProvince())
                .betType(request.getBetType())
                .selectedNumbers(convertToJsonString(request.getSelectedNumbers()))
                .betAmount(betPoints) // Tổng số điểm cược (chia đều cho các số)
                .pricePerPoint(request.getPricePerPoint()) // Đơn giá 1 điểm
                .totalAmount(totalBetAmount) // Tổng tiền cược (điểm)
                .odds(request.getOdds())
                .potentialWin(potentialWin) // Tổng tiền có thể nhận (gốc + lãi)
                .status(Bet.BetStatus.PENDING)
                .resultDate(getBetResultDate(request.getRegion(), request.getProvince()))
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
     * Kiểm tra kết quả bet - tự động check mỗi 10 giây
     * KHÔNG CẦN @Transactional vì:
     * 1. Query đã JOIN FETCH user, không cần lazy loading
     * 2. Mỗi bet có transaction riêng (REQUIRES_NEW)
     * 3. Tránh conflict với nested transactions
     * CHỈ CHECK BET CỦA HÔM NAY, nếu chưa có kết quả thì bỏ qua
     */
    public void checkBetResults() {
        String currentDate = getCurrentDateString();
        log.info("========================================");
        log.info("🔍 AUTO CHECK STARTING - Current date: {}", currentDate);
        log.info("========================================");
        
        List<Bet> pendingBets = betRepository.findPendingBetsToCheck(currentDate);

        log.info("📊 Found {} PENDING bets for date: {}", pendingBets.size(), currentDate);
        
        if (pendingBets.isEmpty()) {
            log.info("✅ No pending bets to check for today");
            return;
        }

        int successCount = 0;
        int errorCount = 0;
        int skippedCount = 0;
        
        for (Bet bet : pendingBets) {
            try {
                log.info("⚡ Processing bet ID: {}, betType: {}, userId: {}, region: {}, province: {}", 
                        bet.getId(), bet.getBetType(), bet.getUser().getId(), bet.getRegion(), bet.getProvince());
                checkBetResult(bet);
                successCount++;
                log.info("✅ Successfully checked bet ID: {}", bet.getId());
            } catch (RuntimeException e) {
                // Nếu lỗi do chưa có kết quả xổ số thì skip (không đếm là lỗi)
                if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                    skippedCount++;
                    log.warn("⏭️ Skipped bet ID {} - Chưa có kết quả xổ số: {}", bet.getId(), e.getMessage());
                } else {
                    errorCount++;
                    log.error("❌ Error checking result for bet ID {}: {}", bet.getId(), e.getMessage(), e);
                }
            } catch (Exception e) {
                errorCount++;
                log.error("❌ Unexpected error checking bet ID {}: {}", bet.getId(), e.getMessage(), e);
            }
        }
        
        log.info("========================================");
        log.info("📈 Bet check COMPLETED: ✅ {} successful, ⏭️ {} skipped (no result), ❌ {} errors out of {} total", 
                successCount, skippedCount, errorCount, pendingBets.size());
        log.info("========================================");
    }

    /**
     * Kiểm tra kết quả bet cho ngày cụ thể (dùng khi admin publish kết quả)
     * KHÔNG CẦN @Transactional vì:
     * 1. Query đã JOIN FETCH user, không cần lazy loading
     * 2. Mỗi bet có transaction riêng (REQUIRES_NEW)
     * 3. Tránh conflict với nested transactions
     * CHỈ CHECK BET CỦA NGÀY ĐƯỢC CHỈ ĐỊNH
     */
    public void checkBetResultsForDate(String targetDate) {
        log.info("========================================");
        log.info("🎯 ADMIN TRIGGERED CHECK - Target date: {}", targetDate);
        log.info("========================================");
        
        List<Bet> pendingBets = betRepository.findPendingBetsToCheck(targetDate);

        log.info("📊 Found {} PENDING bets for specific date: {}", pendingBets.size(), targetDate);
        
        // Debug: Log chi tiết từng bet
        for (Bet bet : pendingBets) {
            log.info("🔍 DEBUG Bet ID: {}, region: {}, province: {}, betType: {}, resultDate: {}, status: {}", 
                    bet.getId(), bet.getRegion(), bet.getProvince(), bet.getBetType(), bet.getResultDate(), bet.getStatus());
        }
        
        // Debug: Log target date format
        log.info("🔍 DEBUG Target date format: '{}', length: {}", targetDate, targetDate.length());
        
        if (pendingBets.isEmpty()) {
            log.info("✅ No pending bets to check for date: {}", targetDate);
            return;
        }

        int successCount = 0;
        int errorCount = 0;
        int skippedCount = 0;
        
        for (Bet bet : pendingBets) {
            try {
                log.info("⚡ Processing bet ID: {}, betType: {}, userId: {}, region: {}, province: {}", 
                        bet.getId(), bet.getBetType(), bet.getUser().getId(), bet.getRegion(), bet.getProvince());
                checkBetResult(bet);
                successCount++;
                log.info("✅ Successfully checked bet ID: {}", bet.getId());
            } catch (RuntimeException e) {
                // Nếu lỗi do chưa có kết quả xổ số thì skip (không đếm là lỗi)
                if (e.getMessage() != null && e.getMessage().contains("Chưa có kết quả xổ số")) {
                    skippedCount++;
                    log.warn("⏭️ Skipped bet ID {} - Chưa có kết quả xổ số: {}", bet.getId(), e.getMessage());
                } else {
                    errorCount++;
                    log.error("❌ Error checking result for bet ID {}: {}", bet.getId(), e.getMessage(), e);
                }
            } catch (Exception e) {
                errorCount++;
                log.error("❌ Unexpected error checking bet ID {}: {}", bet.getId(), e.getMessage(), e);
            }
        }
        
        log.info("========================================");
        log.info("📈 Bet check for date {} COMPLETED: ✅ {} successful, ⏭️ {} skipped (no result), ❌ {} errors out of {} total", 
                targetDate, successCount, skippedCount, errorCount, pendingBets.size());
        log.info("========================================");
    }

    /**
     * Kiểm tra kết quả cho 1 bet cụ thể
     * LOGIC CUỐI CÙNG: 
     * 1. Đặt cược: Trừ toàn bộ tiền cược
     * 2. Thắng cược: Cộng CHỈ tiền lãi của số trúng (thua mất luôn, không hoàn vốn)
     * 
     * REQUIRES_NEW: Mỗi bet chạy trong transaction riêng để tránh ảnh hưởng lẫn nhau
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void checkBetResult(Bet betParam) {
        // Fetch fresh bet from DB WITH user to avoid LazyInitializationException
        Bet bet = betRepository.findByIdWithUser(betParam.getId())
                .orElseThrow(() -> new RuntimeException("Bet không tồn tại: " + betParam.getId()));
        
        // Kiểm tra xem bet đã được check chưa (tránh check lại)
        if (bet.getStatus() != Bet.BetStatus.PENDING) {
            log.info("Bet {} already checked with status {}, skipping", bet.getId(), bet.getStatus());
            return;
        }
        
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

        // Lưu và flush ngay để đảm bảo data được persist
        bet = betRepository.save(bet);
        log.info("Bet {} saved with status: {}, isWin: {}, winAmount: {}", 
                bet.getId(), bet.getStatus(), bet.getIsWin(), bet.getWinAmount());
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
     * Hủy bet - CHO PHÉP HỦY TRƯỚC GIỜ KHÓA CƯỢC
     * Logic:
     * 1. Miền Bắc: Chỉ cho phép hủy trước 18:10
     * 2. Miền Trung: Chỉ cho phép hủy trước 17:00
     * 3. Miền Nam: Chỉ cho phép hủy trước 16:00
     * 4. Chỉ hủy được bet ở trạng thái PENDING
     * 5. Hoàn lại toàn bộ tiền cược
     */
    @Transactional
    public BetResponse cancelBet(Long betId, Long userId) {
        // 1. Tìm bet và kiểm tra quyền sở hữu
        Bet bet = betRepository.findByIdWithUser(betId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lệnh cược"));
        
        if (!bet.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy lệnh cược này");
        }
        
        // 2. Check thời gian cho phép hủy cược theo vùng miền
        // SAU GIỜ QUY ĐỊNH thì KHÔNG CHO HỦY
        checkCancelTimeLimit(bet.getRegion(), bet.getProvince());
        
        // 3. Kiểm tra trạng thái: chỉ hủy được PENDING
        if (bet.getStatus() != Bet.BetStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể hủy lệnh cược đang chờ kết quả. Trạng thái hiện tại: " + bet.getStatus());
        }
        
        // 4. Hoàn lại tiền cược
        User user = bet.getUser();
        BigDecimal refundAmount = bet.getTotalAmount();
        
        log.info("User {} cancelling bet ID {}. Refund amount: {} points", user.getUsername(), betId, refundAmount);
        
        // Cộng điểm hoàn lại vào tài khoản
        pointService.addPoints(user, refundAmount, 
            com.xsecret.entity.PointTransaction.PointTransactionType.BET_CANCELLED,
            "Hoàn tiền do hủy lệnh cược #" + betId,
            "BET_CANCEL", betId, null);
        
        // 5. Cập nhật trạng thái bet
        bet.setStatus(Bet.BetStatus.CANCELLED);
        bet.setResultCheckedAt(LocalDateTime.now());
        betRepository.save(bet);
        
        log.info("✅ Bet {} cancelled successfully. Refunded {} points to user {}", 
                betId, refundAmount, user.getUsername());
        
        return BetResponse.fromEntity(bet);
    }

    /**
     * Tự động hủy các bet expired (PENDING sau 20:00)
     * Chạy vào 20:00 mỗi ngày, hủy tất cả bets PENDING của hôm nay và hoàn tiền
     */
    @Transactional
    public int autoCancelExpiredBets() {
        String currentDate = getCurrentDateString();
        log.info("========================================");
        log.info("🚫 AUTO CANCEL EXPIRED BETS - Date: {}", currentDate);
        log.info("========================================");
        
        // Tìm tất cả bets PENDING của hôm nay
        List<Bet> pendingBets = betRepository.findPendingBetsToCheck(currentDate);
        
        if (pendingBets.isEmpty()) {
            log.info("✅ No expired bets to cancel for date: {}", currentDate);
            return 0;
        }
        
        log.info("📊 Found {} PENDING bets to cancel for date: {}", pendingBets.size(), currentDate);
        
        int cancelledCount = 0;
        
        for (Bet bet : pendingBets) {
            try {
                // Hoàn tiền
                User user = bet.getUser();
                BigDecimal refundAmount = bet.getTotalAmount();
                
                log.info("⚡ Auto cancelling expired bet ID: {}, userId: {}, refund: {} points", 
                        bet.getId(), user.getId(), refundAmount);
                
                // Cộng điểm hoàn lại vào tài khoản
                pointService.addPoints(user, refundAmount, 
                    com.xsecret.entity.PointTransaction.PointTransactionType.BET_CANCELLED,
                    "Hoàn tiền do lệnh cược hết hạn (chưa có kết quả sau 20:00) #" + bet.getId(),
                    "BET_EXPIRED", bet.getId(), null);
                
                // Cập nhật trạng thái bet
                bet.setStatus(Bet.BetStatus.CANCELLED);
                bet.setResultCheckedAt(LocalDateTime.now());
                betRepository.save(bet);
                
                cancelledCount++;
                log.info("✅ Bet {} auto cancelled. Refunded {} points to user {}", 
                        bet.getId(), refundAmount, user.getUsername());
                
            } catch (Exception e) {
                log.error("❌ Error auto cancelling bet {}: {}", bet.getId(), e.getMessage(), e);
            }
        }
        
        log.info("========================================");
        log.info("📈 Auto cancel COMPLETED: {} bets cancelled out of {} total", 
                cancelledCount, pendingBets.size());
        log.info("========================================");
        
        return cancelledCount;
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
     * Xác định ngày kết quả cho bet dựa trên thời gian đặt cược và lịch quay
     * - Miền Bắc: Sau 18:30 → ngày mai (quay hàng ngày)
     * - Miền Trung/Nam: Tính động dựa trên lịch quay của tỉnh
     */
    private String getBetResultDate(String region, String province) {
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();
        
        if ("mienBac".equals(region)) {
            // Miền Bắc: Logic cũ (quay hàng ngày)
            boolean shouldUseNextDay = now.isAfter(LocalTime.of(18, 30));
            LocalDate resultDate = shouldUseNextDay ? today.plusDays(1) : today;
            log.info("📅 Miền Bắc bet result date: {}", resultDate);
            return resultDate.toString();
        }
        
        // Miền Trung Nam: Logic ĐỘNG dựa trên lịch tỉnh
        if ("mienTrungNam".equals(region) && province != null) {
            log.info("🔍 [DEBUG] Calculating result date for province: {}", province);
            
            // 1. Check hôm nay có phải ngày quay của tỉnh không?
            boolean isTodayDrawDay = isProvinceDrawDay(province, today);
            log.info("📅 [DEBUG] Is today draw day for {}: {}", province, isTodayDrawDay);
            
            if (!isTodayDrawDay) {
                // Hôm nay không quay → resultDate = ngày quay tiếp theo
                LocalDate nextDrawDate = getNextProvinceDrawDate(province, today);
                log.info("📅 [DEBUG] {} không quay hôm nay. Result date = {} (ngày quay tiếp theo)", 
                        province, nextDrawDate);
                return nextDrawDate.toString();
            }
            
            // 2. Hôm nay có quay → Check giờ
            LocalTime lockEnd = LocalTime.of(18, 45);
            
            if (now.isBefore(lockEnd)) {
                // Trước 18:45 → Đánh cho hôm nay
                log.info("📅 [DEBUG] {} quay hôm nay và chưa qua 18:45. Result date = {}", province, today);
                return today.toString();
            } else {
                // Sau 18:45 → Đánh cho kỳ tiếp theo
                LocalDate nextDrawDate = getNextProvinceDrawDate(province, today);
                log.info("📅 [DEBUG] {} đã qua 18:45. Result date = {} (kỳ tiếp theo)", 
                        province, nextDrawDate);
                return nextDrawDate.toString();
            }
        }
        
        // Fallback
        log.warn("⚠️ Không xác định được result date, dùng hôm nay: {}", today);
        return today.toString();
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
     * DISABLED: Chỉ cho phép check bet lúc 18:30 theo lịch trình
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
    
    /**
     * Kiểm tra thời gian khóa cược ĐỘNG theo lịch tỉnh
     * - Miền Bắc: Khóa cố định từ 18:10 đến 18:45 (quay hàng ngày)
     * - Miền Trung/Nam: CHỈ khóa giờ nếu HÔM NAY là ngày quay của tỉnh đó
     *   + Miền Trung: 17:00 - 18:45
     *   + Miền Nam: 16:00 - 18:45
     */
    private void checkBettingTimeLimit(String region, String province) {
        LocalTime now = LocalTime.now();
        
        if ("mienBac".equals(region)) {
            // Miền Bắc: Khóa cố định từ 18:10 đến 18:45 (quay hàng ngày)
            if (isInLockTimeRange(now, 18, 10, 18, 45)) {
                throw new RuntimeException("Miền Bắc đang khóa cược từ 18:10 đến 18:45. Vui lòng đợi đến 18:45.");
            }
        } else if ("mienTrungNam".equals(region) && province != null) {
            // Miền Trung Nam: Kiểm tra ĐỘNG theo lịch tỉnh
            
            // 1. Check hôm nay có phải ngày quay của tỉnh này không?
            boolean isTodayDrawDay = isProvinceDrawDay(province, LocalDate.now());
            log.info("🔍 [DEBUG] Checking betting time lock for province: {}", province);
            log.info("📅 [DEBUG] Is today draw day for {}: {}", province, isTodayDrawDay);
            
            if (!isTodayDrawDay) {
                // Hôm nay không quay → KHÔNG khóa giờ
                log.info("✅ [DEBUG] Province {} không quay hôm nay, cho phép đặt cược mọi giờ", province);
                return;
            }
            
            // 2. Hôm nay là ngày quay → Áp dụng khóa giờ
            boolean isMienTrungProvince = isMienTrung(province);
            LocalTime lockStart = isMienTrungProvince ? LocalTime.of(17, 0) : LocalTime.of(16, 0);
            LocalTime lockEnd = LocalTime.of(18, 45);
            
            log.info("⏰ [DEBUG] Province {} is draw day today. Lock time: {} - {} (isMienTrung: {})", 
                province, lockStart, lockEnd, isMienTrungProvince);
            log.info("🕐 [DEBUG] Current time: {}, isInLockRange: {}", now, 
                isInLockTimeRange(now, lockStart.getHour(), lockStart.getMinute(), lockEnd.getHour(), lockEnd.getMinute()));
            
            if (isInLockTimeRange(now, lockStart.getHour(), lockStart.getMinute(), lockEnd.getHour(), lockEnd.getMinute())) {
                String regionName = isMienTrungProvince ? "Miền Trung" : "Miền Nam";
                throw new RuntimeException(String.format(
                    "%s (%s) đang khóa cược từ %s đến 18:45 (hôm nay là ngày quay). Vui lòng đợi đến 18:45.",
                    regionName, province, lockStart
                ));
            }
        }
    }
    
    /**
     * Kiểm tra thời gian cho phép HỦY CƯỢC theo vùng miền
     * SAU GIỜ QUY ĐỊNH thì KHÔNG CHO HỦY các lệnh đã đặt trước đó
     * - Miền Bắc: Không cho hủy từ 18:10 trở đi
     * - Miền Trung: Không cho hủy từ 17:00 trở đi
     * - Miền Nam: Không cho hủy từ 16:00 trở đi
     */
    private void checkCancelTimeLimit(String region, String province) {
        LocalTime now = LocalTime.now();
        
        if ("mienBac".equals(region)) {
            // Miền Bắc: Không cho hủy từ 18:10 trở đi (bao gồm cả 18:10)
            if (!now.isBefore(LocalTime.of(18, 10))) {
                throw new RuntimeException("Đã quá giờ cho phép hủy cược Miền Bắc (18:10). Vui lòng liên hệ admin nếu cần hỗ trợ.");
            }
        } else if ("mienTrungNam".equals(region)) {
            // Kiểm tra tỉnh để xác định Miền Trung hay Miền Nam
            if (isMienTrung(province)) {
                // Miền Trung: Không cho hủy từ 17:00 trở đi (bao gồm cả 17:00)
                if (!now.isBefore(LocalTime.of(17, 0))) {
                    throw new RuntimeException("Đã quá giờ cho phép hủy cược Miền Trung (17:00). Vui lòng liên hệ admin nếu cần hỗ trợ.");
                }
            } else {
                // Miền Nam: Không cho hủy từ 16:00 trở đi (bao gồm cả 16:00)
                if (!now.isBefore(LocalTime.of(16, 0))) {
                    throw new RuntimeException("Đã quá giờ cho phép hủy cược Miền Nam (16:00). Vui lòng liên hệ admin nếu cần hỗ trợ.");
                }
            }
        }
    }
    
    /**
     * Kiểm tra xem thời gian hiện tại có nằm trong khoảng khóa cược không
     */
    private boolean isInLockTimeRange(LocalTime now, int startHour, int startMinute, int endHour, int endMinute) {
        LocalTime startTime = LocalTime.of(startHour, startMinute);
        LocalTime endTime = LocalTime.of(endHour, endMinute);
        
        // Nếu khoảng thời gian không vượt qua nửa đêm (VD: 18:10 - 18:45)
        if (startTime.isBefore(endTime)) {
            // Khóa từ startTime đến endTime (bao gồm cả startTime, không bao gồm endTime)
            return !now.isBefore(startTime) && now.isBefore(endTime);
        } 
        // Nếu khoảng thời gian vượt qua nửa đêm (VD: 23:30 - 00:30) - trường hợp này không áp dụng
        else {
            return !now.isBefore(startTime) || !now.isAfter(endTime);
        }
    }
    
    /**
     * Kiểm tra xem tỉnh có thuộc Miền Trung không
     * Dựa trên danh sách đầy đủ 31 tỉnh
     */
    private boolean isMienTrung(String province) {
        if (province == null) {
            return false;
        }
        
        List<String> mienTrungProvinces = Arrays.asList(
            "phuyen", "thuathienhue", "daklak", "quangnam", "danang",
            "khanhhoa", "binhdinh", "quangbinh", "quangtri",
            "gialai", "ninhthuan", "daknong", "quangngai", "kontum"
        );
        
        return mienTrungProvinces.contains(province.toLowerCase());
    }
    
    /**
     * Kiểm tra ngày cụ thể có phải ngày quay của tỉnh không
     * @param province Tên tỉnh (backend format: lowercase)
     * @param date Ngày cần check
     * @return true nếu ngày đó tỉnh có quay
     */
    private boolean isProvinceDrawDay(String province, LocalDate date) {
        if (province == null) {
            return false;
        }
        
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<String> provincesThatDay = getProvincesForDayOfWeek(dayOfWeek);
        
        return provincesThatDay.contains(province.toLowerCase());
    }
    
    /**
     * Tìm ngày quay TIẾP THEO của tỉnh (từ ngày chỉ định)
     * @param province Tên tỉnh
     * @param fromDate Ngày bắt đầu tìm
     * @return Ngày quay tiếp theo
     */
    private LocalDate getNextProvinceDrawDate(String province, LocalDate fromDate) {
        if (province == null) {
            return fromDate.plusDays(1);
        }
        
        // Tìm tất cả các ngày trong tuần mà tỉnh này quay
        List<java.time.DayOfWeek> drawDays = new ArrayList<>();
        for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
            List<String> provinces = getProvincesForDayOfWeek(day);
            if (provinces.contains(province.toLowerCase())) {
                drawDays.add(day);
            }
        }
        
        if (drawDays.isEmpty()) {
            // Nếu không tìm thấy lịch, mặc định là ngày mai
            log.warn("⚠️ Không tìm thấy lịch quay cho province: {}", province);
            return fromDate.plusDays(1);
        }
        
        // Tìm ngày quay gần nhất trong tương lai (từ ngày mai trở đi)
        LocalDate currentDate = fromDate.plusDays(1);
        for (int i = 0; i < 7; i++) {
            if (drawDays.contains(currentDate.getDayOfWeek())) {
                return currentDate;
            }
            currentDate = currentDate.plusDays(1);
        }
        
        // Fallback: nếu không tìm được trong 7 ngày thì trả về 7 ngày sau
        return fromDate.plusDays(7);
    }
    
    /**
     * Lấy danh sách tỉnh quay theo ngày trong tuần
     * GIỐNG HỆT LotteryResultAutoImportService để đồng bộ
     */
    private List<String> getProvincesForDayOfWeek(java.time.DayOfWeek dayOfWeek) {
        Map<DayOfWeek, List<String>> schedule = new java.util.HashMap<>();
        
        // Thứ 2
        schedule.put(java.time.DayOfWeek.MONDAY, Arrays.asList(
            "phuyen", "thuathienhue", "camau", "dongthap", "hcm"
        ));
        
        // Thứ 3
        schedule.put(java.time.DayOfWeek.TUESDAY, Arrays.asList(
            "daklak", "quangnam", "baclieu", "bentre", "vungtau"
        ));
        
        // Thứ 4
        schedule.put(java.time.DayOfWeek.WEDNESDAY, Arrays.asList(
            "danang", "khanhhoa", "cantho", "dongnai", "soctrang"
        ));
        
        // Thứ 5
        schedule.put(java.time.DayOfWeek.THURSDAY, Arrays.asList(
            "binhdinh", "quangbinh", "quangtri", "angiang", "binhthuan", "tayninh"
        ));
        
        // Thứ 6
        schedule.put(java.time.DayOfWeek.FRIDAY, Arrays.asList(
            "gialai", "ninhthuan", "binhduong", "travinh", "vinhlong"
        ));
        
        // Thứ 7
        schedule.put(java.time.DayOfWeek.SATURDAY, Arrays.asList(
            "danang", "daknong", "quangngai", "binhphuoc", "haugiang", "hcm", "longan"
        ));
        
        // Chủ Nhật
        schedule.put(java.time.DayOfWeek.SUNDAY, Arrays.asList(
            "khanhhoa", "kontum", "thuathienhue", "dalat", "kiengiang", "tiengiang"
        ));
        
        return schedule.getOrDefault(dayOfWeek, new ArrayList<>());
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
     * Admin: Cập nhật số đã chọn của bet
     * CHỈ cho phép cập nhật khi bet chưa có kết quả (PENDING)
     */
    @Transactional
    public BetResponse updateBetSelectedNumbers(Long betId, List<String> newSelectedNumbers) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new RuntimeException("Bet không tồn tại với ID: " + betId));
        
        // QUAN TRỌNG: Chỉ cho phép cập nhật khi bet chưa có kết quả
        if (bet.getStatus() != Bet.BetStatus.PENDING) {
            throw new RuntimeException("Không thể cập nhật bet đã có kết quả. Chỉ có thể cập nhật bet có status = PENDING");
        }
        
        // Validate số đã chọn mới
        if (newSelectedNumbers == null || newSelectedNumbers.isEmpty()) {
            throw new RuntimeException("Danh sách số đã chọn không được trống");
        }
        
        // Cập nhật số đã chọn
        String oldSelectedNumbers = bet.getSelectedNumbers();
        bet.setSelectedNumbers(convertToJsonString(newSelectedNumbers));
        
        log.info("Admin updating bet {} selected numbers from {} to {}", 
                betId, oldSelectedNumbers, convertToJsonString(newSelectedNumbers));
        
        // Lưu bet
        bet = betRepository.save(bet);
        log.info("Bet {} selected numbers updated successfully", betId);
        
        return BetResponse.fromEntity(bet);
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


