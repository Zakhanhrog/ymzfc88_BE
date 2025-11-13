package com.xsecret.service;

import com.xsecret.dto.request.UserFilterRequestDto;
import com.xsecret.dto.response.StaffMktBetResponse;
import com.xsecret.dto.response.StaffMktFinanceOverviewResponse;
import com.xsecret.dto.response.StaffMktGameOverviewResponse;
import com.xsecret.dto.response.StaffMktGameSummaryResponse;
import com.xsecret.dto.response.StaffMktTransactionResponse;
import com.xsecret.dto.response.StaffMktUserPageResponse;
import com.xsecret.dto.response.StaffMktUserResponse;
import com.xsecret.entity.Bet;
import com.xsecret.entity.SicboBet;
import com.xsecret.entity.Transaction;
import com.xsecret.entity.User;
import com.xsecret.entity.XocDiaBet;
import com.xsecret.repository.BetRepository;
import com.xsecret.repository.SicboBetRepository;
import com.xsecret.repository.TransactionRepository;
import com.xsecret.repository.XocDiaBetRepository;
import com.xsecret.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffMktService {

    private final UserService userService;
    private final TransactionRepository transactionRepository;
    private final BetRepository betRepository;
    private final XocDiaBetRepository xocDiaBetRepository;
    private final SicboBetRepository sicboBetRepository;

    private void ensureStaffMkt(UserPrincipal principal) {
        if (principal == null || principal.getStaffRole() != User.StaffRole.STAFF_MKT) {
            throw new AccessDeniedException("Chỉ nhân viên MKT mới có quyền truy cập chức năng này.");
        }
    }

    @Transactional(readOnly = true)
    public StaffMktUserPageResponse getUsers(
            UserPrincipal principal,
            String search,
            User.UserStatus status,
            int page,
            int size
    ) {
        ensureStaffMkt(principal);

        UserFilterRequestDto filters = UserFilterRequestDto.builder()
                .searchTerm(StringUtils.hasText(search) ? search.trim() : null)
                .status(status != null ? status.name() : null)
                .page(Math.max(page, 0))
                .size(Math.max(size, 1))
                .build();

        Page<User> users = userService.getUsersWithFilters(filters);
        List<StaffMktUserResponse> items = users.getContent().stream()
                .map(user -> StaffMktUserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhoneNumber())
                        .status(user.getStatus() != null ? user.getStatus().name() : null)
                        .role(user.getRole() != null ? user.getRole().name() : null)
                        .points(user.getPoints())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return StaffMktUserPageResponse.builder()
                .items(items)
                .totalItems(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .page(users.getNumber())
                .size(users.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    public StaffMktFinanceOverviewResponse getFinanceOverview(
            UserPrincipal principal,
            LocalDate startDate,
            LocalDate endDate
    ) {
        ensureStaffMkt(principal);

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;

        BigDecimal totalDeposit = safe(transactionRepository.sumNetAmountByFilters(
                Transaction.TransactionType.DEPOSIT,
                Transaction.TransactionStatus.APPROVED,
                start,
                end
        ));

        BigDecimal totalWithdraw = safe(transactionRepository.sumNetAmountByFilters(
                Transaction.TransactionType.WITHDRAW,
                Transaction.TransactionStatus.APPROVED,
                start,
                end
        ));

        long approvedDeposits = transactionRepository.countByTypeAndStatus(
                Transaction.TransactionType.DEPOSIT,
                Transaction.TransactionStatus.APPROVED
        );
        long approvedWithdraws = transactionRepository.countByTypeAndStatus(
                Transaction.TransactionType.WITHDRAW,
                Transaction.TransactionStatus.APPROVED
        );
        long pendingDeposits = transactionRepository.countByTypeAndStatus(
                Transaction.TransactionType.DEPOSIT,
                Transaction.TransactionStatus.PENDING
        );
        long pendingWithdraws = transactionRepository.countByTypeAndStatus(
                Transaction.TransactionType.WITHDRAW,
                Transaction.TransactionStatus.PENDING
        );

        List<Transaction> recentTransactions = transactionRepository.findRecentTransactionsByStatuses(
                List.of(
                        Transaction.TransactionStatus.APPROVED,
                        Transaction.TransactionStatus.PENDING
                ),
                PageRequest.of(0, 10)
        );

        List<StaffMktTransactionResponse> recent = recentTransactions.stream()
                .map(tx -> StaffMktTransactionResponse.builder()
                        .id(tx.getId())
                        .transactionCode(tx.getTransactionCode())
                        .username(tx.getUser() != null ? tx.getUser().getUsername() : null)
                        .fullName(tx.getUser() != null ? tx.getUser().getFullName() : null)
                        .type(tx.getType() != null ? tx.getType().name() : null)
                        .status(tx.getStatus() != null ? tx.getStatus().name() : null)
                        .amount(safe(tx.getAmount()))
                        .netAmount(safe(tx.getNetAmount()))
                        .createdAt(tx.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return StaffMktFinanceOverviewResponse.builder()
                .totalDepositAmount(totalDeposit)
                .totalWithdrawAmount(totalWithdraw)
                .approvedDepositCount(approvedDeposits)
                .approvedWithdrawCount(approvedWithdraws)
                .pendingDepositCount(pendingDeposits)
                .pendingWithdrawCount(pendingWithdraws)
                .recentTransactions(recent)
                .build();
    }

    @Transactional(readOnly = true)
    public StaffMktGameOverviewResponse getGameOverview(
            UserPrincipal principal
    ) {
        ensureStaffMkt(principal);

        StaffMktGameSummaryResponse lotterySummary = StaffMktGameSummaryResponse.builder()
                .totalBets(betRepository.count())
                .totalStakeAmount(safe(betRepository.sumTotalAmountByFilters(null, null, null)))
                .totalWinAmount(safe(betRepository.sumWinAmountByFilters(null, null, null)))
                .build();

        StaffMktGameSummaryResponse xocDiaSummary = StaffMktGameSummaryResponse.builder()
                .totalBets(xocDiaBetRepository.count())
                .totalStakeAmount(safe(xocDiaBetRepository.sumStakeByCreatedAtFilters(null, null, null)))
                .totalWinAmount(safe(xocDiaBetRepository.sumWinAmountByCreatedAtFilters(null, null, null)))
                .build();

        StaffMktGameSummaryResponse sicboSummary = StaffMktGameSummaryResponse.builder()
                .totalBets(sicboBetRepository.count())
                .totalStakeAmount(safe(sicboBetRepository.sumStakeByCreatedAtFilters(null, null, null)))
                .totalWinAmount(safe(sicboBetRepository.sumWinAmountByCreatedAtFilters(null, null, null)))
                .build();

        List<StaffMktBetResponse> recentBets = new ArrayList<>();

        betRepository.findForAnalytics(null, null, null, PageRequest.of(0, 5))
                .forEach(bet -> recentBets.add(toBetResponse(bet)));
        xocDiaBetRepository.findAdminHistory(null, null, null, PageRequest.of(0, 5))
                .forEach(bet -> recentBets.add(toBetResponse(bet)));
        sicboBetRepository.findAdminHistory(null, null, null, PageRequest.of(0, 5))
                .forEach(bet -> recentBets.add(toBetResponse(bet)));

        // Sort recent bets by createdAt descending
        recentBets.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) {
                return 0;
            }
            if (a.getCreatedAt() == null) {
                return 1;
            }
            if (b.getCreatedAt() == null) {
                return -1;
            }
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        return StaffMktGameOverviewResponse.builder()
                .lottery(lotterySummary)
                .xocdia(xocDiaSummary)
                .sicbo(sicboSummary)
                .recentBets(recentBets.stream().limit(15).collect(Collectors.toList()))
                .build();
    }

    private StaffMktBetResponse toBetResponse(Bet bet) {
        return StaffMktBetResponse.builder()
                .id(bet.getId())
                .gameType("LOTTERY")
                .username(bet.getUser() != null ? bet.getUser().getUsername() : null)
                .betCode(bet.getBetType())
                .stakeAmount(safe(bet.getTotalAmount()))
                .winAmount(safe(bet.getWinAmount()))
                .status(bet.getStatus() != null ? bet.getStatus().name() : null)
                .createdAt(bet.getCreatedAt() != null
                        ? bet.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
                        : null)
                .build();
    }

    private StaffMktBetResponse toBetResponse(XocDiaBet bet) {
        return StaffMktBetResponse.builder()
                .id(bet.getId())
                .gameType("XOCDIA")
                .username(bet.getUser() != null ? bet.getUser().getUsername() : null)
                .betCode(bet.getBetCode())
                .stakeAmount(safe(bet.getStake()))
                .winAmount(safe(bet.getWinAmount()))
                .status(bet.getStatus() != null ? bet.getStatus().name() : null)
                .createdAt(bet.getCreatedAt())
                .build();
    }

    private StaffMktBetResponse toBetResponse(SicboBet bet) {
        return StaffMktBetResponse.builder()
                .id(bet.getId())
                .gameType("SICBO")
                .username(bet.getUser() != null ? bet.getUser().getUsername() : null)
                .betCode(bet.getBetCode())
                .stakeAmount(safe(bet.getStake()))
                .winAmount(safe(bet.getWinAmount()))
                .status(bet.getStatus() != null ? bet.getStatus().name() : null)
                .createdAt(bet.getCreatedAt())
                .build();
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}

