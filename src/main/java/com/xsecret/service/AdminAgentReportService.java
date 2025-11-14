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
import com.xsecret.repository.AgentCommissionPayoutRepository;
import com.xsecret.repository.BetRepository;
import com.xsecret.repository.SicboBetRepository;
import com.xsecret.repository.UserRepository;
import com.xsecret.repository.XocDiaBetRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAgentReportService {

    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final XocDiaBetRepository xocDiaBetRepository;
    private final SicboBetRepository sicboBetRepository;
    private final AgentCommissionPayoutRepository agentCommissionPayoutRepository;
    private final SystemSettingsService systemSettingsService;
    private final PointService pointService;
    private final UserService userService;

    @Transactional
    public AdminAgentCommissionReportResponse getMonthlyReport(YearMonth month) {
        YearMonth targetMonth = month != null ? month : YearMonth.now();
        LocalDateTime startDateTime = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime endDateTime = targetMonth.atEndOfMonth().atTime(LocalTime.MAX);
        Instant startInstant = startDateTime.atZone(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDateTime.atZone(ZoneId.systemDefault()).toInstant();

        double commissionRate = systemSettingsService.getAgentCommissionPercentage();
        BigDecimal commissionMultiplier = BigDecimal.valueOf(commissionRate)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

        List<User> agents = userRepository.findByStaffRole(User.StaffRole.AGENT);
        Map<Long, AgentCommissionPayout> payoutMap = loadPayouts(targetMonth);

        List<AdminAgentCommissionReportRowResponse> rows = new ArrayList<>();

        BigDecimal totalBetAmount = BigDecimal.ZERO;
        BigDecimal totalLostAmount = BigDecimal.ZERO;
        BigDecimal totalCalculatedCommission = BigDecimal.ZERO;
        BigDecimal totalPaidCommission = BigDecimal.ZERO;
        BigDecimal totalPendingDifference = BigDecimal.ZERO;
        long totalCustomers = 0L;

        for (User agent : agents) {
            AgentMonthlyStats stats = computeAgentStats(agent, startDateTime, endDateTime, startInstant, endInstant, commissionMultiplier);
            AgentCommissionPayout payout = payoutMap.get(agent.getId());

            BigDecimal paidCommission = payout != null ? safe(payout.getCommissionAmount()) : BigDecimal.ZERO;
            BigDecimal pendingDifference = stats.commissionAmount.subtract(paidCommission);

            AdminAgentCommissionReportRowResponse row = AdminAgentCommissionReportRowResponse.builder()
                    .agentId(agent.getId())
                    .username(agent.getUsername())
                    .fullName(agent.getFullName())
                    .referralCode(agent.getReferralCode())
                    .customerCount(stats.customerCount)
                    .totalBetAmount(stats.totalBetAmount)
                    .totalLostAmount(stats.totalLostAmount)
                    .calculatedCommissionAmount(stats.commissionAmount)
                    .payoutStatus(payout != null ? payout.getStatus() : AgentCommissionPayout.Status.PENDING)
                    .paidCommissionAmount(paidCommission)
                    .paidAt(payout != null ? payout.getPaidAt() : null)
                    .payoutId(payout != null ? payout.getId() : null)
                    .payoutNote(payout != null ? payout.getNotes() : null)
                    .commissionRate(commissionRate)
                    .canPayout(stats.commissionAmount.compareTo(BigDecimal.ZERO) > 0
                            && (payout == null || payout.getStatus() != AgentCommissionPayout.Status.PAID))
                    .pendingDifference(pendingDifference)
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

        if (stats.commissionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Hoa hồng tháng " + targetMonth + " của đại lý "
                    + agent.getUsername() + " bằng 0, không thể chia.");
        }

        AgentCommissionPayout payout = agentCommissionPayoutRepository
                .findByAgentAndPeriodMonth(agent, targetMonth.toString())
                .orElseGet(() -> AgentCommissionPayout.builder()
                        .agent(agent)
                        .periodMonth(targetMonth.toString())
                        .periodStart(startDateTime.toLocalDate())
                        .periodEnd(endDateTime.toLocalDate())
                        .build());

        if (payout.getStatus() == AgentCommissionPayout.Status.PAID) {
            throw new IllegalStateException("Hoa hồng tháng " + targetMonth + " đã được chia cho đại lý này.");
        }

        payout.setAgent(agent);
        payout.setPeriodMonth(targetMonth.toString());
        payout.setPeriodStart(startDateTime.toLocalDate());
        payout.setPeriodEnd(endDateTime.toLocalDate());
        payout.setTotalLostAmount(stats.totalLostAmount.setScale(0, RoundingMode.HALF_UP));
        payout.setCommissionAmount(stats.commissionAmount);
        payout.setStatus(AgentCommissionPayout.Status.PAID);
        payout.setPaidAt(LocalDateTime.now());
        payout.setNotes(request.getNote());

        AgentCommissionPayout saved = agentCommissionPayoutRepository.save(payout);

        pointService.addCommissionToAgent(
                agent,
                stats.commissionAmount,
                String.format(Locale.ROOT, "Chia hoa hồng tháng %s", targetMonth),
                "AGENT_COMMISSION",
                saved.getId(),
                admin,
                PointTransaction.PointTransactionType.ADMIN_ADD
        );

        return AdminAgentCommissionReportRowResponse.builder()
                .agentId(agent.getId())
                .username(agent.getUsername())
                .fullName(agent.getFullName())
                .referralCode(agent.getReferralCode())
                .customerCount(stats.customerCount)
                .totalBetAmount(stats.totalBetAmount)
                .totalLostAmount(stats.totalLostAmount)
                .calculatedCommissionAmount(stats.commissionAmount)
                .payoutStatus(saved.getStatus())
                .paidCommissionAmount(saved.getCommissionAmount())
                .paidAt(saved.getPaidAt())
                .payoutId(saved.getId())
                .payoutNote(saved.getNotes())
                .commissionRate(commissionRate)
                .canPayout(false)
                .pendingDifference(BigDecimal.ZERO)
                .build();
    }

    private Map<Long, AgentCommissionPayout> loadPayouts(YearMonth targetMonth) {
        Map<Long, AgentCommissionPayout> payoutMap = new HashMap<>();
        agentCommissionPayoutRepository.findByPeriodMonth(targetMonth.toString())
                .forEach(payout -> payoutMap.put(payout.getAgent().getId(), payout));
        return payoutMap;
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

