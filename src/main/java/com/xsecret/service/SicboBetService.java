package com.xsecret.service;

import com.xsecret.dto.request.SicboBetRequest;
import com.xsecret.dto.response.SicboBetHistoryItemResponse;
import com.xsecret.dto.response.SicboBetHistoryPageResponse;
import com.xsecret.dto.response.SicboBetPlacementResponse;
import com.xsecret.entity.PointTransaction;
import com.xsecret.entity.SicboBet;
import com.xsecret.entity.SicboQuickBetConfig;
import com.xsecret.entity.SicboResultHistory;
import com.xsecret.entity.SicboSession;
import com.xsecret.entity.User;
import com.xsecret.repository.SicboBetRepository;
import com.xsecret.repository.SicboQuickBetConfigRepository;
import com.xsecret.repository.SicboSessionRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SicboBetService {

    private static final Set<SicboSession.Phase> LOCKED_PHASES = EnumSet.of(
            SicboSession.Phase.BETTING_CLOSED,
            SicboSession.Phase.WAITING_RESULT,
            SicboSession.Phase.SHOW_RESULT,
            SicboSession.Phase.PAYOUT,
            SicboSession.Phase.INVITE_BET
    );

    private static final Map<String, SicboQuickBetConfig> FALLBACK_CONFIGS;

    static {
        Map<String, SicboQuickBetConfig> fallback = new HashMap<>();

        fallback.put("sicbo_primary_small", createFallbackConfig("sicbo_primary_small", "Xỉu", 0.97, SicboQuickBetConfig.GROUP_PRIMARY, 0));
        fallback.put("sicbo_primary_big", createFallbackConfig("sicbo_primary_big", "Tài", 0.97, SicboQuickBetConfig.GROUP_PRIMARY, 1));

        for (int i = 1; i <= 6; i++) {
            fallback.put("sicbo_combo_triple_" + i,
                    createFallbackConfig("sicbo_combo_triple_" + i, "Bộ ba " + i, 20, SicboQuickBetConfig.GROUP_COMBINATION, i - 1));
            fallback.put("sicbo_single_" + i,
                    createFallbackConfig("sicbo_single_" + i, "Một mặt " + i, 0.97, SicboQuickBetConfig.GROUP_SINGLE, i - 1));
        }

        fallback.put("sicbo_parity_even", createFallbackConfig("sicbo_parity_even", "Chẵn", 0.97, SicboQuickBetConfig.GROUP_PARITY, 0));
        fallback.put("sicbo_parity_odd", createFallbackConfig("sicbo_parity_odd", "Lẻ", 0.97, SicboQuickBetConfig.GROUP_PARITY, 1));

        int[] topTotals = {4, 5, 6, 7, 8, 9, 10};
        double[] topMultipliers = {30, 18, 14, 12, 8, 6, 6};
        for (int index = 0; index < topTotals.length; index++) {
            int total = topTotals[index];
            fallback.put("sicbo_total_" + total,
                    createFallbackConfig("sicbo_total_" + total, "Tổng " + total, topMultipliers[index], SicboQuickBetConfig.GROUP_TOTAL_TOP, index + 1));
        }

        int[] bottomTotals = {17, 16, 15, 14, 13, 12, 11};
        double[] bottomMultipliers = {30, 18, 14, 12, 8, 6, 6};
        for (int index = 0; index < bottomTotals.length; index++) {
            int total = bottomTotals[index];
            fallback.put("sicbo_total_" + total,
                    createFallbackConfig("sicbo_total_" + total, "Tổng " + total, bottomMultipliers[index], SicboQuickBetConfig.GROUP_TOTAL_BOTTOM, index + 1));
        }

        FALLBACK_CONFIGS = Collections.unmodifiableMap(fallback);
    }

    private final SicboBetRepository betRepository;
    private final SicboSessionRepository sessionRepository;
    private final SicboQuickBetConfigRepository quickBetConfigRepository;
    private final PointService pointService;
    private final SicboResultHistoryService resultHistoryService;

    @Transactional
    public SicboBetPlacementResponse placeBets(User user, SicboBetRequest request) {
        if (request == null || CollectionUtils.isEmpty(request.getBets())) {
            throw new IllegalArgumentException("Danh sách cược không được để trống");
        }

        SicboSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalStateException("Phiên Sicbo không tồn tại"));

        if (session.getStatus() != SicboSession.Status.RUNNING) {
            throw new IllegalStateException("Phiên Sicbo hiện không khả dụng để đặt cược");
        }

        if (session.getTableNumber() != null && request.getTableNumber() != null
                && !session.getTableNumber().equals(request.getTableNumber())) {
            throw new IllegalStateException("Phiên Sicbo không thuộc bàn chơi đã chọn");
        }

        if (session.getPhase() == null || LOCKED_PHASES.contains(session.getPhase())) {
            throw new IllegalStateException("Phiên đã ngưng cược, vui lòng chờ phiên tiếp theo");
        }

        Set<String> requestedCodes = request.getBets().stream()
                .map(item -> normalizeCode(item.getCode()))
                .collect(Collectors.toSet());

        List<SicboQuickBetConfig> configs = quickBetConfigRepository.findAllByCodeIn(requestedCodes);
        Map<String, SicboQuickBetConfig> configMap = configs.stream()
                .collect(Collectors.toMap(
                        config -> normalizeCode(config.getCode()),
                        config -> config
                ));

        List<String> missingCodes = requestedCodes.stream()
                .filter(code -> !configMap.containsKey(code) && !FALLBACK_CONFIGS.containsKey(code))
                .toList();

        if (!missingCodes.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy cấu hình cược hợp lệ cho: " + String.join(", ", missingCodes));
        }

        BigDecimal totalStake = BigDecimal.ZERO;
        List<SicboBet> betsToSave = new ArrayList<>();
        List<SicboBetPlacementResponse.PlacedBetItem> placedItems = new ArrayList<>();

        for (SicboBetRequest.BetItem item : request.getBets()) {
            String normalizedCode = normalizeCode(item.getCode());
            SicboQuickBetConfig config = Optional.ofNullable(configMap.get(normalizedCode))
                    .orElse(FALLBACK_CONFIGS.get(normalizedCode));

            if (config == null) {
                throw new IllegalStateException("Loại cược " + item.getCode() + " hiện không hỗ trợ");
            }

            long amount = Optional.ofNullable(item.getAmount()).orElse(0L);
            if (amount <= 0) {
                throw new IllegalArgumentException("Giá trị cược phải lớn hơn 0");
            }

            BigDecimal stake = BigDecimal.valueOf(amount);
            totalStake = totalStake.add(stake);

            SicboBet bet = SicboBet.builder()
                    .user(user)
                    .session(session)
                    .betCode(normalizedCode)
                    .stake(stake)
                    .payoutMultiplier(config.getPayoutMultiplier())
                    .status(SicboBet.Status.PENDING)
                    .build();

            betsToSave.add(bet);
            placedItems.add(
                    SicboBetPlacementResponse.PlacedBetItem.builder()
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

        long balanceBefore = Optional.ofNullable(user.getPoints()).orElse(0L);
        if (BigDecimal.valueOf(balanceBefore).compareTo(totalStake) < 0) {
            throw new IllegalStateException("Số điểm không đủ, vui lòng nạp thêm để đặt cược");
        }

        pointService.subtractPoints(
                user,
                totalStake,
                PointTransaction.PointTransactionType.BET_PLACED,
                String.format("Đặt cược Sicbo bàn %d phiên #%d", session.getTableNumber(), session.getId()),
                "SICBO",
                session.getId(),
                null
        );

        betRepository.saveAll(betsToSave);

        long balanceAfter = Optional.ofNullable(user.getPoints()).orElse(0L);

        return SicboBetPlacementResponse.builder()
                .sessionId(session.getId())
                .tableNumber(session.getTableNumber())
                .totalStake(totalStake.longValue())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .bets(placedItems)
                .build();
    }

    @Transactional
    public void settleBets(SicboSession session, String resultCode) {
        Instant now = Instant.now();
        Set<String> winningCodes = resolveWinningCodes(resultCode);
        int resultSum = 0;
        boolean isTripleSum = false;
        Integer tripleFace = null;
        SicboResultHistory.Category historyCategory = null;
        List<Integer> faces = parseFaces(resultCode);
        if (faces.size() == 3) {
            resultSum = faces.stream().mapToInt(Integer::intValue).sum();
            isTripleSum = (resultSum == 3 || resultSum == 18);
            Map<Integer, Long> counts = faces.stream()
                    .collect(Collectors.groupingBy(face -> face, Collectors.counting()));
            tripleFace = counts.entrySet().stream()
                    .filter(entry -> entry.getValue() == 3L)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            if (session.getTableNumber() != null) {
                if (isTripleSum) {
                    historyCategory = SicboResultHistory.Category.TRIPLE;
                } else if (resultSum >= 11) {
                    historyCategory = SicboResultHistory.Category.BIG;
                } else {
                    historyCategory = SicboResultHistory.Category.SMALL;
                }
            }
        }

        List<SicboBet> pendingBets = betRepository.findBySessionAndStatus(session, SicboBet.Status.PENDING);
        if (!pendingBets.isEmpty()) {
            boolean isTripleResult = tripleFace != null;
            boolean isLowTriple = false;
            boolean isHighTriple = false;
            if (tripleFace != null) {
                isLowTriple = tripleFace <= 3;
                isHighTriple = tripleFace >= 4;
            }
            for (SicboBet bet : pendingBets) {
                bet.setResultCode(resultCode);
                bet.setSettledAt(now);

                    String betCode = bet.getBetCode();
                boolean resolved = false;

                if (isTripleResult && betCode != null) {
                    if (betCode.equals("sicbo_combo_triple_" + tripleFace)) {
                        BigDecimal multiplier = bet.getPayoutMultiplier() != null
                                ? bet.getPayoutMultiplier().add(BigDecimal.ONE)
                                : BigDecimal.ONE;
                        BigDecimal winAmount = bet.getStake().multiply(multiplier);

                        pointService.addPoints(
                                bet.getUser(),
                                winAmount,
                                PointTransaction.PointTransactionType.BET_WIN,
                                String.format("Thắng cược Sicbo bàn %d phiên #%d (%s)",
                                        session.getTableNumber(), session.getId(), bet.getBetCode()),
                                "SICBO",
                                session.getId(),
                                null
                        );

                        bet.setWinAmount(winAmount);
                        bet.setStatus(SicboBet.Status.WON);
                        resolved = true;
                    } else if (betCode.equals("sicbo_single_" + tripleFace)) {
                        BigDecimal baseMultiplier = Optional.ofNullable(bet.getPayoutMultiplier()).orElse(BigDecimal.ZERO);
                        BigDecimal winAmount = bet.getStake()
                                .add(bet.getStake().multiply(baseMultiplier).multiply(BigDecimal.valueOf(3)));

                        pointService.addPoints(
                                bet.getUser(),
                                winAmount,
                                PointTransaction.PointTransactionType.BET_WIN,
                                String.format("Thắng cược Sicbo (x3) bàn %d phiên #%d (%s)",
                                        session.getTableNumber(), session.getId(), bet.getBetCode()),
                                "SICBO",
                                session.getId(),
                                null
                        );

                        bet.setWinAmount(winAmount);
                        bet.setStatus(SicboBet.Status.WON);
                        resolved = true;
                    } else if (session.getTableNumber() != null && session.getTableNumber() == 2) {
                        if (isLowTriple && "sicbo_primary_small".equals(betCode)) {
                            pointService.addPoints(
                                    bet.getUser(),
                                    bet.getStake(),
                                    PointTransaction.PointTransactionType.BET_REFUND,
                                    String.format("Hoàn cược Sicbo bàn %d phiên #%d (%s) do ra bộ ba nhỏ",
                                            session.getTableNumber(), session.getId(), bet.getBetCode()),
                                    "SICBO",
                                    session.getId(),
                                    null
                            );

                            bet.setWinAmount(bet.getStake());
                            bet.setStatus(SicboBet.Status.REFUNDED);
                            resolved = true;
                        } else if (isHighTriple && "sicbo_primary_big".equals(betCode)) {
                            pointService.addPoints(
                                    bet.getUser(),
                                    bet.getStake(),
                                    PointTransaction.PointTransactionType.BET_REFUND,
                                    String.format("Hoàn cược Sicbo bàn %d phiên #%d (%s) do ra bộ ba lớn",
                                            session.getTableNumber(), session.getId(), bet.getBetCode()),
                                    "SICBO",
                                    session.getId(),
                                    null
                            );

                            bet.setWinAmount(bet.getStake());
                            bet.setStatus(SicboBet.Status.REFUNDED);
                            resolved = true;
                        }
                    }
                    }

                if (!resolved && betCode != null && winningCodes.contains(betCode)) {
                    BigDecimal multiplier = bet.getPayoutMultiplier() != null
                            ? bet.getPayoutMultiplier().add(BigDecimal.ONE)
                            : BigDecimal.ONE;
                    BigDecimal winAmount = bet.getStake().multiply(multiplier);

                    pointService.addPoints(
                            bet.getUser(),
                            winAmount,
                            PointTransaction.PointTransactionType.BET_WIN,
                            String.format("Thắng cược Sicbo bàn %d phiên #%d (%s)",
                                    session.getTableNumber(), session.getId(), bet.getBetCode()),
                            "SICBO",
                            session.getId(),
                            null
                    );

                    bet.setWinAmount(winAmount);
                    bet.setStatus(SicboBet.Status.WON);
                    resolved = true;
                }

                if (!resolved) {
                    bet.setWinAmount(BigDecimal.ZERO);
                    bet.setStatus(SicboBet.Status.LOST);
                }
            }

            betRepository.saveAll(pendingBets);
        }

        if (resultSum > 0 && historyCategory != null) {
            resultHistoryService.record(session, resultCode, resultSum, historyCategory, now);
        }
    }

    @Transactional
    public void refundUnsettledBets(SicboSession session, String reason) {
        List<SicboBet> pendingBets = betRepository.findBySessionAndStatusIn(
                session,
                Set.of(SicboBet.Status.PENDING)
        );

        if (pendingBets.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        for (SicboBet bet : pendingBets) {
            pointService.addPoints(
                    bet.getUser(),
                    bet.getStake(),
                    PointTransaction.PointTransactionType.BET_REFUND,
                    String.format("Hoàn điểm do phiên Sicbo bàn %d bị hủy (%s)",
                            session.getTableNumber(), reason),
                    "SICBO",
                    session.getId(),
                    null
            );
            bet.setResultCode(reason);
            bet.setWinAmount(bet.getStake());
            bet.setSettledAt(now);
            bet.setStatus(SicboBet.Status.REFUNDED);
        }

        betRepository.saveAll(pendingBets);
    }

    private static SicboQuickBetConfig createFallbackConfig(String code, String name, double multiplier,
                                                            String group, int order) {
        return SicboQuickBetConfig.builder()
                .code(code)
                .name(name)
                .payoutMultiplier(BigDecimal.valueOf(multiplier))
                .layoutGroup(group)
                .displayOrder(order)
                .isActive(true)
                .build();
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return "";
        }
        return code.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s]+", "_");
    }

    private Set<String> resolveWinningCodes(String resultCode) {
        List<Integer> faces = parseFaces(resultCode);
        if (faces.size() != 3) {
            log.warn("Không thể phân tích kết quả Sicbo [{}]", resultCode);
            return Collections.emptySet();
        }

        Set<String> winners = new HashSet<>();
        Map<Integer, Long> counts = faces.stream()
                .collect(Collectors.groupingBy(face -> face, Collectors.counting()));
        boolean isTriple = counts.values().stream().anyMatch(count -> count == 3);
        int total = faces.stream().mapToInt(Integer::intValue).sum();

        if (!isTriple) {
            if (total >= 4 && total <= 10) {
                winners.add("sicbo_primary_small");
            } else if (total >= 11 && total <= 17) {
                winners.add("sicbo_primary_big");
            }
        }

        if (total >= 4 && total <= 17) {
            winners.add("sicbo_total_" + total);
        }

        if (total % 2 == 0) {
            winners.add("sicbo_parity_even");
        } else {
            winners.add("sicbo_parity_odd");
        }

        counts.forEach((face, count) -> {
            if (count >= 1) {
                winners.add("sicbo_single_" + face);
            }
            if (count == 3) {
                winners.add("sicbo_combo_triple_" + face);
            }
        });

        return winners;
    }

    private List<Integer> parseFaces(String resultCode) {
        if (resultCode == null || resultCode.isBlank()) {
            return Collections.emptyList();
        }
        String[] rawParts = resultCode.split("[^0-9]+");
        List<Integer> faces = new ArrayList<>(3);
        for (String part : rawParts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            try {
                int value = Integer.parseInt(part.trim());
                if (value >= 1 && value <= 6) {
                    faces.add(value);
                }
            } catch (NumberFormatException ignored) {
                // ignore malformed tokens
            }
        }
        return faces;
    }

    @Transactional(readOnly = true)
    public SicboBetHistoryPageResponse getUserBetHistory(User user, int page, int size) {
        if (user == null) {
            throw new IllegalArgumentException("Người dùng không hợp lệ");
        }
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), 50);

        Pageable pageable = PageRequest.of(sanitizedPage, sanitizedSize);
        Page<SicboBet> betPage = betRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        List<SicboBetHistoryItemResponse> items = betPage.getContent().stream()
                .map(SicboBetHistoryItemResponse::fromEntity)
                .collect(Collectors.toList());

        return SicboBetHistoryPageResponse.builder()
                .items(items)
                .page(betPage.getNumber())
                .size(betPage.getSize())
                .totalItems(betPage.getTotalElements())
                .hasMore(betPage.hasNext())
                .build();
    }
}


