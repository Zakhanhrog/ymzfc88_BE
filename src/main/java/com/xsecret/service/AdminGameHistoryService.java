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
import com.xsecret.repository.DailyLossRefundRepository;
import com.xsecret.repository.GameRefundAccrualRepository;
import com.xsecret.repository.PromotionalMoneyRepository;
import com.xsecret.repository.SicboBetRepository;
import com.xsecret.repository.TransactionRepository;
import com.xsecret.repository.UserLoginHistoryRepository;
import com.xsecret.repository.UserRepository;
import com.xsecret.repository.XocDiaBetRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    private static final ZoneId SYSTEM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BetRepository betRepository;
    private final XocDiaBetRepository xocDiaBetRepository;
    private final SicboBetRepository sicboBetRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final UserLoginHistoryRepository userLoginHistoryRepository;
    private final GameRefundAccrualRepository gameRefundAccrualRepository;
    private final DailyLossRefundRepository dailyLossRefundRepository;
    private final PromotionalMoneyRepository promotionalMoneyRepository;

    private static final List<Transaction.TransactionStatus> SUCCESS_DEPOSIT_STATUSES = List.of(
            Transaction.TransactionStatus.APPROVED,
            Transaction.TransactionStatus.COMPLETED
    );

    private static final List<Transaction.TransactionStatus> SUCCESS_WITHDRAW_STATUSES = List.of(
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
            String agentCode,
            LocalDateTime startDate,
            LocalDateTime endDate,
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
            // Nếu có search, dùng findBySearchTermWithFilters
            userPage = userRepository.findBySearchTermWithFilters(
                    search.trim(),
                    null,
                    null,
                    null,
                    null,
                    pageable
            );
        } else if (StringUtils.hasText(agentCode)) {
            // Nếu chỉ có agentCode, dùng findAgentCustomers
            userPage = userRepository.findAgentCustomers(
                    agentCode,
                    null,
                    null,
                    pageable
            );
        } else {
            // Nếu không có search và agentCode, lấy tất cả users (trừ admin)
            userPage = userRepository.findByRoleNot(User.Role.ADMIN, pageable);
        }
        
        // Nếu có cả search và agentCode, filter thêm theo agentCode
        if (StringUtils.hasText(search) && StringUtils.hasText(agentCode)) {
            List<User> filteredUsers = userPage.getContent().stream()
                    .filter(user -> agentCode.equalsIgnoreCase(user.getInvitedByCode()))
                    .collect(Collectors.toList());
            // Tạo Page mới với filtered users
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredUsers.size());
            List<User> pagedUsers = start < filteredUsers.size() ? filteredUsers.subList(start, end) : List.of();
            userPage = new PageImpl<>(pagedUsers, pageable, filteredUsers.size());
        }

        List<AdminUserBetSummaryItemResponse> items = userPage.getContent().stream()
                .map(user -> buildUserBetSummaryItem(user, startDate, endDate))
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

        UserBetAggregate aggregate = computeAggregate(user, null, null);

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
        // Tính tổng lãi (winAmount - totalAmount), không bao gồm vốn
        // Chỉ tính lãi khi status filter là NULL hoặc WON
        BigDecimal totalWin = BigDecimal.ZERO;
        if (betStatus == null || betStatus == Bet.BetStatus.WON) {
            totalWin = betRepository.sumWinProfitByFilters(start, end);
        }

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

    private AdminUserBetSummaryItemResponse buildUserBetSummaryItem(User user, LocalDateTime startDate, LocalDateTime endDate) {
        UserBetAggregate aggregate = computeAggregate(user, startDate, endDate);
        BigDecimal totalWithdraw = safe(
                transactionRepository.sumWithdrawAmountByUserAndStatuses(user, SUCCESS_WITHDRAW_STATUSES)
        );
        Long currentBalance = user.getPoints() != null ? user.getPoints() : 0L;
        
        // Lấy IP lần đầu tiên đăng nhập
        List<String> firstLoginIps = userLoginHistoryRepository.findFirstLoginIpByUser(user, PageRequest.of(0, 1));
        String firstLoginIp = firstLoginIps.isEmpty() ? null : firstLoginIps.get(0);
        
        return AdminUserBetSummaryItemResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .totalStakeAmount(aggregate.totalStake())
                .totalWinAmount(aggregate.totalWin())
                .totalLossAmount(aggregate.totalLoss())
                .totalDepositAmount(aggregate.totalDeposit())
                .totalWithdrawAmount(totalWithdraw)
                .currentBalance(currentBalance)
                .netProfitAmount(aggregate.netProfit())
                .firstLoginIp(firstLoginIp)
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
                .totalWithdrawAmount(aggregate.totalWithdraw())
                .totalRefundAmount(aggregate.totalRefund())
                .totalDailyLossRefundAmount(aggregate.totalDailyLossRefund())
                .totalPromotionalMoneyAmount(aggregate.totalPromotionalMoney())
                .netProfitAmount(aggregate.netProfit())
                .items(items)
                .totalItems(totalItems)
                .page(page)
                .size(size)
                .hasMore(hasMore)
                .build();
    }

    private UserBetAggregate computeAggregate(User user, LocalDateTime startDate, LocalDateTime endDate) {
        Instant startInstant = startDate != null ? startDate.atZone(SYSTEM_ZONE).toInstant() : null;
        Instant endInstant = endDate != null ? toInstantEndOfDay(endDate) : null;
        
        // Tính tổng cược: bao gồm TẤT CẢ bets (kể cả REFUNDED) - giống user betting history
        // Lottery: chỉ filter CANCELLED
        BigDecimal lotteryStake = safe(betRepository.sumStakeByUserId(user.getId()));
        // Tính lãi thắng (không tính gốc): winAmount - totalAmount
        BigDecimal lotteryWin = safe(betRepository.sumWinProfitByUserId(user.getId()));
        BigDecimal lotteryLoss = safe(betRepository.sumLostStakeByUserId(user.getId()));

        // Sicbo: tính TẤT CẢ stake (bao gồm REFUNDED) - giống user betting history
        List<SicboBet> allSicboBets = sicboBetRepository.findByUserOrderByCreatedAtDesc(user, Pageable.unpaged()).getContent();
        BigDecimal sicboStake = allSicboBets.stream()
                .map(SicboBet::getStake)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Tính lãi thắng (không tính gốc): winAmount - stake
        BigDecimal sicboWin = safe(sicboBetRepository.sumWinProfitByUser(user));
        BigDecimal sicboLoss = safe(sicboBetRepository.sumLostStakeByUser(user));

        // XocDia: tính TẤT CẢ stake (bao gồm REFUNDED) - giống user betting history
        List<XocDiaBet> allXocDiaBets = xocDiaBetRepository.findByUserOrderByCreatedAtDesc(user, Pageable.unpaged()).getContent();
        BigDecimal xocDiaStake = allXocDiaBets.stream()
                .map(XocDiaBet::getStake)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Tính lãi thắng (không tính gốc): winAmount - stake
        BigDecimal xocDiaWin = safe(xocDiaBetRepository.sumWinProfitByUser(user));
        BigDecimal xocDiaLoss = safe(xocDiaBetRepository.sumLostStakeByUser(user));

        // Nếu có date range, filter lại các bet theo date range
        if (startInstant != null || endInstant != null) {
            // Filter Lottery bets
            List<Bet> lotteryBets = betRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), Pageable.unpaged()).getContent();
            if (startInstant != null || endInstant != null) {
                lotteryBets = lotteryBets.stream()
                        .filter(bet -> {
                            LocalDateTime betTime = bet.getCreatedAt();
                            if (betTime == null) return false;
                            Instant betInstant = betTime.atZone(SYSTEM_ZONE).toInstant();
                            if (startInstant != null && betInstant.isBefore(startInstant)) return false;
                            if (endInstant != null && betInstant.isAfter(endInstant)) return false;
                            return true;
                        })
                        .collect(Collectors.toList());
            }
            // Tổng cược: chỉ tính totalAmount, không tính CANCELLED bets
            lotteryStake = lotteryBets.stream()
                    .filter(b -> b.getStatus() != Bet.BetStatus.CANCELLED)
                    .map(Bet::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // Tổng thắng: chỉ tính lãi (winAmount - totalAmount), không tính gốc
            lotteryWin = lotteryBets.stream()
                    .filter(b -> b.getStatus() == Bet.BetStatus.WON && b.getWinAmount() != null && b.getTotalAmount() != null)
                    .map(b -> b.getWinAmount().subtract(b.getTotalAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // Tổng thua: chỉ tính totalAmount khi thua
            lotteryLoss = lotteryBets.stream()
                    .filter(b -> b.getStatus() == Bet.BetStatus.LOST)
                    .map(Bet::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Filter Sicbo bets
            List<SicboBet> sicboBets = sicboBetRepository.findByUserOrderByCreatedAtDesc(user, Pageable.unpaged()).getContent();
            if (startInstant != null || endInstant != null) {
                sicboBets = sicboBets.stream()
                        .filter(bet -> {
                            Instant betTime = bet.getCreatedAt();
                            if (betTime == null) return false;
                            if (startInstant != null && betTime.isBefore(startInstant)) return false;
                            if (endInstant != null && betTime.isAfter(endInstant)) return false;
                            return true;
                        })
                        .collect(Collectors.toList());
            }
            // Tổng cược: tính TẤT CẢ stake (bao gồm REFUNDED) - giống user betting history
            sicboStake = sicboBets.stream()
                    .map(SicboBet::getStake)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // Tổng thắng: chỉ tính lãi (winAmount - stake), không tính gốc
            sicboWin = sicboBets.stream()
                    .filter(b -> b.getStatus() == SicboBet.Status.WON && b.getWinAmount() != null)
                    .map(b -> b.getWinAmount().subtract(b.getStake()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // Tổng thua: chỉ tính stake khi thua
            sicboLoss = sicboBets.stream()
                    .filter(b -> b.getStatus() == SicboBet.Status.LOST)
                    .map(SicboBet::getStake)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Filter XocDia bets
            List<XocDiaBet> xocDiaBets = xocDiaBetRepository.findByUserOrderByCreatedAtDesc(user, Pageable.unpaged()).getContent();
            if (startInstant != null || endInstant != null) {
                xocDiaBets = xocDiaBets.stream()
                        .filter(bet -> {
                            Instant betTime = bet.getCreatedAt();
                            if (betTime == null) return false;
                            if (startInstant != null && betTime.isBefore(startInstant)) return false;
                            if (endInstant != null && betTime.isAfter(endInstant)) return false;
                            return true;
                        })
                        .collect(Collectors.toList());
            }
            // Tổng cược: tính TẤT CẢ stake (bao gồm REFUNDED) - giống user betting history
            xocDiaStake = xocDiaBets.stream()
                    .map(XocDiaBet::getStake)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // Tổng thắng: chỉ tính lãi (winAmount - stake), không tính gốc
            xocDiaWin = xocDiaBets.stream()
                    .filter(b -> b.getStatus() == XocDiaBet.Status.WON && b.getWinAmount() != null)
                    .map(b -> b.getWinAmount().subtract(b.getStake()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            // Tổng thua: chỉ tính stake khi thua
            xocDiaLoss = xocDiaBets.stream()
                    .filter(b -> b.getStatus() == XocDiaBet.Status.LOST)
                    .map(XocDiaBet::getStake)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal totalStake = lotteryStake.add(sicboStake).add(xocDiaStake);
        BigDecimal totalWin = lotteryWin.add(sicboWin).add(xocDiaWin);
        BigDecimal totalLoss = lotteryLoss.add(sicboLoss).add(xocDiaLoss);
        BigDecimal totalDeposit = safe(
                transactionRepository.sumDepositAmountByUserAndStatuses(user, SUCCESS_DEPOSIT_STATUSES)
        );
        BigDecimal totalWithdraw = safe(
                transactionRepository.sumWithdrawAmountByUserAndStatuses(user, SUCCESS_WITHDRAW_STATUSES)
        );
        // Chỉ tính hoàn trả cho lệnh cược thua
        BigDecimal totalRefund = safe(
                gameRefundAccrualRepository.sumPaidLossRefundByUser(user)
        );
        BigDecimal totalDailyLossRefund = safe(
                dailyLossRefundRepository.sumPaidRefundByUser(user)
        );
        BigDecimal totalPromotionalMoney = safe(
                promotionalMoneyRepository.sumAmountByUser(user)
        );
        BigDecimal netProfit = totalWin.subtract(totalLoss);

        return new UserBetAggregate(totalStake, totalWin, totalLoss, totalDeposit, totalWithdraw, 
                totalRefund, totalDailyLossRefund, totalPromotionalMoney, netProfit);
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
        // Tính tổng lãi (winAmount - stake), không bao gồm vốn
        // Chỉ tính lãi khi status filter là NULL hoặc WON
        BigDecimal totalWin = BigDecimal.ZERO;
        if (betStatus == null || betStatus == XocDiaBet.Status.WON) {
            totalWin = xocDiaBetRepository.sumWinProfitByCreatedAtFilters(start, end);
        }

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
        // Tính tổng lãi (winAmount - stake), không bao gồm vốn
        // Chỉ tính lãi khi status filter là NULL hoặc WON
        BigDecimal totalWin = BigDecimal.ZERO;
        if (betStatus == null || betStatus == SicboBet.Status.WON) {
            totalWin = sicboBetRepository.sumWinProfitByCreatedAtFilters(start, end);
        }

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
        return date.atStartOfDay(SYSTEM_ZONE).toInstant();
    }

    private Instant toInstantEnd(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atTime(LocalTime.MAX).atZone(SYSTEM_ZONE).toInstant();
    }

    private Instant toInstantEndOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toLocalDate().atTime(LocalTime.MAX).atZone(SYSTEM_ZONE).toInstant();
    }

    private Instant toInstant(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(SYSTEM_ZONE).toInstant();
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
            BigDecimal totalWithdraw,
            BigDecimal totalRefund,
            BigDecimal totalDailyLossRefund,
            BigDecimal totalPromotionalMoney,
            BigDecimal netProfit
    ) {
    }
}

