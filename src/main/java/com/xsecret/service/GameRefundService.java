package com.xsecret.service;

import com.xsecret.entity.GameRefundAccrual;
import com.xsecret.entity.Notification;
import com.xsecret.entity.PointTransaction;
import com.xsecret.entity.SystemSettings;
import com.xsecret.entity.User;
import com.xsecret.repository.GameRefundAccrualRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameRefundService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String DEFAULT_PAYOUT_TIME = "12:00";

    private final GameRefundAccrualRepository accrualRepository;
    private final SystemSettingsService systemSettingsService;
    private final PointService pointService;
    private final NotificationService notificationService;

    @Transactional
    public void accrueRefund(
            User user,
            GameRefundAccrual.GameType gameType,
            BigDecimal rawAmount,
            String description,
            Long referenceSessionId
    ) {
        if (user == null || rawAmount == null) {
            return;
        }

        BigDecimal amount = normalizePoints(rawAmount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Instant payoutAt = resolveNextPayoutInstant(gameType);

        GameRefundAccrual accrual = GameRefundAccrual.builder()
                .user(user)
                .gameType(gameType)
                .amount(amount)
                .description(description)
                .status(GameRefundAccrual.Status.PENDING)
                .payoutAt(payoutAt)
                .accruedAt(Instant.now())
                .referenceSessionId(referenceSessionId)
                .build();

        accrualRepository.save(accrual);
    }

    @Transactional
    public void processDueRefunds() {
        Instant now = Instant.now();
        List<GameRefundAccrual> batch;
        do {
            batch = accrualRepository.findTop500ByStatusAndPayoutAtLessThanEqualOrderByPayoutAtAsc(
                    GameRefundAccrual.Status.PENDING,
                    now
            );

            if (batch.isEmpty()) {
                break;
            }

            for (GameRefundAccrual accrual : batch) {
                try {
                    String referenceType = accrual.getGameType() == GameRefundAccrual.GameType.SICBO
                            ? "SICBO_CASHBACK"
                            : "XOC_DIA_CASHBACK";

                    pointService.addPoints(
                            accrual.getUser(),
                            accrual.getAmount(),
                            PointTransaction.PointTransactionType.BET_REFUND,
                            buildTransactionDescription(accrual),
                            referenceType,
                            accrual.getReferenceSessionId(),
                            null
                    );

                    accrual.setStatus(GameRefundAccrual.Status.PAID);
                    accrual.setPaidAt(Instant.now());
                    accrual.setFailureReason(null);
                    accrualRepository.save(accrual);
                    notifyRefundPaid(accrual);
                } catch (Exception ex) {
                    accrual.setStatus(GameRefundAccrual.Status.FAILED);
                    accrual.setFailureReason(ex.getMessage());
                    accrualRepository.save(accrual);
                    log.error("Failed to process game refund accrual {}: {}", accrual.getId(), ex.getMessage(), ex);
                }
            }
        } while (batch.size() == 500);
    }

    private Instant resolveNextPayoutInstant(GameRefundAccrual.GameType gameType) {
        String key = gameType == GameRefundAccrual.GameType.SICBO
                ? SystemSettings.SICBO_REFUND_PAYOUT_TIME
                : SystemSettings.XOC_DIA_REFUND_PAYOUT_TIME;

        LocalTime payoutTime = systemSettingsService.getTimeSetting(key, DEFAULT_PAYOUT_TIME);
        LocalDate today = LocalDate.now(VN_ZONE);
        LocalDateTime todayRun = LocalDateTime.of(today, payoutTime);
        Instant candidate = todayRun.atZone(VN_ZONE).toInstant();
        Instant now = Instant.now();

        if (now.isBefore(candidate)) {
            return candidate;
        }

        LocalDateTime nextRun = todayRun.plusDays(1);
        return nextRun.atZone(VN_ZONE).toInstant();
    }

    private BigDecimal normalizePoints(BigDecimal value) {
        BigDecimal sanitized = value.setScale(0, RoundingMode.DOWN);
        if (sanitized.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return sanitized;
    }

    private String buildTransactionDescription(GameRefundAccrual accrual) {
        StringBuilder builder = new StringBuilder("Hoàn trả ");
        builder.append(accrual.getGameType() == GameRefundAccrual.GameType.SICBO ? "Sicbo" : "Xóc Đĩa");
        builder.append(" theo lịch");

        if (StringUtils.hasText(accrual.getDescription())) {
            builder.append(" - ").append(accrual.getDescription());
        }
        return builder.toString();
    }

    private void notifyRefundPaid(GameRefundAccrual accrual) {
        User user = accrual.getUser();
        if (user == null || accrual.getAmount() == null) {
            return;
        }
        String gameLabel = accrual.getGameType() == GameRefundAccrual.GameType.SICBO ? "Sicbo" : "Xóc Đĩa";
        StringBuilder message = new StringBuilder()
                .append("Bạn vừa nhận hoàn trả ")
                .append(formatPoints(accrual.getAmount()))
                .append(" điểm từ ")
                .append(gameLabel);
        if (accrual.getReferenceSessionId() != null) {
            message.append(" phiên #").append(accrual.getReferenceSessionId());
        }
        if (StringUtils.hasText(accrual.getDescription())) {
            message.append(" - ").append(accrual.getDescription());
        }

        notificationService.createSystemNotificationForUser(
                user,
                "Hoàn trả " + gameLabel,
                message.toString(),
                Notification.NotificationPriority.INFO,
                Notification.NotificationType.TRANSACTION
        );
    }

    private String formatPoints(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        return formatter.format(amount.setScale(0, RoundingMode.DOWN));
    }
}


