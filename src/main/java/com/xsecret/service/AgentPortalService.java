package com.xsecret.service;

import com.xsecret.dto.response.AgentCommissionChartPointResponse;
import com.xsecret.dto.response.AgentCommissionCustomerSummaryResponse;
import com.xsecret.dto.response.AgentCommissionPayoutResponse;
import com.xsecret.dto.response.AgentCommissionSummaryResponse;
import com.xsecret.dto.response.AgentCustomerBetHistoryItemResponse;
import com.xsecret.dto.response.AgentCustomerBetHistoryResponse;
import com.xsecret.dto.response.AgentCustomerListResponse;
import com.xsecret.dto.response.AgentCustomerSummaryResponse;
import com.xsecret.dto.response.AgentDashboardSummaryResponse;
import com.xsecret.dto.response.AgentInviteInfoResponse;
import com.xsecret.dto.response.AgentInviteReferralResponse;
import com.xsecret.entity.AgentCommissionPayout;
import com.xsecret.entity.User;
import com.xsecret.repository.AgentCommissionPayoutRepository;
import com.xsecret.repository.BetRepository;
import com.xsecret.repository.SicboBetRepository;
import com.xsecret.repository.UserRepository;
import com.xsecret.repository.XocDiaBetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentPortalService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final BetRepository betRepository;
    private final XocDiaBetRepository xocDiaBetRepository;
    private final SicboBetRepository sicboBetRepository;
    private final AgentCommissionPayoutRepository agentCommissionPayoutRepository;
    private final SystemSettingsService systemSettingsService;

    @Transactional(readOnly = true)
    public AgentCustomerListResponse getAgentCustomers(
            Long agentId,
            String searchTerm,
            User.UserStatus status,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size
    ) {
        User agent = userService.getUserById(agentId);
        if (agent.getStaffRole() != User.StaffRole.AGENT) {
            throw new AccessDeniedException("Chỉ đại lý mới được phép truy cập danh sách khách hàng.");
        }

        String referralCode = agent.getReferralCode();
        if (referralCode == null || referralCode.isBlank()) {
            throw new IllegalStateException("Đại lý chưa được cấp mã giới thiệu, không thể tải danh sách khách hàng.");
        }

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));

        Page<User> resultPage = userRepository.findAgentCustomers(
                referralCode,
                status,
                searchTerm != null && !searchTerm.isBlank() ? searchTerm.trim() : null,
                pageable
        );

        List<User> customers = resultPage.getContent();
        List<Long> userIds = customers.stream().map(User::getId).toList();

        Map<Long, BigDecimal> totalBetMap = new HashMap<>();
        Map<Long, BigDecimal> totalLostMap = new HashMap<>();

        if (!userIds.isEmpty()) {
            // Lottery bets
            List<Object[]> betAggregates = betRepository.aggregateTotalsByUsers(userIds, startDateTime, endDateTime);
            for (Object[] row : betAggregates) {
                Long userId = (Long) row[0];
                accumulate(totalBetMap, userId, toBigDecimal(row[1]));
                accumulate(totalLostMap, userId, toBigDecimal(row[2]));
            }

            Instant startInstant = startDateTime != null ? startDateTime.atZone(ZoneId.systemDefault()).toInstant() : null;
            Instant endInstant = endDateTime != null ? endDateTime.atZone(ZoneId.systemDefault()).toInstant() : null;

            List<Object[]> xocDiaAggregates = xocDiaBetRepository.aggregateTotalsByUsers(userIds, startInstant, endInstant);
            for (Object[] row : xocDiaAggregates) {
                Long userId = (Long) row[0];
                accumulate(totalBetMap, userId, toBigDecimal(row[1]));
                accumulate(totalLostMap, userId, toBigDecimal(row[2]));
            }

            List<Object[]> sicboAggregates = sicboBetRepository.aggregateTotalsByUsers(userIds, startInstant, endInstant);
            for (Object[] row : sicboAggregates) {
                Long userId = (Long) row[0];
                accumulate(totalBetMap, userId, toBigDecimal(row[1]));
                accumulate(totalLostMap, userId, toBigDecimal(row[2]));
            }
        }

        double commissionRate = systemSettingsService.getAgentCommissionPercentage();
        BigDecimal commissionMultiplier = BigDecimal.valueOf(commissionRate)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

        List<AgentCustomerSummaryResponse> items = customers.stream()
                .map(customer -> {
                    Long userId = customer.getId();
                    BigDecimal totalBet = totalBetMap.getOrDefault(userId, BigDecimal.ZERO);
                    BigDecimal totalLost = totalLostMap.getOrDefault(userId, BigDecimal.ZERO);
                    BigDecimal commissionAmount = totalLost.multiply(commissionMultiplier)
                            .setScale(0, RoundingMode.HALF_UP);

                    AgentCustomerSummaryResponse response = new AgentCustomerSummaryResponse();
                    response.setId(userId);
                    response.setUsername(customer.getUsername());
                    response.setStatus(customer.getStatus());
                    response.setJoinedAt(customer.getCreatedAt());
                    response.setTotalBetAmount(totalBet);
                    response.setTotalLostAmount(totalLost);
                    response.setCommissionAmount(commissionAmount);
                    return response;
                })
                .collect(Collectors.toList());

        AgentCustomerListResponse listResponse = new AgentCustomerListResponse();
        listResponse.setItems(items);
        listResponse.setTotalItems(resultPage.getTotalElements());
        listResponse.setTotalPages(resultPage.getTotalPages());
        listResponse.setPage(resultPage.getNumber());
        listResponse.setSize(resultPage.getSize());
        listResponse.setCommissionRate(commissionRate);
        return listResponse;
    }

    @Transactional(readOnly = true)
    public AgentCustomerBetHistoryResponse getCustomerBetHistory(
            Long agentId,
            Long customerId,
            String gameType,
            LocalDate startDate,
            LocalDate endDate,
            int size
    ) {
        User agent = userService.getUserById(agentId);
        if (agent.getStaffRole() != User.StaffRole.AGENT) {
            throw new AccessDeniedException("Chỉ đại lý mới được phép truy cập dữ liệu khách hàng.");
        }

        User customer = userService.getUserById(customerId);
        String agentReferral = agent.getReferralCode();
        if (agentReferral == null || agentReferral.isBlank()
                || customer.getInvitedByCode() == null
                || !agentReferral.equalsIgnoreCase(customer.getInvitedByCode())) {
            throw new AccessDeniedException("Người dùng này không thuộc đại lý của bạn.");
        }

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        size = Math.max(1, Math.min(size, 200));

        List<AgentCustomerBetHistoryItemResponse> items = new ArrayList<>();

        boolean includeLottery = gameType == null || "lottery".equalsIgnoreCase(gameType) || "all".equalsIgnoreCase(gameType);
        boolean includeXocDia = gameType == null || "xocdia".equalsIgnoreCase(gameType) || "xoc-dia".equalsIgnoreCase(gameType) || "all".equalsIgnoreCase(gameType);
        boolean includeSicbo = gameType == null || "sicbo".equalsIgnoreCase(gameType) || "all".equalsIgnoreCase(gameType);

        if (includeLottery) {
            betRepository.findRecentBetsByUserIdAndDateRange(
                    customerId,
                    startDateTime,
                    endDateTime,
                    PageRequest.of(0, size)
            ).forEach(bet -> {
                BigDecimal stake = bet.getTotalAmount() != null ? bet.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal winAmount = bet.getWinAmount() != null ? bet.getWinAmount() : BigDecimal.ZERO;
                BigDecimal net = winAmount.subtract(stake);
                AgentCustomerBetHistoryItemResponse item = new AgentCustomerBetHistoryItemResponse();
                item.setBetId(bet.getId());
                item.setGameType("lottery");
                item.setBetCode(bet.getBetType());
                item.setStake(stake);
                item.setWinAmount(winAmount);
                item.setNetResult(net);
                item.setStatus(bet.getStatus().name());
                item.setPlacedAt(bet.getCreatedAt());
                items.add(item);
            });
        }

        Instant startInstant = startDateTime != null ? startDateTime.atZone(ZoneId.systemDefault()).toInstant() : null;
        Instant endInstant = endDateTime != null ? endDateTime.atZone(ZoneId.systemDefault()).toInstant() : null;

        if (includeXocDia) {
            xocDiaBetRepository.findRecentBetsByUserIdAndDateRange(
                    customerId,
                    startInstant,
                    endInstant,
                    PageRequest.of(0, size)
            ).forEach(bet -> {
                BigDecimal stake = bet.getStake() != null ? bet.getStake() : BigDecimal.ZERO;
                BigDecimal winAmount = bet.getWinAmount() != null ? bet.getWinAmount() : BigDecimal.ZERO;
                BigDecimal net = winAmount.subtract(stake);
                LocalDateTime placedAt = bet.getCreatedAt() != null
                        ? LocalDateTime.ofInstant(bet.getCreatedAt(), ZoneId.systemDefault())
                        : null;
                AgentCustomerBetHistoryItemResponse item = new AgentCustomerBetHistoryItemResponse();
                item.setBetId(bet.getId());
                item.setGameType("xoc-dia");
                item.setBetCode(bet.getBetCode());
                item.setStake(stake);
                item.setWinAmount(winAmount);
                item.setNetResult(net);
                item.setStatus(bet.getStatus().name());
                item.setPlacedAt(placedAt);
                items.add(item);
            });
        }

        if (includeSicbo) {
            sicboBetRepository.findRecentBetsByUserIdAndDateRange(
                    customerId,
                    startInstant,
                    endInstant,
                    PageRequest.of(0, size)
            ).forEach(bet -> {
                BigDecimal stake = bet.getStake() != null ? bet.getStake() : BigDecimal.ZERO;
                BigDecimal winAmount = bet.getWinAmount() != null ? bet.getWinAmount() : BigDecimal.ZERO;
                BigDecimal net = winAmount.subtract(stake);
                LocalDateTime placedAt = bet.getCreatedAt() != null
                        ? LocalDateTime.ofInstant(bet.getCreatedAt(), ZoneId.systemDefault())
                        : null;
                AgentCustomerBetHistoryItemResponse item = new AgentCustomerBetHistoryItemResponse();
                item.setBetId(bet.getId());
                item.setGameType("sicbo");
                item.setBetCode(bet.getBetCode());
                item.setStake(stake);
                item.setWinAmount(winAmount);
                item.setNetResult(net);
                item.setStatus(bet.getStatus().name());
                item.setPlacedAt(placedAt);
                items.add(item);
            });
        }

        items.sort(Comparator.comparing(AgentCustomerBetHistoryItemResponse::getPlacedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        BigDecimal totalStake = items.stream()
                .map(AgentCustomerBetHistoryItemResponse::getStake)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWin = items.stream()
                .map(AgentCustomerBetHistoryItemResponse::getWinAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLost = items.stream()
                .filter(item -> "LOST".equalsIgnoreCase(item.getStatus()))
                .map(AgentCustomerBetHistoryItemResponse::getStake)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = items.stream()
                .map(AgentCustomerBetHistoryItemResponse::getNetResult)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AgentCustomerBetHistoryResponse response = new AgentCustomerBetHistoryResponse();
        response.setItems(items);
        response.setTotalStake(totalStake);
        response.setTotalWinAmount(totalWin);
        response.setTotalLostAmount(totalLost);
        response.setTotalNetResult(totalNet);
        return response;
    }

    @Transactional(readOnly = true)
    public AgentInviteInfoResponse getAgentInviteInfo(Long agentId) {
        User agent = userService.getUserById(agentId);
        if (agent.getStaffRole() != User.StaffRole.AGENT) {
            throw new AccessDeniedException("Chỉ đại lý mới được phép truy cập thông tin mã mời.");
        }

        String referralCode = agent.getReferralCode();
        if (referralCode == null || referralCode.isBlank()) {
            throw new IllegalStateException("Đại lý chưa được cấp mã mời. Vui lòng liên hệ quản trị viên.");
        }

        long totalReferrals = userRepository.countByInvitedByCodeIgnoreCase(referralCode);
        long activeReferrals = userRepository.countByInvitedByCodeIgnoreCaseAndStatus(referralCode, User.UserStatus.ACTIVE);

        List<User> recentUsers = userRepository
                .findTop20ByInvitedByCodeIgnoreCaseOrderByCreatedAtDesc(referralCode);

        List<AgentInviteReferralResponse> recentReferrals = recentUsers.stream()
                .map(user -> new AgentInviteReferralResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getStatus(),
                        user.getCreatedAt()
                ))
                .collect(Collectors.toList());

        AgentInviteInfoResponse response = new AgentInviteInfoResponse();
        response.setReferralCode(referralCode);
        response.setInvitePath("/?inviteCode=" + referralCode + "&register=1");
        response.setTotalReferrals(totalReferrals);
        response.setActiveReferrals(activeReferrals);
        response.setRecentReferrals(recentReferrals);
        return response;
    }

    @Transactional(readOnly = true)
    private List<AgentCommissionCustomerSummaryResponse> computeCustomerSummaries(
            List<User> customers,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            double commissionRate
    ) {
        List<Long> customerIds = customers.stream().map(User::getId).toList();

        Map<Long, BigDecimal> totalBetMap = new HashMap<>();
        Map<Long, BigDecimal> totalLostMap = new HashMap<>();

        if (!customerIds.isEmpty()) {
            List<Object[]> betAggregates = betRepository.aggregateTotalsByUsers(customerIds, startDateTime, endDateTime);
            for (Object[] row : betAggregates) {
                Long userId = (Long) row[0];
                accumulate(totalBetMap, userId, toBigDecimal(row[1]));
                accumulate(totalLostMap, userId, toBigDecimal(row[2]));
            }

            Instant startInstant = startDateTime != null ? startDateTime.atZone(ZoneId.systemDefault()).toInstant() : null;
            Instant endInstant = endDateTime != null ? endDateTime.atZone(ZoneId.systemDefault()).toInstant() : null;

            List<Object[]> xocDiaAggregates = xocDiaBetRepository.aggregateTotalsByUsers(customerIds, startInstant, endInstant);
            for (Object[] row : xocDiaAggregates) {
                Long userId = (Long) row[0];
                accumulate(totalBetMap, userId, toBigDecimal(row[1]));
                accumulate(totalLostMap, userId, toBigDecimal(row[2]));
            }

            List<Object[]> sicboAggregates = sicboBetRepository.aggregateTotalsByUsers(customerIds, startInstant, endInstant);
            for (Object[] row : sicboAggregates) {
                Long userId = (Long) row[0];
                accumulate(totalBetMap, userId, toBigDecimal(row[1]));
                accumulate(totalLostMap, userId, toBigDecimal(row[2]));
            }
        }

        BigDecimal commissionMultiplier = BigDecimal.valueOf(commissionRate)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);

        return customers.stream()
                .map(customer -> {
                    BigDecimal totalBet = totalBetMap.getOrDefault(customer.getId(), BigDecimal.ZERO);
                    BigDecimal totalLost = totalLostMap.getOrDefault(customer.getId(), BigDecimal.ZERO);
                    BigDecimal commission = totalLost.multiply(commissionMultiplier).setScale(0, RoundingMode.HALF_UP);
                    return new AgentCommissionCustomerSummaryResponse(
                            customer.getId(),
                            customer.getUsername(),
                            customer.getStatus(),
                            totalBet,
                            totalLost,
                            commission
                    );
                })
                .collect(Collectors.toList());
    }

    private AgentDashboardSummaryResponse buildSummary(
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            List<User> customers
    ) {
        double commissionRate = systemSettingsService.getAgentCommissionPercentage();
        List<AgentCommissionCustomerSummaryResponse> allCustomers = computeCustomerSummaries(
                customers,
                startDateTime,
                endDateTime,
                commissionRate
        );

        BigDecimal totalBetAmount = allCustomers.stream()
                .map(AgentCommissionCustomerSummaryResponse::getTotalBetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLostAmount = allCustomers.stream()
                .map(AgentCommissionCustomerSummaryResponse::getTotalLostAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCommissionAmount = allCustomers.stream()
                .map(AgentCommissionCustomerSummaryResponse::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AgentCommissionCustomerSummaryResponse> topCustomers = allCustomers.stream()
                .sorted(Comparator.comparing(AgentCommissionCustomerSummaryResponse::getCommissionAmount).reversed())
                .limit(10)
                .collect(Collectors.toList());

        return new AgentDashboardSummaryResponse(
                customers.size(),
                totalBetAmount,
                totalLostAmount,
                totalCommissionAmount,
                commissionRate,
                topCustomers
        );
    }

    @Transactional(readOnly = true)
    public AgentDashboardSummaryResponse getAgentDashboardSummary(
            Long agentId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        User agent = userService.getUserById(agentId);
        if (agent.getStaffRole() != User.StaffRole.AGENT) {
            throw new AccessDeniedException("Chỉ đại lý mới được phép xem báo cáo hoa hồng.");
        }

        String referralCode = agent.getReferralCode();
        if (referralCode == null || referralCode.isBlank()) {
            throw new IllegalStateException("Đại lý chưa được cấp mã mời. Vui lòng liên hệ quản trị viên.");
        }

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : LocalDate.now().atTime(LocalTime.MAX);

        List<User> customers = userRepository.findByInvitedByCodeIgnoreCase(referralCode);

        return buildSummary(startDateTime, endDateTime, customers);
    }

    @Transactional(readOnly = true)
    public List<AgentCommissionChartPointResponse> getAgentCommissionChart(
            Long agentId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        User agent = userService.getUserById(agentId);
        if (agent.getStaffRole() != User.StaffRole.AGENT) {
            throw new AccessDeniedException("Chỉ đại lý mới được phép xem biểu đồ hoa hồng.");
        }

        String referralCode = agent.getReferralCode();
        if (referralCode == null || referralCode.isBlank()) {
            throw new IllegalStateException("Đại lý chưa được cấp mã mời. Vui lòng liên hệ quản trị viên.");
        }

        List<User> customers = userRepository.findByInvitedByCodeIgnoreCase(referralCode);

        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(29);
        LocalDate end = endDate != null ? endDate : LocalDate.now();

        List<AgentCommissionChartPointResponse> chart = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            LocalDateTime dayStart = cursor.atStartOfDay();
            LocalDateTime dayEnd = cursor.atTime(LocalTime.MAX);

            AgentDashboardSummaryResponse daySummary = buildSummary(
                    dayStart,
                    dayEnd,
                    customers
            );

            chart.add(new AgentCommissionChartPointResponse(
                    cursor,
                    daySummary.getTotalBetAmount(),
                    daySummary.getTotalLostAmount(),
                    daySummary.getTotalCommissionAmount()
            ));

            cursor = cursor.plusDays(1);
        }

        return chart;
    }

    @Transactional(readOnly = true)
    public AgentCommissionSummaryResponse getCommissionSummary(Long agentId, YearMonth period) {
        User agent = userService.getUserById(agentId);
        if (agent.getStaffRole() != User.StaffRole.AGENT) {
            throw new AccessDeniedException("Chỉ đại lý mới được phép xem báo cáo hoa hồng.");
        }

        String referralCode = agent.getReferralCode();
        if (referralCode == null || referralCode.isBlank()) {
            throw new IllegalStateException("Đại lý chưa được cấp mã mời. Vui lòng liên hệ quản trị viên.");
        }

        LocalDate periodStart = period.atDay(1);
        LocalDate periodEnd = period.atEndOfMonth();
        LocalDateTime startDateTime = periodStart.atStartOfDay();
        LocalDateTime endDateTime = periodEnd.atTime(LocalTime.MAX);

        List<User> customers = userRepository.findByInvitedByCodeIgnoreCase(referralCode);
        AgentDashboardSummaryResponse summary = buildSummary(startDateTime, endDateTime, customers);
        List<AgentCommissionCustomerSummaryResponse> allCustomers = computeCustomerSummaries(
                customers,
                startDateTime,
                endDateTime,
                summary.getCommissionRate()
        );

        return new AgentCommissionSummaryResponse(
                period.toString(),
                summary.getCommissionRate(),
                summary.getTotalBetAmount(),
                summary.getTotalLostAmount(),
                summary.getTotalCommissionAmount(),
                allCustomers.size(),
                allCustomers
        );
    }

    @Transactional(readOnly = true)
    public Page<AgentCommissionPayoutResponse> getCommissionPayoutHistory(
            Long agentId,
            AgentCommissionPayout.Status status,
            Pageable pageable
    ) {
        User agent = userService.getUserById(agentId);
        if (agent.getStaffRole() != User.StaffRole.AGENT) {
            throw new AccessDeniedException("Chỉ đại lý mới được phép xem lịch sử thanh toán hoa hồng.");
        }

        Page<AgentCommissionPayout> payouts = status == null
                ? agentCommissionPayoutRepository.findByAgentOrderByPeriodStartDesc(agent, pageable)
                : agentCommissionPayoutRepository.findByAgentAndStatusOrderByPeriodStartDesc(agent, status, pageable);

        return payouts.map(payout -> new AgentCommissionPayoutResponse(
                payout.getId(),
                payout.getPeriodMonth(),
                payout.getPeriodStart(),
                payout.getPeriodEnd(),
                payout.getTotalLostAmount(),
                payout.getCommissionAmount(),
                payout.getStatus(),
                payout.getPaidAt(),
                payout.getNotes()
        ));
    }

    private void accumulate(Map<Long, BigDecimal> map, Long userId, BigDecimal amount) {
        if (amount == null) {
            return;
        }
        map.merge(userId, amount, BigDecimal::add);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}

