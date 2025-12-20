package com.xsecret.service;

import com.xsecret.dto.response.DashboardOverviewResponse;
import com.xsecret.entity.Bet;
import com.xsecret.entity.Transaction;
import com.xsecret.entity.User;
import com.xsecret.entity.SicboBet;
import com.xsecret.entity.XocDiaBet;
import com.xsecret.repository.BetRepository;
import com.xsecret.repository.TransactionRepository;
import com.xsecret.repository.SicboBetRepository;
import com.xsecret.repository.XocDiaBetRepository;
import com.xsecret.repository.UserRepository;
import com.xsecret.repository.GameRefundAccrualRepository;
import com.xsecret.repository.DailyLossRefundRepository;
import com.xsecret.repository.AgentCommissionPayoutRepository;
import com.xsecret.repository.PromotionalMoneyRepository;
import com.xsecret.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardService {

    private static final int DEFAULT_RECENT_ACTIVITY_LIMIT = 10;
    private static final int DEFAULT_CHART_DAYS = 7;
    private static final int ONLINE_THRESHOLD_MINUTES = 15;

    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final TransactionRepository transactionRepository;
    private final SicboBetRepository sicboBetRepository;
    private final XocDiaBetRepository xocDiaBetRepository;
    private final GameRefundAccrualRepository gameRefundAccrualRepository;
    private final DailyLossRefundRepository dailyLossRefundRepository;
    private final AgentCommissionPayoutRepository agentCommissionPayoutRepository;
    private final PromotionalMoneyRepository promotionalMoneyRepository;
    private final PointTransactionRepository pointTransactionRepository;

    private final ZoneId systemZone = ZoneId.systemDefault();

    public DashboardOverviewResponse getOverview() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1);
        LocalDate chartStartDate = today.minusDays(DEFAULT_CHART_DAYS - 1L);
        LocalDateTime chartStartDateTime = chartStartDate.atStartOfDay();
        LocalDateTime chartEndDateTime = endOfDay;
        LocalDateTime onlineThreshold = LocalDateTime.now().minusMinutes(ONLINE_THRESHOLD_MINUTES);

        long totalUsers = userRepository.countUsers();
        long activeUsers = userRepository.countByStatus(User.UserStatus.ACTIVE);
        long newUsersToday = userRepository.countByCreatedAtAfter(startOfDay);
        long onlineUsers = userRepository.countOnlineUsers(User.UserStatus.ACTIVE, onlineThreshold);

        Instant startOfDayInstant = startOfDay.atZone(systemZone).toInstant();
        Instant endOfDayInstant = endOfDay.atZone(systemZone).toInstant();

        BigDecimal lotteryRevenue = safeBigDecimal(
                betRepository.sumTotalAmountByStatusAndResultCheckedAtBetween(Bet.BetStatus.LOST, startOfDay, endOfDay));
        BigDecimal sicboRevenue = safeBigDecimal(
                sicboBetRepository.sumStakeByStatusAndSettledAtBetween(SicboBet.Status.LOST, startOfDayInstant, endOfDayInstant));
        BigDecimal xocdiaRevenue = safeBigDecimal(
                xocDiaBetRepository.sumStakeByStatusAndSettledAtBetween(XocDiaBet.Status.LOST, startOfDayInstant, endOfDayInstant));
        BigDecimal revenueToday = lotteryRevenue.add(sicboRevenue).add(xocdiaRevenue);

        List<Transaction.TransactionStatus> completedStatuses = List.of(
                Transaction.TransactionStatus.APPROVED,
                Transaction.TransactionStatus.COMPLETED
        );

        long transactionsTodayCount = transactionRepository.countByStatusInAndCreatedAtBetween(
                completedStatuses, startOfDay, endOfDay);
        BigDecimal transactionsTodayAmount = safeBigDecimal(
                transactionRepository.sumNetAmountByStatusInAndCreatedAtBetween(completedStatuses, startOfDay, endOfDay));

        // Tổng nạp hôm nay (DEPOSIT với status COMPLETED)
        BigDecimal depositsTodayAmount = safeBigDecimal(
                transactionRepository.sumAmountByTypeAndStatusAndCreatedAtBetween(
                        Transaction.TransactionType.DEPOSIT,
                        Transaction.TransactionStatus.COMPLETED,
                        startOfDay,
                        endOfDay
                )
        );

        // Tổng rút hôm nay (WITHDRAW với status APPROVED hoặc COMPLETED)
        List<Transaction.TransactionStatus> withdrawStatuses = List.of(
                Transaction.TransactionStatus.APPROVED,
                Transaction.TransactionStatus.COMPLETED
        );
        BigDecimal withdrawalsTodayAmount = safeBigDecimal(
                transactionRepository.sumNetAmountByTypeAndStatusesAndCreatedAtBetween(
                        Transaction.TransactionType.WITHDRAW,
                        withdrawStatuses,
                        startOfDay,
                        endOfDay
                )
        );

        // Tính lợi nhuận hôm nay theo công thức Analytics
        // Doanh thu = Tổng tiền cược - Tổng tiền thắng
        BigDecimal totalStakeToday = safeBigDecimal(betRepository.sumTotalAmountByFilters(null, startOfDay, endOfDay))
                .add(safeBigDecimal(sicboBetRepository.sumStakeByCreatedAtFilters(null, startOfDayInstant, endOfDayInstant)))
                .add(safeBigDecimal(xocDiaBetRepository.sumStakeByCreatedAtFilters(null, startOfDayInstant, endOfDayInstant)));
        
        BigDecimal totalWinAmountToday = safeBigDecimal(betRepository.sumWinProfitByFilters(startOfDay, endOfDay))
                .add(safeBigDecimal(sicboBetRepository.sumWinProfitByCreatedAtFilters(startOfDayInstant, endOfDayInstant)))
                .add(safeBigDecimal(xocDiaBetRepository.sumWinProfitByCreatedAtFilters(startOfDayInstant, endOfDayInstant)));
        
        BigDecimal revenueTodayCalculated = totalStakeToday.subtract(totalWinAmountToday);
        
        // Tính các khoản trừ cho lợi nhuận
        // Tổng hoàn trả (scheduled + instant)
        BigDecimal scheduledRefund = safeBigDecimal(gameRefundAccrualRepository.sumPaidRefundByDateRange(
                startOfDayInstant, endOfDayInstant));
        BigDecimal instantRefund = safeBigDecimal(pointTransactionRepository.sumInstantGameRefundByDateRange(
                startOfDay, endOfDay));
        BigDecimal totalRefund = scheduledRefund.add(instantRefund);
        
        // Tổng khuyến mãi
        BigDecimal totalPromotionalMoney = safeBigDecimal(promotionalMoneyRepository.sumAmountByDateRange(
                startOfDay, endOfDay));
        
        // Tổng hoàn thua
        BigDecimal totalDailyLossRefund = safeBigDecimal(dailyLossRefundRepository.sumPaidRefundByDateRange(
                startOfDayInstant, endOfDayInstant));
        
        // Tổng hoa hồng đại lý
        BigDecimal totalAgentCommission = safeBigDecimal(agentCommissionPayoutRepository.sumPaidCommissionByDateRange(
                startOfDay, endOfDay));
        
        // Lợi nhuận = Doanh thu - (Hoàn trả + Khuyến mãi + Hoàn Thua + Hoa hồng)
        BigDecimal profitToday = revenueTodayCalculated
                .subtract(totalRefund)
                .subtract(totalPromotionalMoney)
                .subtract(totalDailyLossRefund)
                .subtract(totalAgentCommission);

        Map<LocalDate, DashboardOverviewResponse.ChartPoint> chartMap = initChartMap(chartStartDate);

        populateBetChartData(chartMap, chartStartDateTime, chartEndDateTime);
        populateSicboChartData(chartMap, chartStartDateTime, chartEndDateTime);
        populateXocDiaChartData(chartMap, chartStartDateTime, chartEndDateTime);
        populateTransactionChartData(chartMap, completedStatuses, chartStartDateTime, chartEndDateTime);
        populateWinLossRefundData(chartMap, chartStartDateTime, chartEndDateTime);

        List<DashboardOverviewResponse.ActivityItem> recentActivities = buildRecentActivities(completedStatuses);
        List<DashboardOverviewResponse.RecentUserItem> recentUsers = buildRecentUsers();

        DashboardOverviewResponse.Summary summary = DashboardOverviewResponse.Summary.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .newUsersToday(newUsersToday)
                .onlineUsers(onlineUsers)
                .revenueToday(revenueToday)
                .profitToday(profitToday)
                .transactionsTodayCount(transactionsTodayCount)
                .transactionsTodayAmount(transactionsTodayAmount)
                .depositsTodayAmount(depositsTodayAmount)
                .withdrawalsTodayAmount(withdrawalsTodayAmount)
                .build();

        return DashboardOverviewResponse.builder()
                .summary(summary)
                .chart(new ArrayList<>(chartMap.values()))
                .recentActivities(recentActivities)
                .recentUsers(recentUsers)
                .build();
    }

    private Map<LocalDate, DashboardOverviewResponse.ChartPoint> initChartMap(LocalDate startDate) {
        Map<LocalDate, DashboardOverviewResponse.ChartPoint> chartMap = new LinkedHashMap<>();
        for (int i = 0; i < DEFAULT_CHART_DAYS; i++) {
            LocalDate date = startDate.plusDays(i);
            chartMap.put(date, DashboardOverviewResponse.ChartPoint.builder()
                    .date(date)
                    .revenue(BigDecimal.ZERO)
                    .totalBets(0L)
                    .transactions(0L)
                    .transactionAmount(BigDecimal.ZERO)
                    .winProfit(BigDecimal.ZERO)
                    .lostStake(BigDecimal.ZERO)
                    .totalRefund(BigDecimal.ZERO)
                    .build());
        }
        return chartMap;
    }

    private void populateBetChartData(Map<LocalDate, DashboardOverviewResponse.ChartPoint> chartMap,
                                      LocalDateTime start, LocalDateTime end) {
        List<Object[]> stats = betRepository.getDailyBetStats(start, end);
        for (Object[] row : stats) {
            LocalDate date = toLocalDate(row[0]);
            if (date == null || !chartMap.containsKey(date)) {
                continue;
            }
            BigDecimal lostAmount = safeBigDecimal(row[1]);
            long totalBets = row[2] != null ? ((Number) row[2]).longValue() : 0L;

            DashboardOverviewResponse.ChartPoint point = chartMap.get(date);
            point.setRevenue(lostAmount);
            point.setTotalBets(totalBets);
        }
    }

    private void populateTransactionChartData(Map<LocalDate, DashboardOverviewResponse.ChartPoint> chartMap,
                                              List<Transaction.TransactionStatus> statuses,
                                              LocalDateTime start,
                                              LocalDateTime end) {
        List<Object[]> stats = transactionRepository.getDailyTransactionStats(statuses, start, end);
        for (Object[] row : stats) {
            LocalDate date = toLocalDate(row[0]);
            if (date == null || !chartMap.containsKey(date)) {
                continue;
            }
            long txnCount = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            BigDecimal txnAmount = safeBigDecimal(row[2]);

            DashboardOverviewResponse.ChartPoint point = chartMap.get(date);
            point.setTransactions(txnCount);
            point.setTransactionAmount(txnAmount);
        }
    }

    private List<DashboardOverviewResponse.ActivityItem> buildRecentActivities(List<Transaction.TransactionStatus> statuses) {
        List<Transaction> transactions = transactionRepository.findRecentTransactionsByStatuses(
                statuses, PageRequest.of(0, DEFAULT_RECENT_ACTIVITY_LIMIT));
        List<Bet> bets = betRepository.findRecentBetsByStatuses(
                List.of(Bet.BetStatus.WON, Bet.BetStatus.LOST),
                PageRequest.of(0, DEFAULT_RECENT_ACTIVITY_LIMIT));
        List<SicboBet> sicboBets = sicboBetRepository.findRecentSettledBetsByStatuses(
                List.of(SicboBet.Status.WON, SicboBet.Status.LOST),
                PageRequest.of(0, DEFAULT_RECENT_ACTIVITY_LIMIT));
        List<XocDiaBet> xocDiaBets = xocDiaBetRepository.findRecentSettledBetsByStatuses(
                List.of(XocDiaBet.Status.WON, XocDiaBet.Status.LOST),
                PageRequest.of(0, DEFAULT_RECENT_ACTIVITY_LIMIT));

        var transactionActivities = transactions.stream()
                .map(this::mapTransactionToActivity);

        var betActivities = bets.stream()
                .map(this::mapBetToActivity);

        var sicboActivities = sicboBets.stream()
                .map(this::mapSicboBetToActivity);

        var xocDiaActivities = xocDiaBets.stream()
                .map(this::mapXocDiaBetToActivity);

        return Stream.of(transactionActivities, betActivities, sicboActivities, xocDiaActivities)
                .flatMap(stream -> stream)
                .sorted(Comparator.comparing(DashboardOverviewResponse.ActivityItem::getTime, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .limit(DEFAULT_RECENT_ACTIVITY_LIMIT)
                .collect(Collectors.toList());
    }

    private List<DashboardOverviewResponse.RecentUserItem> buildRecentUsers() {
        List<User> users = userRepository.findTop10RecentUsers(
                PageRequest.of(0, DEFAULT_RECENT_ACTIVITY_LIMIT));
        return users.stream()
                .map(user -> DashboardOverviewResponse.RecentUserItem.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .createdAt(user.getCreatedAt())
                        .status(user.getStatus().name())
                        .points(user.getPoints() != null ? BigDecimal.valueOf(user.getPoints()) : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());
    }

    private DashboardOverviewResponse.ActivityItem mapTransactionToActivity(Transaction transaction) {
        String description = String.format(Locale.getDefault(),
                "%s %s %s",
                transaction.getUser().getUsername(),
                transaction.getType().getDisplayName().toLowerCase(Locale.getDefault()),
                transaction.getStatus().getDisplayName().toLowerCase(Locale.getDefault()));

        return DashboardOverviewResponse.ActivityItem.builder()
                .id("txn-" + transaction.getId())
                .type("TRANSACTION")
                .username(transaction.getUser().getUsername())
                .description(description.trim())
                .amount(transaction.getNetAmount())
                .status(transaction.getStatus().name())
                .time(transaction.getUpdatedAt() != null ? transaction.getUpdatedAt() : transaction.getCreatedAt())
                .build();
    }

    private DashboardOverviewResponse.ActivityItem mapBetToActivity(Bet bet) {
        String resultText = bet.getStatus() == Bet.BetStatus.WON ? "thắng" : "thua";
        BigDecimal amount = bet.getStatus() == Bet.BetStatus.LOST
                ? bet.getTotalAmount()
                : bet.getWinAmount();
        String description = String.format(Locale.getDefault(),
                "Bet #%d %s (%s)", bet.getId(), resultText, bet.getBetType());

        return DashboardOverviewResponse.ActivityItem.builder()
                .id("bet-" + bet.getId())
                .type("BET")
                .username(bet.getUser().getUsername())
                .description(description)
                .betCode(bet.getBetType())
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .status(bet.getStatus().name())
                .time(bet.getResultCheckedAt() != null ? bet.getResultCheckedAt() : bet.getUpdatedAt())
                .build();
    }

    private DashboardOverviewResponse.ActivityItem mapSicboBetToActivity(SicboBet bet) {
        String resultText = bet.getStatus() == SicboBet.Status.WON ? "thắng" : "thua";
        BigDecimal amount = bet.getStatus() == SicboBet.Status.LOST
                ? bet.getStake()
                : bet.getWinAmount();
        String description = String.format(Locale.getDefault(),
                "Sicbo #%d %s (%s)", bet.getId(), resultText, bet.getBetCode());

        return DashboardOverviewResponse.ActivityItem.builder()
                .id("sicbo-" + bet.getId())
                .type("SICBO")
                .username(bet.getUser().getUsername())
                .description(description)
                .betCode(bet.getBetCode())
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .status(bet.getStatus().name())
                .time(bet.getSettledAt() != null ? LocalDateTime.ofInstant(bet.getSettledAt(), systemZone) : null)
                .build();
    }

    private DashboardOverviewResponse.ActivityItem mapXocDiaBetToActivity(XocDiaBet bet) {
        String resultText = bet.getStatus() == XocDiaBet.Status.WON ? "thắng" : "thua";
        BigDecimal amount = bet.getStatus() == XocDiaBet.Status.LOST
                ? bet.getStake()
                : bet.getWinAmount();
        String description = String.format(Locale.getDefault(),
                "Xóc Đĩa #%d %s (%s)", bet.getId(), resultText, bet.getBetCode());

        return DashboardOverviewResponse.ActivityItem.builder()
                .id("xocdia-" + bet.getId())
                .type("XOCDIA")
                .betCode(bet.getBetCode())
                .username(bet.getUser().getUsername())
                .description(description)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .status(bet.getStatus().name())
                .time(bet.getSettledAt() != null ? LocalDateTime.ofInstant(bet.getSettledAt(), systemZone) : null)
                .build();
    }

    private void populateSicboChartData(Map<LocalDate, DashboardOverviewResponse.ChartPoint> chartMap,
                                        LocalDateTime start,
                                        LocalDateTime end) {
        Instant startInstant = start.atZone(systemZone).toInstant();
        Instant endInstant = end.atZone(systemZone).toInstant();

        List<SicboBet> lostBets = sicboBetRepository.findByStatusAndSettledAtBetween(
                SicboBet.Status.LOST, startInstant, endInstant);
        addSicboBetsToChart(chartMap, lostBets, true);

        List<SicboBet> settledBets = sicboBetRepository.findByStatusInAndSettledAtBetween(
                List.of(SicboBet.Status.WON, SicboBet.Status.LOST),
                startInstant,
                endInstant
        );
        addSicboBetsToChart(chartMap, settledBets, false);
    }

    private void populateXocDiaChartData(Map<LocalDate, DashboardOverviewResponse.ChartPoint> chartMap,
                                         LocalDateTime start,
                                         LocalDateTime end) {
        Instant startInstant = start.atZone(systemZone).toInstant();
        Instant endInstant = end.atZone(systemZone).toInstant();

        List<XocDiaBet> lostBets = xocDiaBetRepository.findByStatusAndSettledAtBetween(
                XocDiaBet.Status.LOST, startInstant, endInstant);
        addXocDiaBetsToChart(chartMap, lostBets, true);

        List<XocDiaBet> settledBets = xocDiaBetRepository.findByStatusInAndSettledAtBetween(
                List.of(XocDiaBet.Status.WON, XocDiaBet.Status.LOST),
                startInstant,
                endInstant
        );
        addXocDiaBetsToChart(chartMap, settledBets, false);
    }

    private void addSicboBetsToChart(Map<LocalDate, DashboardOverviewResponse.ChartPoint> chartMap,
                                     List<SicboBet> bets,
                                     boolean addRevenue) {
        for (SicboBet bet : bets) {
            LocalDate date = bet.getSettledAt() != null ? toLocalDate(bet.getSettledAt()) : null;
            if (date == null || !chartMap.containsKey(date)) {
                continue;
            }
            DashboardOverviewResponse.ChartPoint point = chartMap.get(date);
            if (addRevenue) {
                point.setRevenue(point.getRevenue().add(safeBigDecimal(bet.getStake())));
            } else {
                point.setTotalBets(point.getTotalBets() + 1);
            }
        }
    }

    private void addXocDiaBetsToChart(Map<LocalDate, DashboardOverviewResponse.ChartPoint> chartMap,
                                      List<XocDiaBet> bets,
                                      boolean addRevenue) {
        for (XocDiaBet bet : bets) {
            LocalDate date = bet.getSettledAt() != null ? toLocalDate(bet.getSettledAt()) : null;
            if (date == null || !chartMap.containsKey(date)) {
                continue;
            }
            DashboardOverviewResponse.ChartPoint point = chartMap.get(date);
            if (addRevenue) {
                point.setRevenue(point.getRevenue().add(safeBigDecimal(bet.getStake())));
            } else {
                point.setTotalBets(point.getTotalBets() + 1);
            }
        }
    }

    private void populateWinLossRefundData(Map<LocalDate, DashboardOverviewResponse.ChartPoint> chartMap,
                                           LocalDateTime start,
                                           LocalDateTime end) {
        // Tính toán cho từng ngày trong chartMap
        for (Map.Entry<LocalDate, DashboardOverviewResponse.ChartPoint> entry : chartMap.entrySet()) {
            LocalDate date = entry.getKey();
            DashboardOverviewResponse.ChartPoint point = entry.getValue();
            
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay().minusNanos(1);
            Instant dayStartInstant = dayStart.atZone(systemZone).toInstant();
            Instant dayEndInstant = dayEnd.atZone(systemZone).toInstant();
            
            // Tính Win Profit (tiền thắng cược - không tính gốc)
            // Lottery: winAmount - totalAmount cho bets WON
            BigDecimal lotteryWinProfit = safeBigDecimal(
                    betRepository.sumWinProfitByFilters(dayStart, dayEnd));
            
            // Sicbo: winAmount - stake cho bets WON
            BigDecimal sicboWinProfit = safeBigDecimal(
                    sicboBetRepository.sumWinProfitByCreatedAtFilters(dayStartInstant, dayEndInstant));
            
            // XocDia: winAmount - stake cho bets WON
            BigDecimal xocDiaWinProfit = safeBigDecimal(
                    xocDiaBetRepository.sumWinProfitByCreatedAtFilters(dayStartInstant, dayEndInstant));
            
            BigDecimal totalWinProfit = lotteryWinProfit.add(sicboWinProfit).add(xocDiaWinProfit);
            
            // Tính Lost Stake (tiền thua cược) - đã có trong revenue, nhưng tính riêng để rõ ràng
            // Lottery: totalAmount cho bets LOST
            BigDecimal lotteryLostStake = safeBigDecimal(
                    betRepository.sumTotalAmountByStatusAndResultCheckedAtBetween(Bet.BetStatus.LOST, dayStart, dayEnd));
            
            // Sicbo: stake cho bets LOST
            BigDecimal sicboLostStake = safeBigDecimal(
                    sicboBetRepository.sumStakeByStatusAndSettledAtBetween(SicboBet.Status.LOST, dayStartInstant, dayEndInstant));
            
            // XocDia: stake cho bets LOST
            BigDecimal xocDiaLostStake = safeBigDecimal(
                    xocDiaBetRepository.sumStakeByStatusAndSettledAtBetween(XocDiaBet.Status.LOST, dayStartInstant, dayEndInstant));
            
            BigDecimal totalLostStake = lotteryLostStake.add(sicboLostStake).add(xocDiaLostStake);
            
            // Tính Total Refund (tổng tiền hoàn)
            // Game refund accrual (scheduled refund)
            BigDecimal scheduledRefund = safeBigDecimal(
                    gameRefundAccrualRepository.sumPaidRefundByDateRange(dayStartInstant, dayEndInstant));
            
            // Instant game refund
            BigDecimal instantRefund = safeBigDecimal(
                    pointTransactionRepository.sumInstantGameRefundByDateRange(dayStart, dayEnd));
            
            // Daily loss refund
            BigDecimal dailyLossRefund = safeBigDecimal(
                    dailyLossRefundRepository.sumPaidRefundByDateRange(dayStartInstant, dayEndInstant));
            
            BigDecimal totalRefund = scheduledRefund.add(instantRefund).add(dailyLossRefund);
            
            // Set vào chart point
            point.setWinProfit(totalWinProfit);
            point.setLostStake(totalLostStake);
            point.setTotalRefund(totalRefund);
        }
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof Instant instant) {
            return instant.atZone(systemZone).toLocalDate();
        }
        return null;
    }

    private BigDecimal safeBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return BigDecimal.ZERO;
    }
}


