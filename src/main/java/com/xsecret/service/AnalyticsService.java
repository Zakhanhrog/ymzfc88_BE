package com.xsecret.service;

import com.xsecret.dto.response.BetAnalyticsItemResponse;
import com.xsecret.dto.response.BetAnalyticsResponse;
import com.xsecret.dto.response.TransactionAnalyticsItemResponse;
import com.xsecret.dto.response.TransactionAnalyticsResponse;
import com.xsecret.entity.Bet;
import com.xsecret.entity.SicboBet;
import com.xsecret.entity.Transaction;
import com.xsecret.entity.XocDiaBet;
import com.xsecret.repository.BetRepository;
import com.xsecret.repository.SicboBetRepository;
import com.xsecret.repository.TransactionRepository;
import com.xsecret.repository.XocDiaBetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AnalyticsService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private final BetRepository betRepository;
    private final SicboBetRepository sicboBetRepository;
    private final XocDiaBetRepository xocDiaBetRepository;
    private final TransactionRepository transactionRepository;

    public BetAnalyticsResponse getBetAnalytics(String rawGameType,
                                                String rawStatus,
                                                LocalDateTime start,
                                                LocalDateTime end,
                                                int page,
                                                int size) {
        String gameType = rawGameType == null ? "lottery" : rawGameType.toLowerCase(Locale.ROOT);
        size = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        page = Math.max(0, page);

        switch (gameType) {
            case "sicbo":
                return buildSicboAnalytics(parseSicboStatus(rawStatus), start, end, page, size);
            case "xocdia":
            case "xóc đĩa":
            case "xoc-dia":
                return buildXocDiaAnalytics(parseXocDiaStatus(rawStatus), start, end, page, size);
            default:
                return buildLotteryAnalytics(parseLotteryStatus(rawStatus), start, end, page, size);
        }
    }

    public TransactionAnalyticsResponse getTransactionAnalytics(String rawType,
                                                                 String rawStatus,
                                                                 LocalDateTime start,
                                                                 LocalDateTime end,
                                                                 int page,
                                                                 int size) {
        size = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        page = Math.max(0, page);

        Transaction.TransactionType type = parseTransactionType(rawType);
        Transaction.TransactionStatus status = parseTransactionStatus(rawStatus);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transaction> transactionPage = transactionRepository.findAnalytics(type, status, start, end, pageable);

        BigDecimal totalAmount = safeBigDecimal(transactionRepository.sumAmountByFilters(type, status, start, end));
        BigDecimal totalNetAmount = safeBigDecimal(transactionRepository.sumNetAmountByFilters(type, status, start, end));

        List<TransactionAnalyticsItemResponse> items = transactionPage.getContent().stream()
                .map(this::mapTransaction)
                .collect(Collectors.toList());

        return TransactionAnalyticsResponse.builder()
                .items(items)
                .totalItems(transactionPage.getTotalElements())
                .totalPages(transactionPage.getTotalPages())
                .page(page)
                .size(size)
                .summary(TransactionAnalyticsResponse.Summary.builder()
                        .totalAmount(totalAmount)
                        .totalNetAmount(totalNetAmount)
                        .totalCount(transactionPage.getTotalElements())
                        .build())
                .build();
    }

    private BetAnalyticsResponse buildLotteryAnalytics(Bet.BetStatus status,
                                                       LocalDateTime start,
                                                       LocalDateTime end,
                                                       int page,
                                                       int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Bet> betPage = betRepository.findForAnalytics(status, start, end, pageable);

        BigDecimal totalStake = safeBigDecimal(betRepository.sumTotalAmountByFilters(status, start, end));
        BigDecimal totalWinAmount = safeBigDecimal(betRepository.sumWinAmountByFilters(status, start, end));
        BigDecimal totalLostAmount = shouldCalculateLoss(status)
                ? safeBigDecimal(betRepository.sumTotalAmountByStatusAndDate(Bet.BetStatus.LOST, start, end))
                : BigDecimal.ZERO;

        List<BetAnalyticsItemResponse> items = betPage.getContent().stream()
                .map(this::mapLotteryBet)
                .collect(Collectors.toList());

        return BetAnalyticsResponse.builder()
                .items(items)
                .totalItems(betPage.getTotalElements())
                .totalPages(betPage.getTotalPages())
                .page(page)
                .size(size)
                .summary(BetAnalyticsResponse.Summary.builder()
                        .totalStake(totalStake)
                        .totalWinAmount(totalWinAmount)
                        .totalLostAmount(totalLostAmount)
                        .build())
                .build();
    }

    private BetAnalyticsResponse buildSicboAnalytics(SicboBet.Status status,
                                                     LocalDateTime start,
                                                     LocalDateTime end,
                                                     int page,
                                                     int size) {
        Instant startInstant = toInstant(start);
        Instant endInstant = toInstant(end);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "settledAt"));

        Page<SicboBet> betPage = sicboBetRepository.findForAnalytics(status, startInstant, endInstant, pageable);

        BigDecimal totalStake = safeBigDecimal(sicboBetRepository.sumStakeByFilters(status, startInstant, endInstant));
        BigDecimal totalWinAmount = safeBigDecimal(sicboBetRepository.sumWinAmountByFilters(status, startInstant, endInstant));
        BigDecimal totalLostAmount = shouldCalculateLoss(status)
                ? safeBigDecimal(sicboBetRepository.sumStakeByStatusesAndDate(List.of(SicboBet.Status.LOST), startInstant, endInstant))
                : BigDecimal.ZERO;

        List<BetAnalyticsItemResponse> items = betPage.getContent().stream()
                .map(this::mapSicboBet)
                .collect(Collectors.toList());

        return BetAnalyticsResponse.builder()
                .items(items)
                .totalItems(betPage.getTotalElements())
                .totalPages(betPage.getTotalPages())
                .page(page)
                .size(size)
                .summary(BetAnalyticsResponse.Summary.builder()
                        .totalStake(totalStake)
                        .totalWinAmount(totalWinAmount)
                        .totalLostAmount(totalLostAmount)
                        .build())
                .build();
    }

    private BetAnalyticsResponse buildXocDiaAnalytics(XocDiaBet.Status status,
                                                      LocalDateTime start,
                                                      LocalDateTime end,
                                                      int page,
                                                      int size) {
        Instant startInstant = toInstant(start);
        Instant endInstant = toInstant(end);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "settledAt"));

        Page<XocDiaBet> betPage = xocDiaBetRepository.findForAnalytics(status, startInstant, endInstant, pageable);

        BigDecimal totalStake = safeBigDecimal(xocDiaBetRepository.sumStakeByFilters(status, startInstant, endInstant));
        BigDecimal totalWinAmount = safeBigDecimal(xocDiaBetRepository.sumWinAmountByFilters(status, startInstant, endInstant));
        BigDecimal totalLostAmount = shouldCalculateLoss(status)
                ? safeBigDecimal(xocDiaBetRepository.sumStakeByStatusesAndDate(List.of(XocDiaBet.Status.LOST), startInstant, endInstant))
                : BigDecimal.ZERO;

        List<BetAnalyticsItemResponse> items = betPage.getContent().stream()
                .map(this::mapXocDiaBet)
                .collect(Collectors.toList());

        return BetAnalyticsResponse.builder()
                .items(items)
                .totalItems(betPage.getTotalElements())
                .totalPages(betPage.getTotalPages())
                .page(page)
                .size(size)
                .summary(BetAnalyticsResponse.Summary.builder()
                        .totalStake(totalStake)
                        .totalWinAmount(totalWinAmount)
                        .totalLostAmount(totalLostAmount)
                        .build())
                .build();
    }

    private BetAnalyticsItemResponse mapLotteryBet(Bet bet) {
        BigDecimal stake = safeBigDecimal(bet.getTotalAmount());
        BigDecimal winAmount = safeBigDecimal(bet.getWinAmount());
        BigDecimal revenue = bet.getStatus() == Bet.BetStatus.LOST ? stake : BigDecimal.ZERO;

        return BetAnalyticsItemResponse.builder()
                .id(bet.getId())
                .gameType("LOTTERY")
                .username(bet.getUser() != null ? bet.getUser().getUsername() : null)
                .betCode(bet.getBetType())
                .betType(bet.getBetType())
                .stake(stake)
                .winAmount(winAmount)
                .revenue(revenue)
                .status(bet.getStatus().name())
                .createdAt(bet.getCreatedAt())
                .settledAt(bet.getResultCheckedAt())
                .build();
    }

    private BetAnalyticsItemResponse mapSicboBet(SicboBet bet) {
        BigDecimal stake = safeBigDecimal(bet.getStake());
        BigDecimal winAmount = safeBigDecimal(bet.getWinAmount());
        BigDecimal revenue = bet.getStatus() == SicboBet.Status.LOST ? stake : BigDecimal.ZERO;

        return BetAnalyticsItemResponse.builder()
                .id(bet.getId())
                .gameType("SICBO")
                .username(bet.getUser() != null ? bet.getUser().getUsername() : null)
                .betCode(bet.getBetCode())
                .betType(bet.getBetCode())
                .stake(stake)
                .winAmount(winAmount)
                .revenue(revenue)
                .status(bet.getStatus().name())
                .createdAt(bet.getCreatedAt() != null ? LocalDateTime.ofInstant(bet.getCreatedAt(), SYSTEM_ZONE) : null)
                .settledAt(bet.getSettledAt() != null ? LocalDateTime.ofInstant(bet.getSettledAt(), SYSTEM_ZONE) : null)
                .build();
    }

    private BetAnalyticsItemResponse mapXocDiaBet(XocDiaBet bet) {
        BigDecimal stake = safeBigDecimal(bet.getStake());
        BigDecimal winAmount = safeBigDecimal(bet.getWinAmount());
        BigDecimal revenue = bet.getStatus() == XocDiaBet.Status.LOST ? stake : BigDecimal.ZERO;

        return BetAnalyticsItemResponse.builder()
                .id(bet.getId())
                .gameType("XOCDIA")
                .username(bet.getUser() != null ? bet.getUser().getUsername() : null)
                .betCode(bet.getBetCode())
                .betType(bet.getBetCode())
                .stake(stake)
                .winAmount(winAmount)
                .revenue(revenue)
                .status(bet.getStatus().name())
                .createdAt(bet.getCreatedAt() != null ? LocalDateTime.ofInstant(bet.getCreatedAt(), SYSTEM_ZONE) : null)
                .settledAt(bet.getSettledAt() != null ? LocalDateTime.ofInstant(bet.getSettledAt(), SYSTEM_ZONE) : null)
                .build();
    }

    private TransactionAnalyticsItemResponse mapTransaction(Transaction transaction) {
        return TransactionAnalyticsItemResponse.builder()
                .id(transaction.getId())
                .transactionCode(transaction.getTransactionCode())
                .username(transaction.getUser() != null ? transaction.getUser().getUsername() : null)
                .type(transaction.getType().name())
                .status(transaction.getStatus().name())
                .amount(safeBigDecimal(transaction.getAmount()))
                .netAmount(safeBigDecimal(transaction.getNetAmount()))
                .createdAt(transaction.getCreatedAt())
                .processedAt(transaction.getProcessedAt())
                .paymentMethod(transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().getName() : null)
                .build();
    }

    private Bet.BetStatus parseLotteryStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank() || "all".equalsIgnoreCase(rawStatus)) {
            return null;
        }
        try {
            return Bet.BetStatus.valueOf(rawStatus.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid lottery status '{}'", rawStatus);
            return null;
        }
    }

    private SicboBet.Status parseSicboStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank() || "all".equalsIgnoreCase(rawStatus)) {
            return null;
        }
        try {
            return SicboBet.Status.valueOf(rawStatus.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid sicbo status '{}'", rawStatus);
            return null;
        }
    }

    private XocDiaBet.Status parseXocDiaStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank() || "all".equalsIgnoreCase(rawStatus)) {
            return null;
        }
        try {
            return XocDiaBet.Status.valueOf(rawStatus.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid xoc dia status '{}'", rawStatus);
            return null;
        }
    }

    private Transaction.TransactionType parseTransactionType(String rawType) {
        if (rawType == null || rawType.isBlank() || "all".equalsIgnoreCase(rawType)) {
            return null;
        }
        try {
            return Transaction.TransactionType.valueOf(rawType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid transaction type '{}'", rawType);
            return null;
        }
    }

    private Transaction.TransactionStatus parseTransactionStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank() || "all".equalsIgnoreCase(rawStatus)) {
            return null;
        }
        try {
            return Transaction.TransactionStatus.valueOf(rawStatus.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid transaction status '{}'", rawStatus);
            return null;
        }
    }

    private boolean shouldCalculateLoss(Enum<?> status) {
        return status == null || Objects.equals(status.name(), "LOST");
    }

    private Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(SYSTEM_ZONE).toInstant();
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


