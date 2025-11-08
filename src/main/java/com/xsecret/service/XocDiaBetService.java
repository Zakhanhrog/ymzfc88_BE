package com.xsecret.service;

import com.xsecret.dto.request.XocDiaBetRequest;
import com.xsecret.dto.response.XocDiaBetPlacementResponse;
import com.xsecret.entity.PointTransaction;
import com.xsecret.entity.User;
import com.xsecret.entity.XocDiaBet;
import com.xsecret.entity.XocDiaQuickBetConfig;
import com.xsecret.entity.XocDiaSession;
import com.xsecret.repository.XocDiaBetRepository;
import com.xsecret.repository.XocDiaQuickBetConfigRepository;
import com.xsecret.repository.XocDiaSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class XocDiaBetService {

    private static final Set<XocDiaSession.Phase> LOCKED_PHASES = EnumSet.of(
            XocDiaSession.Phase.BETTING_CLOSED,
            XocDiaSession.Phase.WAITING_RESULT,
            XocDiaSession.Phase.SHOW_RESULT,
            XocDiaSession.Phase.PAYOUT,
            XocDiaSession.Phase.INVITE_BET
    );

    private static final Map<String, Set<String>> RESULT_CODE_TO_WINNERS = Map.of(
            "four-white", Set.of("four-white", "four-white-or-four-red"),
            "four-red", Set.of("four-red", "four-white-or-four-red"),
            "three-white-one-red", Set.of("three-white-one-red"),
            "three-red-one-white", Set.of("three-red-one-white"),
            "two-two", Set.of("two-two")
    );

    private final XocDiaBetRepository betRepository;
    private final XocDiaSessionRepository sessionRepository;
    private final XocDiaQuickBetConfigRepository quickBetConfigRepository;
    private final PointService pointService;

    @Transactional
    public XocDiaBetPlacementResponse placeBets(User user, XocDiaBetRequest request) {
        if (request.getBets() == null || request.getBets().isEmpty()) {
            throw new IllegalArgumentException("Danh sách cược không được để trống");
        }

        XocDiaSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalStateException("Phiên Xóc Đĩa không tồn tại"));

        if (session.getStatus() != XocDiaSession.Status.RUNNING) {
            throw new IllegalStateException("Phiên Xóc Đĩa hiện không khả dụng để đặt cược");
        }

        if (session.getPhase() == null || LOCKED_PHASES.contains(session.getPhase())) {
            throw new IllegalStateException("Phiên đã ngưng cược, vui lòng chờ phiên tiếp theo");
        }

        Set<String> requestedCodes = request.getBets().stream()
                .map(item -> normalizeCode(item.getCode()))
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toSet());

        if (requestedCodes.isEmpty()) {
            throw new IllegalStateException("Không có mã cược hợp lệ");
        }

        List<XocDiaQuickBetConfig> configs = quickBetConfigRepository.findAllByCodeIn(requestedCodes);
        Map<String, XocDiaQuickBetConfig> configMap = configs.stream()
                .filter(config -> Boolean.TRUE.equals(config.getIsActive()))
                .collect(Collectors.toMap(
                        config -> normalizeCode(config.getCode()),
                        config -> config
                ));

        if (configMap.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy cấu hình cược hợp lệ");
        }

        BigDecimal totalStake = BigDecimal.ZERO;
        List<XocDiaBet> betsToSave = new ArrayList<>();
        List<XocDiaBetPlacementResponse.PlacedBetItem> placedItems = new ArrayList<>();

        for (XocDiaBetRequest.BetItem item : request.getBets()) {
            String normalizedCode = normalizeCode(item.getCode());
            if (!configMap.containsKey(normalizedCode)) {
                throw new IllegalStateException("Loại cược " + item.getCode() + " hiện không hỗ trợ");
            }

            XocDiaQuickBetConfig config = configMap.get(normalizedCode);
            if (!hasPattern(config.getPattern())) {
                throw new IllegalStateException("Loại cược " + config.getName() + " chưa khả dụng");
            }

            long amount = Optional.ofNullable(item.getAmount()).orElse(0L);
            if (amount <= 0) {
                throw new IllegalArgumentException("Giá trị cược phải lớn hơn 0");
            }

            BigDecimal stake = BigDecimal.valueOf(amount);
            totalStake = totalStake.add(stake);

            XocDiaBet bet = XocDiaBet.builder()
                    .user(user)
                    .session(session)
                    .betCode(normalizedCode)
                    .stake(stake)
                    .payoutMultiplier(config.getPayoutMultiplier())
                    .status(XocDiaBet.Status.PENDING)
                    .build();

            betsToSave.add(bet);
            placedItems.add(
                    XocDiaBetPlacementResponse.PlacedBetItem.builder()
                            .code(normalizedCode)
                            .amount(amount)
                            .displayName(config.getName())
                            .payoutMultiplier(config.getPayoutMultiplier().doubleValue())
                            .build()
            );
        }

        if (totalStake.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Không có giá trị cược hợp lệ");
        }

        long currentPoints = Optional.ofNullable(user.getPoints()).orElse(0L);
        if (BigDecimal.valueOf(currentPoints).compareTo(totalStake) < 0) {
            throw new IllegalStateException("Số điểm không đủ để đặt cược");
        }

        pointService.subtractPoints(
                user,
                totalStake,
                PointTransaction.PointTransactionType.BET_PLACED,
                "Đặt cược Xóc Đĩa phiên #" + session.getId(),
                "XOC_DIA",
                session.getId(),
                null
        );

        betRepository.saveAll(betsToSave);

        long balanceAfter = Optional.ofNullable(user.getPoints()).orElse(0L);

        return XocDiaBetPlacementResponse.builder()
                .sessionId(session.getId())
                .totalStake(totalStake.longValue())
                .balanceBefore(currentPoints)
                .balanceAfter(balanceAfter)
                .bets(placedItems)
                .build();
    }

    @Transactional
    public void settleBets(XocDiaSession session, String resultCode) {
        List<XocDiaBet> pendingBets = betRepository.findBySessionAndStatus(session, XocDiaBet.Status.PENDING);
        if (pendingBets.isEmpty()) {
            return;
        }

        Set<String> winningCodes = RESULT_CODE_TO_WINNERS.getOrDefault(
                normalizeCode(resultCode), Collections.emptySet()
        );

        Instant now = Instant.now();

        for (XocDiaBet bet : pendingBets) {
            bet.setResultCode(resultCode);
            bet.setSettledAt(now);

            if (winningCodes.contains(bet.getBetCode())) {
                BigDecimal winAmount = bet.getStake().multiply(bet.getPayoutMultiplier());
                pointService.addPoints(
                        bet.getUser(),
                        winAmount,
                        PointTransaction.PointTransactionType.BET_WIN,
                        "Thắng cược Xóc Đĩa phiên #" + session.getId() + " (" + bet.getBetCode() + ")",
                        "XOC_DIA",
                        session.getId(),
                        null
                );
                bet.setWinAmount(winAmount);
                bet.setStatus(XocDiaBet.Status.WON);
            } else {
                bet.setWinAmount(BigDecimal.ZERO);
                bet.setStatus(XocDiaBet.Status.LOST);
            }
        }

        betRepository.saveAll(pendingBets);
    }

    @Transactional
    public void refundUnsettledBets(XocDiaSession session, String reason) {
        List<XocDiaBet> pendingBets = betRepository.findBySessionAndStatus(session, XocDiaBet.Status.PENDING);
        if (pendingBets.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        for (XocDiaBet bet : pendingBets) {
            pointService.addPoints(
                    bet.getUser(),
                    bet.getStake(),
                    PointTransaction.PointTransactionType.BET_REFUND,
                    "Hoàn điểm do phiên Xóc Đĩa bị hủy (" + reason + ")",
                    "XOC_DIA",
                    session.getId(),
                    null
            );
            bet.setResultCode(reason);
            bet.setWinAmount(bet.getStake());
            bet.setSettledAt(now);
            bet.setStatus(XocDiaBet.Status.REFUNDED);
        }

        betRepository.saveAll(pendingBets);
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return "";
        }
        return code.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasPattern(String pattern) {
        return pattern != null && !pattern.trim().isEmpty();
    }
}


