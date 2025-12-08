package com.xsecret.service;

import com.xsecret.entity.DailyLossRefund;
import com.xsecret.entity.Notification;
import com.xsecret.entity.PointTransaction;
import com.xsecret.entity.SystemSettings;
import com.xsecret.entity.User;
import com.xsecret.repository.DailyLossRefundRepository;
import com.xsecret.repository.SicboBetRepository;
import com.xsecret.repository.UserRepository;
import com.xsecret.repository.XocDiaBetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.text.NumberFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyLossRefundService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String DEFAULT_PAYOUT_TIME = "00:00";

    private final DailyLossRefundRepository dailyLossRefundRepository;
    private final SicboBetRepository sicboBetRepository;
    private final XocDiaBetRepository xocDiaBetRepository;
    private final UserRepository userRepository;
    private final SystemSettingsService systemSettingsService;
    private final PointService pointService;
    private final NotificationService notificationService;

    /**
     * Tính toán và tạo daily loss refund cho một ngày cụ thể
     * Chạy vào cuối ngày để tính toán cho ngày đó
     */
    @Transactional
    public void calculateAndCreateDailyLossRefunds(LocalDate targetDate) {
        // Kiểm tra xem tính năng có được bật không
        String enabledStr = systemSettingsService.getSettingValue(
                SystemSettings.DAILY_LOSS_REFUND_ENABLED, "false");
        boolean enabled = "true".equalsIgnoreCase(enabledStr) || "1".equals(enabledStr);
        if (!enabled) {
            log.info("Daily loss refund is disabled, skipping calculation for date: {}", targetDate);
            return;
        }

        BigDecimal refundPercentage = systemSettingsService.getGameRefundPercentage(
                SystemSettings.DAILY_LOSS_REFUND_PERCENTAGE);
        if (refundPercentage.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Daily loss refund percentage is 0 or not set, skipping calculation for date: {}", targetDate);
            return;
        }

        // Tính thời gian payout
        Instant payoutAt = resolvePayoutInstant(targetDate);

        // Lấy tất cả users
        List<User> allUsers = userRepository.findAll();
        log.info("Calculating daily loss refunds for {} users on date: {}", allUsers.size(), targetDate);

        int processedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        for (User user : allUsers) {
            try {
                // Kiểm tra xem đã tính cho user này trong ngày này chưa
                Optional<DailyLossRefund> existing = dailyLossRefundRepository.findByUserAndRefundDate(user, targetDate);
                if (existing.isPresent()) {
                    log.debug("Daily loss refund already exists for user {} on date {}, skipping", user.getId(), targetDate);
                    skippedCount++;
                    continue;
                }

                // Tính tổng thắng/thua trong ngày (00:00 - 23:59)
                Instant startOfDay = targetDate.atStartOfDay().atZone(VN_ZONE).toInstant();
                Instant endOfDay = targetDate.atTime(LocalTime.MAX).atZone(VN_ZONE).toInstant();

                // Tính tổng thắng/thua từ Sicbo
                // Tổng tiền thắng: chỉ tính winAmount (không trừ stake)
                BigDecimal sicboWinAmount = sicboBetRepository.sumWinAmountByUserAndDateRange(user, startOfDay, endOfDay);
                if (sicboWinAmount == null) sicboWinAmount = BigDecimal.ZERO;

                // Tổng tiền thua: chỉ tính stake khi thua
                BigDecimal sicboLoss = sicboBetRepository.sumLostStakeByUserAndDateRange(user, startOfDay, endOfDay);
                if (sicboLoss == null) sicboLoss = BigDecimal.ZERO;

                // Tính tổng thắng/thua từ XocDia
                // Tổng tiền thắng: chỉ tính winAmount (không trừ stake)
                BigDecimal xocDiaWinAmount = xocDiaBetRepository.sumWinAmountByUserAndDateRange(user, startOfDay, endOfDay);
                if (xocDiaWinAmount == null) xocDiaWinAmount = BigDecimal.ZERO;

                // Tổng tiền thua: chỉ tính stake khi thua
                BigDecimal xocDiaLoss = xocDiaBetRepository.sumLostStakeByUserAndDateRange(user, startOfDay, endOfDay);
                if (xocDiaLoss == null) xocDiaLoss = BigDecimal.ZERO;

                // Tổng hợp
                BigDecimal totalWin = sicboWinAmount.add(xocDiaWinAmount);
                BigDecimal totalLoss = sicboLoss.add(xocDiaLoss);

                // Tính net loss = totalWin - totalLoss
                BigDecimal netLoss = totalWin.subtract(totalLoss);

                // Tính refund amount
                BigDecimal refundAmount = BigDecimal.ZERO;
                DailyLossRefund.Status status = DailyLossRefund.Status.SKIPPED;
                String description = "";

                if (netLoss.compareTo(BigDecimal.ZERO) < 0) {
                    // Nếu netLoss < 0 (thua nhiều hơn thắng), tính hoàn trả
                    BigDecimal absNetLoss = netLoss.abs();
                    refundAmount = absNetLoss.multiply(refundPercentage)
                            .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
                    
                    if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                        status = DailyLossRefund.Status.PENDING;
                        description = String.format("Hoàn trả tổng thua theo ngày %s. Tổng thắng: %s, Tổng thua: %s, Lỗ ròng: %s",
                                targetDate, formatPoints(totalWin), formatPoints(totalLoss), formatPoints(absNetLoss));
                    } else {
                        description = String.format("Tổng thắng: %s, Tổng thua: %s, Lỗ ròng: %s (không đủ để hoàn trả)",
                                formatPoints(totalWin), formatPoints(totalLoss), formatPoints(absNetLoss));
                    }
                } else {
                    // Nếu netLoss >= 0 (thắng nhiều hơn hoặc bằng thua), không hoàn trả
                    description = String.format("Tổng thắng: %s, Tổng thua: %s, Lãi ròng: %s (không hoàn trả)",
                            formatPoints(totalWin), formatPoints(totalLoss), formatPoints(netLoss));
                }

                // Tạo record
                DailyLossRefund refund = DailyLossRefund.builder()
                        .user(user)
                        .refundDate(targetDate)
                        .totalWinAmount(totalWin)
                        .totalLossAmount(totalLoss)
                        .netLossAmount(netLoss)
                        .refundPercentage(refundPercentage)
                        .refundAmount(refundAmount)
                        .status(status)
                        .payoutAt(payoutAt)
                        .description(description)
                        .build();

                dailyLossRefundRepository.save(refund);
                processedCount++;

                if (status == DailyLossRefund.Status.PENDING) {
                    log.info("Created daily loss refund for user {} on date {}: refund amount = {}", 
                            user.getId(), targetDate, refundAmount);
                } else {
                    log.debug("Skipped daily loss refund for user {} on date {}: {}", 
                            user.getId(), targetDate, description);
                }

            } catch (Exception ex) {
                log.error("Error calculating daily loss refund for user {} on date {}: {}", 
                        user.getId(), targetDate, ex.getMessage(), ex);
                errorCount++;
            }
        }

        log.info("Daily loss refund calculation completed for date {}: processed={}, skipped={}, errors={}", 
                targetDate, processedCount, skippedCount, errorCount);
    }

    /**
     * Xử lý hoàn trả các daily loss refund đã đến hạn
     */
    @Transactional
    public void processDueDailyLossRefunds() {
        Instant now = Instant.now();
        List<DailyLossRefund> batch;
        int batchSize = 500;
        
        do {
            batch = dailyLossRefundRepository.findTop500ByStatusAndPayoutAtLessThanEqualOrderByPayoutAtAsc(
                    DailyLossRefund.Status.PENDING,
                    now
            );

            if (batch.isEmpty()) {
                break;
            }

            for (DailyLossRefund refund : batch) {
                try {
                    if (refund.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        refund.setStatus(DailyLossRefund.Status.SKIPPED);
                        refund.setDescription(refund.getDescription() + " (không có số tiền để hoàn)");
                        dailyLossRefundRepository.save(refund);
                        continue;
                    }

                    // Thêm điểm vào tài khoản
                    pointService.addPoints(
                            refund.getUser(),
                            refund.getRefundAmount(),
                            PointTransaction.PointTransactionType.BET_REFUND,
                            buildTransactionDescription(refund),
                            "DAILY_LOSS_REFUND",
                            null,
                            null
                    );

                    refund.setStatus(DailyLossRefund.Status.PAID);
                    refund.setPaidAt(Instant.now());
                    refund.setFailureReason(null);
                    dailyLossRefundRepository.save(refund);
                    
                    notifyRefundPaid(refund);
                    
                    log.info("Processed daily loss refund {} for user {}: amount = {}", 
                            refund.getId(), refund.getUser().getId(), refund.getRefundAmount());

                } catch (Exception ex) {
                    refund.setStatus(DailyLossRefund.Status.FAILED);
                    refund.setFailureReason(ex.getMessage());
                    dailyLossRefundRepository.save(refund);
                    log.error("Failed to process daily loss refund {}: {}", refund.getId(), ex.getMessage(), ex);
                }
            }
        } while (batch.size() == batchSize);
    }

    private Instant resolvePayoutInstant(LocalDate targetDate) {
        String payoutTimeStr = systemSettingsService.getSettingValue(
                SystemSettings.DAILY_LOSS_REFUND_PAYOUT_TIME, DEFAULT_PAYOUT_TIME);
        
        LocalTime payoutTime = parseTime(payoutTimeStr);
        LocalDateTime payoutDateTime = targetDate.plusDays(1).atTime(payoutTime); // Hoàn trả vào ngày hôm sau
        return payoutDateTime.atZone(VN_ZONE).toInstant();
    }

    private LocalTime parseTime(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return LocalTime.of(hour, minute);
        } catch (Exception ex) {
            log.warn("Invalid time format: {}, using default: {}", timeStr, DEFAULT_PAYOUT_TIME);
            String[] parts = DEFAULT_PAYOUT_TIME.split(":");
            return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }

    private String buildTransactionDescription(DailyLossRefund refund) {
        return String.format("Hoàn tổng thua theo ngày %s - %s", 
                refund.getRefundDate(), refund.getDescription());
    }

    private void notifyRefundPaid(DailyLossRefund refund) {
        User user = refund.getUser();
        if (user == null || refund.getRefundAmount() == null) {
            return;
        }

        String message = String.format(
                "Bạn vừa nhận hoàn trả tổng thua theo ngày %s: %s điểm. Tổng thắng: %s, Tổng thua: %s",
                refund.getRefundDate(),
                formatPoints(refund.getRefundAmount()),
                formatPoints(refund.getTotalWinAmount()),
                formatPoints(refund.getTotalLossAmount())
        );

        notificationService.createSystemNotificationForUser(
                user,
                "Hoàn tổng thua theo ngày",
                message,
                Notification.NotificationPriority.INFO,
                Notification.NotificationType.TRANSACTION
        );
    }

    private String formatPoints(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return formatter.format(amount.setScale(0, RoundingMode.DOWN));
    }
}

