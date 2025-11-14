package com.xsecret.service;

import com.xsecret.dto.response.AdminGameBetHistoryItemResponse;
import com.xsecret.dto.response.AdminGameBetHistoryResponse;
import com.xsecret.dto.response.AdminUserBetDetailResponse;
import com.xsecret.dto.response.AdminUserBetSummaryItemResponse;
import com.xsecret.dto.response.AdminUserBetSummaryResponse;
import com.xsecret.entity.Bet;
import com.xsecret.entity.SicboBet;
import com.xsecret.entity.Transaction;
import com.xsecret.entity.User;
import com.xsecret.entity.XocDiaBet;
import com.xsecret.repository.BetRepository;
import com.xsecret.repository.SicboBetRepository;
import com.xsecret.repository.TransactionRepository;
import com.xsecret.repository.UserRepository;
import com.xsecret.repository.XocDiaBetRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminGameHistoryService {

    private final BetRepository betRepository;
    private final XocDiaBetRepository xocDiaBetRepository;
    private final SicboBetRepository sicboBetRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    private static final List<Transaction.TransactionStatus> SUCCESS_DEPOSIT_STATUSES = List.of(
            Transaction.TransactionStatus.APPROVED,
            Transaction.TransactionStatus.COMPLETED
    );

    @Transactional
    public AdminGameBetHistoryResponse getGameHistory(
            String rawGameType,
            String rawStatus,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        String gameType = normalize(rawGameType, "lottery");
        String status = normalize(rawStatus, null);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        switch (gameType) {
            case "xocdia":
            case "xoc-dia":
                return buildXocDiaHistory(status, startDate, endDate, pageable);
            case "sicbo":
                return buildSicboHistory(status, startDate, endDate, pageable);
            case "lottery":
            default:
                return buildLotteryHistory(status, startDate, endDate, pageable);
        }
    }

    @Transactional
    public AdminUserBetSummaryResponse getUserBetSummaries(
            String search,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<User> userPage;
        if (StringUtils.hasText(search)) {
            userPage = userRepository.findBySearchTermWithFilters(
                    search.trim(),
                    null,
                    null,
                    pageable
            );
        } else {
            userPage = userRepository.findByRoleNot(User.Role.ADMIN, pageable);
        }

        List<AdminUserBetSummaryItemResponse> items = userPage.getContent().stream()
                .map(this::buildUserBetSummaryItem)
                .collect(Collectors.toList());

        return AdminUserBetSummaryResponse.builder()
                .items(items)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalItems(userPage.getTotalElements())
                .hasMore(userPage.hasNext())
                .build();
    }

    @Transactional
    public AdminUserBetDetailResponse getUserBetDetail(
            Long userId,
            String rawGameType,
            int page,
            int size
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("Thiếu thông tin người dùng");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        String gameType = normalizeGameType(rawGameType);
        int pageIndex = Math.max(page, 0);
        int pageSize = Math.max(size, 1);

        boolean includeLottery = "all".equals(gameType) || "lottery".equals(gameType);
        boolean includeSicbo = "all".equals(gameType) || "sicbo".equals(gameType);
        boolean includeXocDia = "all".equals(gameType) || "xocdia".equals(gameType);

        long totalLottery = includeLottery ? betRepository.countByUserId(userId) : 0;
        long totalSicbo = includeSicbo ? sicboBetRepository.countByUser(user) : 0;
        long totalXocDia = includeXocDia ? xocDiaBetRepository.countByUser(user) : 0;
        long totalItems = totalLottery + totalSicbo + totalXocDia;

        UserBetAggregate aggregate = computeAggregate(user);

        if (totalItems == 0 || (long) pageIndex * pageSize >= totalItems) {
            return buildDetailResponse(user, aggregate, List.of(), totalItems, pageIndex, pageSize);
        }

        List<AdminGameBetHistoryItemResponse> combined = new ArrayList<>();

        if (includeLottery && totalLottery > 0) {
            int fetchSize = computeFetchSize(totalLottery, pageIndex, pageSize);
            Pageable lotteryPageable = PageRequest.of(0, fetchSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            combined.addAll(
                    betRepository.findByUserIdOrderByCreatedAtDesc(userId, lotteryPageable)
                            .getContent()
                            .stream()
                            .map(this::mapLotteryBet)
                            .collect(Collectors.toList())
            );
        }

        if (includeSicbo && totalSicbo > 0) {
            int fetchSize = computeFetchSize(totalSicbo, pageIndex, pageSize);
            Pageable sicboPageable = PageRequest.of(0, fetchSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            combined.addAll(
                    sicboBetRepository.findByUserOrderByCreatedAtDesc(user, sicboPageable)
                            .getContent()
                            .stream()
                            .map(this::mapSicboBet)
                            .collect(Collectors.toList())
            );
        }

        if (includeXocDia && totalXocDia > 0) {
            int fetchSize = computeFetchSize(totalXocDia, pageIndex, pageSize);
            Pageable xocDiaPageable = PageRequest.of(0, fetchSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            combined.addAll(
                    xocDiaBetRepository.findByUserOrderByCreatedAtDesc(user, xocDiaPageable)
                            .getContent()
                            .stream()
                            .map(this::mapXocDiaBet)
                            .collect(Collectors.toList())
            );
        }

        combined.sort(Comparator.comparing(
                AdminGameBetHistoryItemResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        int fromIndex = Math.min(pageIndex * pageSize, combined.size());
        int toIndex = Math.min(fromIndex + pageSize, combined.size());
        List<AdminGameBetHistoryItemResponse> pageItems = combined.subList(fromIndex, toIndex);

        return buildDetailResponse(user, aggregate, pageItems, totalItems, pageIndex, pageSize);
    }

    private AdminGameBetHistoryResponse buildLotteryHistory(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        Bet.BetStatus betStatus = parseLotteryStatus(status);
        LocalDateTime start = toLocalDateTimeStart(startDate);
        LocalDateTime end = toLocalDateTimeEnd(endDate);

        Page<Bet> pageResult = betRepository.findForAnalytics(
                betStatus,
                start,
                end,
                pageable
        );

        BigDecimal totalStake = betRepository.sumTotalAmountByFilters(betStatus, start, end);
        BigDecimal totalWin = betRepository.sumWinAmountByFilters(betStatus, start, end);

        List<AdminGameBetHistoryItemResponse> items = pageResult.getContent()
                .stream()
                .map(this::mapLotteryBet)
                .collect(Collectors.toList());

        return AdminGameBetHistoryResponse.builder()
                .items(items)
                .totalItems(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalStakeAmount(safe(totalStake))
                .totalWinAmount(safe(totalWin))
                .build();
    }

    private AdminUserBetSummaryItemResponse buildUserBetSummaryItem(User user) {
        UserBetAggregate aggregate = computeAggregate(user);
        return AdminUserBetSummaryItemResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .totalStakeAmount(aggregate.totalStake())
                .totalWinAmount(aggregate.totalWin())
                .totalLossAmount(aggregate.totalLoss())
                .totalDepositAmount(aggregate.totalDeposit())
                .netProfitAmount(aggregate.netProfit())
                .build();
    }

    private AdminUserBetDetailResponse buildDetailResponse(
            User user,
            UserBetAggregate aggregate,
            List<AdminGameBetHistoryItemResponse> items,
            long totalItems,
            int page,
            int size
    ) {
        boolean hasMore = totalItems > (long) (page + 1) * size;
        return AdminUserBetDetailResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .totalStakeAmount(aggregate.totalStake())
                .totalWinAmount(aggregate.totalWin())
                .totalLossAmount(aggregate.totalLoss())
                .totalDepositAmount(aggregate.totalDeposit())
                .netProfitAmount(aggregate.netProfit())
                .items(items)
                .totalItems(totalItems)
                .page(page)
                .size(size)
                .hasMore(hasMore)
                .build();
    }

    private UserBetAggregate computeAggregate(User user) {
        BigDecimal lotteryStake = safe(betRepository.sumStakeByUserId(user.getId()));
        BigDecimal lotteryWin = safe(betRepository.sumWinAmountByUserId(user.getId()));
        BigDecimal lotteryLoss = safe(betRepository.sumLostStakeByUserId(user.getId()));

        BigDecimal sicboStake = safe(sicboBetRepository.sumStakeByUser(user));
        BigDecimal sicboWin = safe(sicboBetRepository.sumWinAmountByUser(user));
        BigDecimal sicboLoss = safe(sicboBetRepository.sumLostStakeByUser(user));

        BigDecimal xocDiaStake = safe(xocDiaBetRepository.sumStakeByUser(user));
        BigDecimal xocDiaWin = safe(xocDiaBetRepository.sumWinAmountByUser(user));
        BigDecimal xocDiaLoss = safe(xocDiaBetRepository.sumLostStakeByUser(user));

        BigDecimal totalStake = lotteryStake.add(sicboStake).add(xocDiaStake);
        BigDecimal totalWin = lotteryWin.add(sicboWin).add(xocDiaWin);
        BigDecimal totalLoss = lotteryLoss.add(sicboLoss).add(xocDiaLoss);
        BigDecimal totalDeposit = safe(
                transactionRepository.sumDepositAmountByUserAndStatuses(user, SUCCESS_DEPOSIT_STATUSES)
        );
        BigDecimal netProfit = totalWin.subtract(totalLoss);

        return new UserBetAggregate(totalStake, totalWin, totalLoss, totalDeposit, netProfit);
    }

    private int computeFetchSize(long totalCount, int page, int size) {
        long required = (long) (page + 1) * size;
        long fetch = Math.min(totalCount, Math.max(required, size));
        return (int) Math.min(fetch, Integer.MAX_VALUE);
    }

    private String normalizeGameType(String rawGameType) {
        String normalized = normalize(rawGameType, "all");
        return switch (normalized) {
            case "sicbo" -> "sicbo";
            case "xoc-dia", "xocdia" -> "xocdia";
            case "lottery", "lotto" -> "lottery";
            case "all" -> "all";
            default -> "all";
        };
    }

    private AdminGameBetHistoryResponse buildXocDiaHistory(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        XocDiaBet.Status betStatus = parseXocDiaStatus(status);
        Instant start = toInstantStart(startDate);
        Instant end = toInstantEnd(endDate);

        Page<XocDiaBet> pageResult = xocDiaBetRepository.findAdminHistory(betStatus, start, end, pageable);
        BigDecimal totalStake = xocDiaBetRepository.sumStakeByCreatedAtFilters(betStatus, start, end);
        BigDecimal totalWin = xocDiaBetRepository.sumWinAmountByCreatedAtFilters(betStatus, start, end);

        List<AdminGameBetHistoryItemResponse> items = pageResult.getContent()
                .stream()
                .map(this::mapXocDiaBet)
                .collect(Collectors.toList());

        return AdminGameBetHistoryResponse.builder()
                .items(items)
                .totalItems(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalStakeAmount(safe(totalStake))
                .totalWinAmount(safe(totalWin))
                .build();
    }

    private AdminGameBetHistoryResponse buildSicboHistory(
            String status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        SicboBet.Status betStatus = parseSicboStatus(status);
        Instant start = toInstantStart(startDate);
        Instant end = toInstantEnd(endDate);

        Page<SicboBet> pageResult = sicboBetRepository.findAdminHistory(betStatus, start, end, pageable);
        BigDecimal totalStake = sicboBetRepository.sumStakeByCreatedAtFilters(betStatus, start, end);
        BigDecimal totalWin = sicboBetRepository.sumWinAmountByCreatedAtFilters(betStatus, start, end);

        List<AdminGameBetHistoryItemResponse> items = pageResult.getContent()
                .stream()
                .map(this::mapSicboBet)
                .collect(Collectors.toList());

        return AdminGameBetHistoryResponse.builder()
                .items(items)
                .totalItems(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalStakeAmount(safe(totalStake))
                .totalWinAmount(safe(totalWin))
                .build();
    }

    private AdminGameBetHistoryItemResponse mapLotteryBet(Bet bet) {
        return AdminGameBetHistoryItemResponse.builder()
                .id(bet.getId())
                .gameType("LOTTERY")
                .userId(bet.getUser() != null ? bet.getUser().getId() : null)
                .username(bet.getUser() != null ? bet.getUser().getUsername() : null)
                .fullName(bet.getUser() != null ? bet.getUser().getFullName() : null)
                .phoneNumber(bet.getUser() != null ? bet.getUser().getPhoneNumber() : null)
                .betCode(bet.getBetType())
                .description(String.format(Locale.ROOT, "Miền: %s | Đài: %s | Số: %s",
                        valueOrDash(bet.getRegion()),
                        valueOrDash(bet.getProvince()),
                        valueOrDash(bet.getSelectedNumbers())))
                .stakeAmount(safe(bet.getTotalAmount()))
                .potentialWinAmount(safe(bet.getPotentialWin()))
                .winAmount(safe(bet.getWinAmount()))
                .status(bet.getStatus() != null ? bet.getStatus().name() : null)
                .resultCode(valueOrDash(bet.getWinningNumbers()))
                .createdAt(toInstant(bet.getCreatedAt()))
                .settledAt(toInstant(bet.getResultCheckedAt()))
                .build();
    }

    private AdminGameBetHistoryItemResponse mapXocDiaBet(XocDiaBet bet) {
        return AdminGameBetHistoryItemResponse.builder()
                .id(bet.getId())
                .gameType("XOCDIA")
                .userId(bet.getUser() != null ? bet.getUser().getId() : null)
                .username(bet.getUser() != null ? bet.getUser().getUsername() : null)
                .fullName(bet.getUser() != null ? bet.getUser().getFullName() : null)
                .phoneNumber(bet.getUser() != null ? bet.getUser().getPhoneNumber() : null)
                .betCode(bet.getBetCode())
                .description("Phiên #" + (bet.getSession() != null ? bet.getSession().getId() : "-"))
                .stakeAmount(safe(bet.getStake()))
                .potentialWinAmount(safe(bet.getStake()).multiply(safe(bet.getPayoutMultiplier())))
                .winAmount(safe(bet.getWinAmount()))
                .status(bet.getStatus() != null ? bet.getStatus().name() : null)
                .resultCode(valueOrDash(bet.getResultCode()))
                .sessionId(bet.getSession() != null ? bet.getSession().getId() : null)
                .createdAt(bet.getCreatedAt())
                .settledAt(bet.getSettledAt())
                .build();
    }

    private AdminGameBetHistoryItemResponse mapSicboBet(SicboBet bet) {
        return AdminGameBetHistoryItemResponse.builder()
                .id(bet.getId())
                .gameType("SICBO")
                .userId(bet.getUser() != null ? bet.getUser().getId() : null)
                .username(bet.getUser() != null ? bet.getUser().getUsername() : null)
                .fullName(bet.getUser() != null ? bet.getUser().getFullName() : null)
                .phoneNumber(bet.getUser() != null ? bet.getUser().getPhoneNumber() : null)
                .betCode(bet.getBetCode())
                .description("Phiên #" + (bet.getSession() != null ? bet.getSession().getId() : "-"))
                .stakeAmount(safe(bet.getStake()))
                .potentialWinAmount(safe(bet.getStake()).multiply(safe(bet.getPayoutMultiplier())))
                .winAmount(safe(bet.getWinAmount()))
                .status(bet.getStatus() != null ? bet.getStatus().name() : null)
                .resultCode(valueOrDash(bet.getResultCode()))
                .sessionId(bet.getSession() != null ? bet.getSession().getId() : null)
                .tableNumber(bet.getSession() != null ? bet.getSession().getTableNumber() : null)
                .createdAt(bet.getCreatedAt())
                .settledAt(bet.getSettledAt())
                .build();
    }

    private Bet.BetStatus parseLotteryStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return Bet.BetStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid lottery status filter: {}", status);
            return null;
        }
    }

    private XocDiaBet.Status parseXocDiaStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return XocDiaBet.Status.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid XocDia status filter: {}", status);
            return null;
        }
    }

    private SicboBet.Status parseSicboStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return SicboBet.Status.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid Sicbo status filter: {}", status);
            return null;
        }
    }

    private String normalize(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private LocalDateTime toLocalDateTimeStart(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay();
    }

    private LocalDateTime toLocalDateTimeEnd(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atTime(LocalTime.MAX);
    }

    private Instant toInstantStart(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Instant toInstantEnd(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();
    }

    private Instant toInstant(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String valueOrDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private record UserBetAggregate(
            BigDecimal totalStake,
            BigDecimal totalWin,
            BigDecimal totalLoss,
            BigDecimal totalDeposit,
            BigDecimal netProfit
    ) {
    }
}

