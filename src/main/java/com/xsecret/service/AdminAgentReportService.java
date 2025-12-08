package com.xsecret.service;

import com.xsecret.dto.request.AdminAgentCommissionPayoutRequest;
import com.xsecret.dto.response.AdminAgentCommissionReportResponse;
import com.xsecret.dto.response.AdminAgentCommissionReportRowResponse;
import com.xsecret.entity.AgentCommissionPayout;
import com.xsecret.entity.Bet;
import com.xsecret.entity.PointTransaction;
import com.xsecret.entity.SicboBet;
import com.xsecret.entity.User;
import com.xsecret.entity.XocDiaBet;
import com.xsecret.entity.Transaction;
import com.xsecret.repository.AgentCommissionPayoutRepository;
import com.xsecret.repository.AgentNoteRepository;
import com.xsecret.repository.BetRepository;
import com.xsecret.repository.DailyLossRefundRepository;
import com.xsecret.repository.GameRefundAccrualRepository;
import com.xsecret.repository.PromotionalMoneyRepository;
import com.xsecret.repository.SicboBetRepository;
import com.xsecret.repository.TransactionRepository;
import com.xsecret.repository.UserLoginHistoryRepository;
import com.xsecret.repository.UserRepository;
import com.xsecret.repository.XocDiaBetRepository;
import com.xsecret.entity.AgentNote;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAgentReportService {

    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final XocDiaBetRepository xocDiaBetRepository;
    private final SicboBetRepository sicboBetRepository;
    private final AgentCommissionPayoutRepository agentCommissionPayoutRepository;
    private final AgentNoteRepository agentNoteRepository;
    private final TransactionRepository transactionRepository;
    private final UserLoginHistoryRepository userLoginHistoryRepository;
    private final GameRefundAccrualRepository gameRefundAccrualRepository;
    private final DailyLossRefundRepository dailyLossRefundRepository;
    private final PromotionalMoneyRepository promotionalMoneyRepository;
    private final SystemSettingsService systemSettingsService;
    private final PointService pointService;
    private final UserService userService;

    private static final List<Transaction.TransactionStatus> SUCCESS_DEPOSIT_STATUSES = List.of(
            Transaction.TransactionStatus.APPROVED,
            Transaction.TransactionStatus.COMPLETED
    );

    private static final List<Transaction.TransactionStatus> SUCCESS_WITHDRAW_STATUSES = List.of(
            Transaction.TransactionStatus.APPROVED,
            Transaction.TransactionStatus.COMPLETED
    );

    @Transactional
    public AdminAgentCommissionReportResponse getMonthlyReport(YearMonth month, String ipSearch) {
        YearMonth targetMonth = month != null ? month : YearMonth.now();
        LocalDateTime startDateTime = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime endDateTime = targetMonth.atEndOfMonth().atTime(LocalTime.MAX);
        Instant startInstant = startDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDateTime.atZone(ZoneId.systemDefault()).toInstant();

        double commissionRate = systemSettingsService.getAgentCommissionPercentage();
        BigDecimal commissionMultiplier = BigDecimal.valueOf(commissionRate)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

        List<User> agents = userRepository.findByStaffRole(User.StaffRole.AGENT);
        
        // Filter by IP if provided
        if (ipSearch != null && !ipSearch.trim().isEmpty()) {
            agents = filterAgentsByIp(agents, ipSearch.trim());
        }
        
        Map<Long, BigDecimal> payoutMap = loadPayouts(targetMonth);
        Map<Long, String> noteMap = loadNotes(targetMonth);

        List<AdminAgentCommissionReportRowResponse> rows = new ArrayList<>();

        BigDecimal totalBetAmount = BigDecimal.ZERO;
        BigDecimal totalLostAmount = BigDecimal.ZERO;
        BigDecimal totalCalculatedCommission = BigDecimal.ZERO;
        BigDecimal totalPaidCommission = BigDecimal.ZERO;
        BigDecimal totalPendingDifference = BigDecimal.ZERO;
        long totalCustomers = 0L;

        for (User agent : agents) {
            AgentMonthlyStats stats = computeAgentStats(agent, startDateTime, endDateTime, startInstant, endInstant, commissionMultiplier);
            // Tổng hoa hồng đã chia (có thể chia nhiều lần trong tháng)
            BigDecimal paidCommission = payoutMap.getOrDefault(agent.getId(), BigDecimal.ZERO);
            BigDecimal pendingDifference = stats.commissionAmount.subtract(paidCommission);
            
            // Lấy danh sách khách hàng của đại lý
            List<User> customers = Optional.ofNullable(agent.getReferralCode())
                    .map(code -> userRepository.findByInvitedByCodeIgnoreCase(code))
                    .orElse(List.of());
            
            // Tính tổng nạp và tổng rút của TẤT CẢ khách hàng của đại lý
            BigDecimal totalDeposit = BigDecimal.ZERO;
            BigDecimal totalWithdraw = BigDecimal.ZERO;
            BigDecimal totalDailyLossRefund = BigDecimal.ZERO;
            BigDecimal totalRefund = BigDecimal.ZERO;
            BigDecimal totalPromotionalMoney = BigDecimal.ZERO;
            
            for (User customer : customers) {
                totalDeposit = totalDeposit.add(safe(transactionRepository.sumDepositAmountByUserAndStatuses(customer, SUCCESS_DEPOSIT_STATUSES)));
                totalWithdraw = totalWithdraw.add(safe(transactionRepository.sumWithdrawAmountByUserAndStatuses(customer, SUCCESS_WITHDRAW_STATUSES)));
                totalDailyLossRefund = totalDailyLossRefund.add(safe(dailyLossRefundRepository.sumPaidRefundByUser(customer)));
                totalRefund = totalRefund.add(safe(gameRefundAccrualRepository.sumPaidLossRefundByUser(customer)));
                totalPromotionalMoney = totalPromotionalMoney.add(safe(promotionalMoneyRepository.sumAmountByUser(customer)));
            }
            
            // Tính số dư cuối: (Tổng thua) - [(Tổng hoàn thua) + (Tổng hoàn cược) + (Tổng KM)]
            BigDecimal refundsTotal = totalDailyLossRefund.add(totalRefund).add(totalPromotionalMoney);
            BigDecimal finalBalance = stats.totalLostAmount.subtract(refundsTotal);
            
            // Lấy IP lần đầu tiên đăng nhập
            List<String> firstLoginIps = userLoginHistoryRepository.findFirstLoginIpByUser(agent, PageRequest.of(0, 1));
            String firstLoginIp = firstLoginIps.isEmpty() ? null : firstLoginIps.get(0);

            AdminAgentCommissionReportRowResponse row = AdminAgentCommissionReportRowResponse.builder()
                    .agentId(agent.getId())
                    .username(agent.getUsername())
                    .fullName(agent.getFullName())
                    .referralCode(agent.getReferralCode())
                    .customerCount(stats.customerCount)
                    .totalBetAmount(stats.totalBetAmount)
                    .totalLostAmount(stats.totalLostAmount)
                    .calculatedCommissionAmount(stats.commissionAmount)
                    .totalDepositAmount(totalDeposit)
                    .totalWithdrawAmount(totalWithdraw)
                    .totalDailyLossRefund(totalDailyLossRefund)
                    .totalRefund(totalRefund)
                    .totalPromotionalMoney(totalPromotionalMoney)
                    .finalBalance(finalBalance)
                    .firstLoginIp(firstLoginIp)
                    .payoutStatus(paidCommission.compareTo(BigDecimal.ZERO) > 0 ? AgentCommissionPayout.Status.PAID : AgentCommissionPayout.Status.PENDING)
                    .paidCommissionAmount(paidCommission)
                    .paidAt(null) // Không có paidAt cụ thể vì có thể có nhiều payouts
                    .payoutId(null) // Không có payoutId cụ thể vì có thể có nhiều payouts
                    .payoutNote(null) // Không có note cụ thể vì có thể có nhiều payouts
                    .customCommissionAmount(null) // Không có customCommissionAmount cụ thể
                    .commissionRate(commissionRate)
                    .canPayout(true) // Luôn cho phép chia (nếu đã điền hoa hồng)
                    .pendingDifference(pendingDifference)
                    .agentNote(noteMap.getOrDefault(agent.getId(), null))
                    .build();

            rows.add(row);

            totalBetAmount = totalBetAmount.add(stats.totalBetAmount);
            totalLostAmount = totalLostAmount.add(stats.totalLostAmount);
            totalCalculatedCommission = totalCalculatedCommission.add(stats.commissionAmount);
            totalPaidCommission = totalPaidCommission.add(paidCommission);
            if (pendingDifference.compareTo(BigDecimal.ZERO) > 0) {
                totalPendingDifference = totalPendingDifference.add(pendingDifference);
            }
            totalCustomers += stats.customerCount;
        }

        return AdminAgentCommissionReportResponse.builder()
                .month(targetMonth.toString())
                .totalAgents(agents.size())
                .totalCustomers(totalCustomers)
                .totalBetAmount(totalBetAmount)
                .totalLostAmount(totalLostAmount)
                .totalCalculatedCommission(totalCalculatedCommission)
                .totalPaidCommission(totalPaidCommission)
                .totalPendingCommission(totalPendingDifference)
                .agents(rows)
                .build();
    }

    @Transactional
    public AdminAgentCommissionReportRowResponse payoutCommission(
            Long agentId,
            AdminAgentCommissionPayoutRequest request,
            Long adminId
    ) {
        YearMonth targetMonth = parseYearMonth(request.getMonth());
        User agent = userService.getUserById(agentId);
        if (agent.getStaffRole() != User.StaffRole.AGENT) {
            throw new IllegalStateException("Người dùng không phải đại lý.");
        }

        User admin = userService.getUserById(adminId);

        LocalDateTime startDateTime = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime endDateTime = targetMonth.atEndOfMonth().atTime(LocalTime.MAX);
        Instant startInstant = startDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDateTime.atZone(ZoneId.systemDefault()).toInstant();

        double commissionRate = systemSettingsService.getAgentCommissionPercentage();
        BigDecimal commissionMultiplier = BigDecimal.valueOf(commissionRate)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

        AgentMonthlyStats stats = computeAgentStats(agent, startDateTime, endDateTime, startInstant, endInstant, commissionMultiplier);

        // Kiểm tra customCommissionAmount - bắt buộc phải có
        if (request.getCustomCommissionAmount() == null || request.getCustomCommissionAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Vui lòng nhập số tiền hoa hồng để chia.");
        }

        // Luôn tạo payout mới mỗi lần chia (cho phép chia nhiều lần trong cùng một tháng)
        BigDecimal commissionToPay = request.getCustomCommissionAmount();
        AgentCommissionPayout payout = AgentCommissionPayout.builder()
                .agent(agent)
                .periodMonth(targetMonth.toString())
                .periodStart(startDateTime.toLocalDate())
                .periodEnd(endDateTime.toLocalDate())
                .totalLostAmount(stats.totalLostAmount.setScale(0, RoundingMode.HALF_UP))
                .commissionAmount(commissionToPay)
                .status(AgentCommissionPayout.Status.PAID)
                .paidAt(LocalDateTime.now())
                .notes(request.getNote())
                .build();

        AgentCommissionPayout saved = agentCommissionPayoutRepository.save(payout);

        pointService.addCommissionToAgent(
                agent,
                commissionToPay,
                String.format(Locale.ROOT, "Chia hoa hồng tháng %s", targetMonth),
                "AGENT_COMMISSION",
                saved.getId(),
                admin,
                PointTransaction.PointTransactionType.ADMIN_ADD
        );

        // Tính lại tổng hoa hồng đã chia (bao gồm cả payout vừa tạo)
        BigDecimal totalPaidCommission = safe(agentCommissionPayoutRepository.sumPaidCommissionByAgentAndMonth(
                agent, targetMonth.toString()));

        // Tính lại các trường mới cho response
        List<User> customers = Optional.ofNullable(agent.getReferralCode())
                .map(code -> userRepository.findByInvitedByCodeIgnoreCase(code))
                .orElse(List.of());
        
        // Tính tổng nạp và tổng rút của TẤT CẢ khách hàng của đại lý
        BigDecimal totalDeposit = BigDecimal.ZERO;
        BigDecimal totalWithdraw = BigDecimal.ZERO;
        BigDecimal totalDailyLossRefund = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalPromotionalMoney = BigDecimal.ZERO;
        
        for (User customer : customers) {
            totalDeposit = totalDeposit.add(safe(transactionRepository.sumDepositAmountByUserAndStatuses(customer, SUCCESS_DEPOSIT_STATUSES)));
            totalWithdraw = totalWithdraw.add(safe(transactionRepository.sumWithdrawAmountByUserAndStatuses(customer, SUCCESS_WITHDRAW_STATUSES)));
            totalDailyLossRefund = totalDailyLossRefund.add(safe(dailyLossRefundRepository.sumPaidRefundByUser(customer)));
            totalRefund = totalRefund.add(safe(gameRefundAccrualRepository.sumPaidLossRefundByUser(customer)));
            totalPromotionalMoney = totalPromotionalMoney.add(safe(promotionalMoneyRepository.sumAmountByUser(customer)));
        }
        
        BigDecimal refundsTotal = totalDailyLossRefund.add(totalRefund).add(totalPromotionalMoney);
        BigDecimal finalBalance = stats.totalLostAmount.subtract(refundsTotal);
        
        List<String> firstLoginIps = userLoginHistoryRepository.findFirstLoginIpByUser(agent, PageRequest.of(0, 1));
        String firstLoginIp = firstLoginIps.isEmpty() ? null : firstLoginIps.get(0);
        
        BigDecimal pendingDifference = stats.commissionAmount.subtract(totalPaidCommission);

        return AdminAgentCommissionReportRowResponse.builder()
                .agentId(agent.getId())
                .username(agent.getUsername())
                .fullName(agent.getFullName())
                .referralCode(agent.getReferralCode())
                .customerCount(stats.customerCount)
                .totalBetAmount(stats.totalBetAmount)
                .totalLostAmount(stats.totalLostAmount)
                .calculatedCommissionAmount(stats.commissionAmount)
                .totalDepositAmount(totalDeposit)
                .totalWithdrawAmount(totalWithdraw)
                .totalDailyLossRefund(totalDailyLossRefund)
                .totalRefund(totalRefund)
                .totalPromotionalMoney(totalPromotionalMoney)
                .finalBalance(finalBalance)
                .firstLoginIp(firstLoginIp)
                .payoutStatus(totalPaidCommission.compareTo(BigDecimal.ZERO) > 0 ? AgentCommissionPayout.Status.PAID : AgentCommissionPayout.Status.PENDING)
                .paidCommissionAmount(totalPaidCommission) // Tổng tất cả payouts đã chia
                .paidAt(saved.getPaidAt()) // Thời gian chia lần cuối
                .payoutId(null) // Không có payoutId cụ thể vì có thể có nhiều payouts
                .payoutNote(null) // Không có note cụ thể vì có thể có nhiều payouts
                .customCommissionAmount(null) // Không có customCommissionAmount cụ thể
                .commissionRate(commissionRate)
                .canPayout(true) // Luôn cho phép chia tiếp
                .pendingDifference(pendingDifference)
                .build();
    }

    public List<AgentCommissionPayout> getAgentPayoutHistory(Long agentId, String periodMonth) {
        User agent = userService.getUserById(agentId);
        if (agent.getStaffRole() != User.StaffRole.AGENT) {
            throw new IllegalStateException("Người dùng không phải đại lý.");
        }
        if (periodMonth != null && !periodMonth.trim().isEmpty()) {
            // Lấy payouts của agent trong tháng cụ thể
            return agentCommissionPayoutRepository.findByAgentAndPeriodMonthOrderByPaidAtDesc(agent, periodMonth);
        } else {
            // Lấy tất cả payouts của agent
            return agentCommissionPayoutRepository.findByAgentOrderByPeriodStartDesc(agent, PageRequest.of(0, 100)).getContent();
        }
    }

    private Map<Long, BigDecimal> loadPayouts(YearMonth targetMonth) {
        // Tính tổng hoa hồng đã chia (PAID) cho mỗi agent trong tháng
        Map<Long, BigDecimal> payoutMap = new HashMap<>();
        List<User> agents = userRepository.findByStaffRole(User.StaffRole.AGENT);
        for (User agent : agents) {
            BigDecimal totalPaid = safe(agentCommissionPayoutRepository.sumPaidCommissionByAgentAndMonth(
                    agent, targetMonth.toString()));
            if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                payoutMap.put(agent.getId(), totalPaid);
            }
        }
        return payoutMap;
    }

    private Map<Long, String> loadNotes(YearMonth targetMonth) {
        // Load ghi chú cho mỗi agent trong tháng
        Map<Long, String> noteMap = new HashMap<>();
        List<User> agents = userRepository.findByStaffRole(User.StaffRole.AGENT);
        for (User agent : agents) {
            Optional<AgentNote> noteOpt = agentNoteRepository.findByAgentAndPeriodMonth(agent, targetMonth.toString());
            if (noteOpt.isPresent() && noteOpt.get().getNote() != null) {
                noteMap.put(agent.getId(), noteOpt.get().getNote());
            }
        }
        return noteMap;
    }

    @Transactional
    public void saveAgentNote(Long agentId, String periodMonth, String note) {
        User agent = userService.getUserById(agentId);
        if (agent.getStaffRole() != User.StaffRole.AGENT) {
            throw new IllegalStateException("Người dùng không phải đại lý.");
        }

        AgentNote agentNote = agentNoteRepository.findByAgentAndPeriodMonth(agent, periodMonth)
                .orElseGet(() -> AgentNote.builder()
                        .agent(agent)
                        .periodMonth(periodMonth)
                        .build());

        agentNote.setNote(note != null && !note.trim().isEmpty() ? note.trim() : null);
        agentNoteRepository.save(agentNote);
        log.info("Saved note for agent {} in month {}", agent.getUsername(), periodMonth);
    }

    private AgentMonthlyStats computeAgentStats(
            User agent,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Instant startInstant,
            Instant endInstant,
            BigDecimal commissionMultiplier
    ) {
        List<User> customers = Optional.ofNullable(agent.getReferralCode())
                .map(code -> userRepository.findByInvitedByCodeIgnoreCase(code))
                .orElse(List.of());

        if (customers.isEmpty()) {
            return AgentMonthlyStats.empty();
        }

        List<Long> customerIds = customers.stream().map(User::getId).toList();

        BigDecimal totalBet = BigDecimal.ZERO;
        BigDecimal totalLost = BigDecimal.ZERO;

        for (Object[] row : betRepository.aggregateTotalsByUsers(
                customerIds,
                startDateTime,
                endDateTime,
                Bet.BetStatus.CANCELLED,
                Bet.BetStatus.LOST
        )) {
            totalBet = totalBet.add(safe(row[1]));
            totalLost = totalLost.add(safe(row[2]));
        }

        for (Object[] row : xocDiaBetRepository.aggregateTotalsByUsers(
                customerIds,
                startInstant,
                endInstant,
                XocDiaBet.Status.REFUNDED,
                XocDiaBet.Status.LOST
        )) {
            totalBet = totalBet.add(safe(row[1]));
            totalLost = totalLost.add(safe(row[2]));
        }

        for (Object[] row : sicboBetRepository.aggregateTotalsByUsers(
                customerIds,
                startInstant,
                endInstant,
                SicboBet.Status.REFUNDED,
                SicboBet.Status.LOST
        )) {
            totalBet = totalBet.add(safe(row[1]));
            totalLost = totalLost.add(safe(row[2]));
        }

        BigDecimal commissionAmount = totalLost.multiply(commissionMultiplier)
                .setScale(0, RoundingMode.HALF_UP);

        return new AgentMonthlyStats(
                totalBet,
                totalLost,
                commissionAmount,
                customers.size()
        );
    }

    private YearMonth parseYearMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        return YearMonth.parse(month.trim());
    }

    private List<User> filterAgentsByIp(List<User> agents, String ipSearch) {
        return agents.stream()
                .filter(agent -> {
                    List<String> firstLoginIps = userLoginHistoryRepository.findFirstLoginIpByUser(agent, PageRequest.of(0, 1));
                    String firstLoginIp = firstLoginIps.isEmpty() ? null : firstLoginIps.get(0);
                    return firstLoginIp != null && firstLoginIp.contains(ipSearch);
                })
                .toList();
    }

    private BigDecimal safe(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(value.toString());
    }

    private record AgentMonthlyStats(
            BigDecimal totalBetAmount,
            BigDecimal totalLostAmount,
            BigDecimal commissionAmount,
            long customerCount
    ) {
        private static AgentMonthlyStats empty() {
            return new AgentMonthlyStats(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }
    }
}

