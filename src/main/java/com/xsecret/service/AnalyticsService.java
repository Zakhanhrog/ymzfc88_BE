package com.xsecret.service;

import com.xsecret.dto.response.BetAnalyticsItemResponse;
import com.xsecret.dto.response.BetAnalyticsResponse;
import com.xsecret.dto.response.TransactionAnalyticsItemResponse;
import com.xsecret.dto.response.TransactionAnalyticsResponse;
import com.xsecret.entity.Bet;
import com.xsecret.entity.SicboBet;
import com.xsecret.entity.Transaction;
import com.xsecret.entity.XocDiaBet;
import com.xsecret.repository.AgentCommissionPayoutRepository;
import com.xsecret.repository.BetRepository;
import com.xsecret.repository.DailyLossRefundRepository;
import com.xsecret.repository.GameRefundAccrualRepository;
import com.xsecret.repository.PointTransactionRepository;
import com.xsecret.repository.PromotionalMoneyRepository;
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
    private static final ZoneId SYSTEM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final BetRepository betRepository;
    private final SicboBetRepository sicboBetRepository;
    private final XocDiaBetRepository xocDiaBetRepository;
    private final TransactionRepository transactionRepository;
    private final GameRefundAccrualRepository gameRefundAccrualRepository;
    private final DailyLossRefundRepository dailyLossRefundRepository;
    private final AgentCommissionPayoutRepository agentCommissionPayoutRepository;
    private final PromotionalMoneyRepository promotionalMoneyRepository;
    private final PointTransactionRepository pointTransactionRepository;

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
            case "all":
                return buildAllGamesAnalytics(rawStatus, start, end, page, size);
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

        // Đảm bảo start là start of day và end là end of day
        LocalDateTime startDate = start != null ? start.toLocalDate().atStartOfDay() : null;
        LocalDateTime endDate = end != null ? end.toLocalDate().atTime(23, 59, 59, 999_999_999) : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transaction> transactionPage = transactionRepository.findAnalytics(type, status, startDate, endDate, pageable);

        BigDecimal totalAmount = safeBigDecimal(transactionRepository.sumAmountByFilters(type, status, startDate, endDate));
        BigDecimal totalNetAmount = safeBigDecimal(transactionRepository.sumNetAmountByFilters(type, status, startDate, endDate));

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
        // Đảm bảo start là start of day và end là end of day
        LocalDateTime startDate = start != null ? start.toLocalDate().atStartOfDay() : null;
        LocalDateTime endDate = end != null ? end.toLocalDate().atTime(23, 59, 59, 999_999_999) : null;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Bet> betPage = betRepository.findForAnalytics(status, startDate, endDate, pageable);

        BigDecimal totalStake = safeBigDecimal(betRepository.sumTotalAmountByFilters(status, startDate, endDate));
        // Tính totalWinAmount là profit (winAmount - stake), không bao gồm tiền cược gốc
        BigDecimal totalWinAmount;
        if (status == null || status == Bet.BetStatus.WON) {
            // Chỉ tính profit cho các bet WON
            totalWinAmount = safeBigDecimal(betRepository.sumWinProfitByFilters(startDate, endDate));
        } else {
            // Nếu filter theo status khác (LOST, PENDING), thì không có win
            totalWinAmount = BigDecimal.ZERO;
        }
        BigDecimal totalLostAmount = shouldCalculateLoss(status)
                ? safeBigDecimal(betRepository.sumTotalAmountByStatusAndDate(Bet.BetStatus.LOST, startDate, endDate))
                : BigDecimal.ZERO;

        List<BetAnalyticsItemResponse> items = betPage.getContent().stream()
                .map(this::mapLotteryBet)
                .collect(Collectors.toList());

        FinancialSummary financial = calculateFinancialSummary(startDate, endDate);
        
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
                        .totalFee(BigDecimal.ZERO)
                        .sicboTotalFee(BigDecimal.ZERO)
                        .xocDiaTotalFee(BigDecimal.ZERO)
                        .totalBao(BigDecimal.ZERO)
                        .totalDeposit(financial.totalDeposit)
                        .totalWithdraw(financial.totalWithdraw)
                        .totalRefund(financial.totalRefund)
                        .totalDailyLossRefund(financial.totalDailyLossRefund)
                        .totalAgentCommission(financial.totalAgentCommission)
                        .totalPromotionalMoney(financial.totalPromotionalMoney)
                        .lotteryWinAmount(totalWinAmount)
                        .lotteryLostAmount(totalLostAmount)
                        .build())
                .build();
    }

    private BetAnalyticsResponse buildSicboAnalytics(SicboBet.Status status,
                                                     LocalDateTime start,
                                                     LocalDateTime end,
                                                     int page,
                                                     int size) {
        Instant startInstant = start != null ? toInstant(start.toLocalDate().atStartOfDay()) : null;
        Instant endInstant = end != null ? toInstantEndOfDay(end) : null;
        // Sort đã được xử lý trong query, không cần Pageable sort
        Pageable pageable = PageRequest.of(page, size);

        Page<SicboBet> betPage = sicboBetRepository.findForAnalytics(status, startInstant, endInstant, pageable);

        BigDecimal totalStake = safeBigDecimal(sicboBetRepository.sumStakeByFilters(status, startInstant, endInstant));
        // Tính totalWinAmount là profit (winAmount - stake), không bao gồm tiền cược gốc
        BigDecimal totalWinAmount;
        if (status == null || status == SicboBet.Status.WON) {
            // Chỉ tính profit cho các bet WON
            totalWinAmount = safeBigDecimal(sicboBetRepository.sumWinProfitByCreatedAtFilters(startInstant, endInstant));
        } else {
            // Nếu filter theo status khác (LOST, PENDING), thì không có win
            totalWinAmount = BigDecimal.ZERO;
        }
        BigDecimal totalLostAmount = shouldCalculateLoss(status)
                ? safeBigDecimal(sicboBetRepository.sumStakeByStatusesAndDate(List.of(SicboBet.Status.LOST), startInstant, endInstant))
                : BigDecimal.ZERO;
        BigDecimal totalFee = safeBigDecimal(sicboBetRepository.sumFeeAmountByFilters(startInstant, endInstant));
        BigDecimal totalBao = safeBigDecimal(sicboBetRepository.sumBaoAmountByFilters(status, startInstant, endInstant));

        List<BetAnalyticsItemResponse> items = betPage.getContent().stream()
                .map(this::mapSicboBet)
                .collect(Collectors.toList());

        FinancialSummary financial = calculateFinancialSummary(start, end);

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
                        .totalFee(totalFee)
                        .sicboTotalFee(totalFee) // Sicbo fee
                        .xocDiaTotalFee(BigDecimal.ZERO) // XocDia không có fee trong Sicbo analytics
                        .totalBao(totalBao)
                        .totalDeposit(financial.totalDeposit)
                        .totalWithdraw(financial.totalWithdraw)
                        .totalRefund(financial.totalRefund)
                        .totalDailyLossRefund(financial.totalDailyLossRefund)
                        .totalAgentCommission(financial.totalAgentCommission)
                        .totalPromotionalMoney(financial.totalPromotionalMoney)
                        .lotteryWinAmount(BigDecimal.ZERO)
                        .lotteryLostAmount(BigDecimal.ZERO)
                        .build())
                .build();
    }

    private BetAnalyticsResponse buildAllGamesAnalytics(String rawStatus,
                                                        LocalDateTime start,
                                                        LocalDateTime end,
                                                        int page,
                                                        int size) {
        // Lấy dữ liệu từ tất cả các game
        BetAnalyticsResponse lotteryResponse = buildLotteryAnalytics(parseLotteryStatus(rawStatus), start, end, 0, Integer.MAX_VALUE);
        BetAnalyticsResponse sicboResponse = buildSicboAnalytics(parseSicboStatus(rawStatus), start, end, 0, Integer.MAX_VALUE);
        BetAnalyticsResponse xocDiaResponse = buildXocDiaAnalytics(parseXocDiaStatus(rawStatus), start, end, 0, Integer.MAX_VALUE);

        // Combine tất cả items và sort theo thời gian (settledAt hoặc createdAt) giảm dần
        List<BetAnalyticsItemResponse> allItems = new java.util.ArrayList<>();
        allItems.addAll(lotteryResponse.getItems());
        allItems.addAll(sicboResponse.getItems());
        allItems.addAll(xocDiaResponse.getItems());

        // Sort theo settledAt (nếu có) hoặc createdAt, giảm dần
        allItems.sort((a, b) -> {
            LocalDateTime timeA = a.getSettledAt() != null ? a.getSettledAt() : a.getCreatedAt();
            LocalDateTime timeB = b.getSettledAt() != null ? b.getSettledAt() : b.getCreatedAt();
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return timeB.compareTo(timeA); // Giảm dần
        });

        // Tính tổng summary
        BigDecimal totalStake = lotteryResponse.getSummary().getTotalStake()
                .add(sicboResponse.getSummary().getTotalStake())
                .add(xocDiaResponse.getSummary().getTotalStake());
        BigDecimal totalWinAmount = lotteryResponse.getSummary().getTotalWinAmount()
                .add(sicboResponse.getSummary().getTotalWinAmount())
                .add(xocDiaResponse.getSummary().getTotalWinAmount());
        BigDecimal totalLostAmount = lotteryResponse.getSummary().getTotalLostAmount()
                .add(sicboResponse.getSummary().getTotalLostAmount())
                .add(xocDiaResponse.getSummary().getTotalLostAmount());
        BigDecimal totalFee = sicboResponse.getSummary().getTotalFee()
                .add(xocDiaResponse.getSummary().getTotalFee());
        BigDecimal sicboTotalFee = sicboResponse.getSummary().getTotalFee();
        BigDecimal xocDiaTotalFee = xocDiaResponse.getSummary().getTotalFee();
        BigDecimal totalBao = sicboResponse.getSummary().getTotalBao();
        
        FinancialSummary financial = calculateFinancialSummary(start, end);

        long totalItems = allItems.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);

        // Phân trang
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, allItems.size());
        List<BetAnalyticsItemResponse> pagedItems = startIndex < allItems.size()
                ? allItems.subList(startIndex, endIndex)
                : List.of();

        return BetAnalyticsResponse.builder()
                .items(pagedItems)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .page(page)
                .size(size)
                .summary(BetAnalyticsResponse.Summary.builder()
                        .totalStake(totalStake)
                        .totalWinAmount(totalWinAmount)
                        .totalLostAmount(totalLostAmount)
                        .totalFee(totalFee)
                        .sicboTotalFee(sicboTotalFee)
                        .xocDiaTotalFee(xocDiaTotalFee)
                        .totalBao(totalBao)
                        .totalDeposit(financial.totalDeposit)
                        .totalWithdraw(financial.totalWithdraw)
                        .totalRefund(financial.totalRefund)
                        .totalDailyLossRefund(financial.totalDailyLossRefund)
                        .totalAgentCommission(financial.totalAgentCommission)
                        .totalPromotionalMoney(financial.totalPromotionalMoney)
                        .lotteryWinAmount(lotteryResponse.getSummary().getTotalWinAmount())
                        .lotteryLostAmount(lotteryResponse.getSummary().getTotalLostAmount())
                        .build())
                .build();
    }

    private BetAnalyticsResponse buildXocDiaAnalytics(XocDiaBet.Status status,
                                                      LocalDateTime start,
                                                      LocalDateTime end,
                                                      int page,
                                                      int size) {
        Instant startInstant = start != null ? toInstant(start.toLocalDate().atStartOfDay()) : null;
        Instant endInstant = end != null ? toInstantEndOfDay(end) : null;
        // Sort đã được xử lý trong query, không cần Pageable sort
        Pageable pageable = PageRequest.of(page, size);

        Page<XocDiaBet> betPage = xocDiaBetRepository.findForAnalytics(status, startInstant, endInstant, pageable);

        BigDecimal totalStake = safeBigDecimal(xocDiaBetRepository.sumStakeByFilters(status, startInstant, endInstant));
        // Tính totalWinAmount là profit (winAmount - stake), không bao gồm tiền cược gốc
        BigDecimal totalWinAmount;
        if (status == null || status == XocDiaBet.Status.WON) {
            // Chỉ tính profit cho các bet WON
            totalWinAmount = safeBigDecimal(xocDiaBetRepository.sumWinProfitByCreatedAtFilters(startInstant, endInstant));
        } else {
            // Nếu filter theo status khác (LOST, PENDING), thì không có win
            totalWinAmount = BigDecimal.ZERO;
        }
        BigDecimal totalLostAmount = shouldCalculateLoss(status)
                ? safeBigDecimal(xocDiaBetRepository.sumStakeByStatusesAndDate(List.of(XocDiaBet.Status.LOST), startInstant, endInstant))
                : BigDecimal.ZERO;
        BigDecimal totalFee = safeBigDecimal(xocDiaBetRepository.sumFeeAmountByFilters(startInstant, endInstant));

        List<BetAnalyticsItemResponse> items = betPage.getContent().stream()
                .map(this::mapXocDiaBet)
                .collect(Collectors.toList());

        FinancialSummary financial = calculateFinancialSummary(start, end);
        
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
                        .totalFee(totalFee)
                        .sicboTotalFee(BigDecimal.ZERO) // Sicbo không có fee trong XocDia analytics
                        .xocDiaTotalFee(totalFee) // XocDia fee
                        .totalBao(BigDecimal.ZERO)
                        .totalDeposit(financial.totalDeposit)
                        .totalWithdraw(financial.totalWithdraw)
                        .totalRefund(financial.totalRefund)
                        .totalDailyLossRefund(financial.totalDailyLossRefund)
                        .totalAgentCommission(financial.totalAgentCommission)
                        .totalPromotionalMoney(financial.totalPromotionalMoney)
                        .lotteryWinAmount(BigDecimal.ZERO)
                        .lotteryLostAmount(BigDecimal.ZERO)
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
                .feeAmount(BigDecimal.ZERO)
                .baoAmount(BigDecimal.ZERO)
                .tableNumber(null)
                .status(bet.getStatus().name())
                .createdAt(bet.getCreatedAt())
                .settledAt(bet.getResultCheckedAt())
                .build();
    }

    private BetAnalyticsItemResponse mapSicboBet(SicboBet bet) {
        BigDecimal stake = safeBigDecimal(bet.getStake());
        BigDecimal winAmount = safeBigDecimal(bet.getWinAmount());
        BigDecimal revenue = bet.getStatus() == SicboBet.Status.LOST ? stake : BigDecimal.ZERO;
        BigDecimal feeAmount = safeBigDecimal(bet.getFeeAmount());
        BigDecimal baoAmount = safeBigDecimal(bet.getBaoAmount());
        Integer tableNumber = bet.getSession() != null ? bet.getSession().getTableNumber() : null;

        return BetAnalyticsItemResponse.builder()
                .id(bet.getId())
                .gameType("SICBO")
                .username(bet.getUser() != null ? bet.getUser().getUsername() : null)
                .betCode(bet.getBetCode())
                .betType(bet.getBetCode())
                .stake(stake)
                .winAmount(winAmount)
                .revenue(revenue)
                .feeAmount(feeAmount)
                .baoAmount(baoAmount)
                .tableNumber(tableNumber)
                .status(bet.getStatus().name())
                .createdAt(bet.getCreatedAt() != null ? LocalDateTime.ofInstant(bet.getCreatedAt(), SYSTEM_ZONE) : null)
                .settledAt(bet.getSettledAt() != null ? LocalDateTime.ofInstant(bet.getSettledAt(), SYSTEM_ZONE) : null)
                .build();
    }

    private BetAnalyticsItemResponse mapXocDiaBet(XocDiaBet bet) {
        BigDecimal stake = safeBigDecimal(bet.getStake());
        BigDecimal winAmount = safeBigDecimal(bet.getWinAmount());
        BigDecimal revenue = bet.getStatus() == XocDiaBet.Status.LOST ? stake : BigDecimal.ZERO;
        BigDecimal feeAmount = safeBigDecimal(bet.getFeeAmount());

        return BetAnalyticsItemResponse.builder()
                .id(bet.getId())
                .gameType("XOCDIA")
                .username(bet.getUser() != null ? bet.getUser().getUsername() : null)
                .betCode(bet.getBetCode())
                .betType(bet.getBetCode())
                .stake(stake)
                .winAmount(winAmount)
                .revenue(revenue)
                .feeAmount(feeAmount)
                .baoAmount(BigDecimal.ZERO)
                .tableNumber(null)
                .status(bet.getStatus().name())
                .createdAt(bet.getCreatedAt() != null ? LocalDateTime.ofInstant(bet.getCreatedAt(), SYSTEM_ZONE) : null)
                .settledAt(bet.getSettledAt() != null ? LocalDateTime.ofInstant(bet.getSettledAt(), SYSTEM_ZONE) : null)
                .build();
    }

    private TransactionAnalyticsItemResponse mapTransaction(Transaction transaction) {
        // Đảm bảo processedBy được load để tránh LazyInitializationException
        String processedByUsername = null;
        try {
            if (transaction.getProcessedBy() != null) {
                processedByUsername = transaction.getProcessedBy().getUsername();
            }
        } catch (Exception e) {
            log.warn("Failed to load processedBy for transaction {}: {}", transaction.getId(), e.getMessage());
        }
        
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
                .processedByUsername(processedByUsername)
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
        if (dateTime == null) {
            return null;
        }
        // Nếu dateTime đã có timezone info (từ ISO string), parse trực tiếp
        // Nếu không, giả định là local time trong SYSTEM_ZONE
        return dateTime.atZone(SYSTEM_ZONE).toInstant();
    }
    
    /**
     * Convert LocalDateTime to Instant, đảm bảo endDate bao gồm cả ngày cuối (end of day)
     */
    private Instant toInstantEndOfDay(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        // Set về end of day để bao gồm cả ngày cuối
        LocalDateTime endOfDay = dateTime.toLocalDate().atTime(23, 59, 59, 999_999_999);
        return endOfDay.atZone(SYSTEM_ZONE).toInstant();
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

    /**
     * Tính tổng nạp, tổng rút, tổng hoàn trả, tổng hoa hồng đại lý theo date range
     */
    private FinancialSummary calculateFinancialSummary(LocalDateTime start, LocalDateTime end) {
        // Đảm bảo start là start of day và end là end of day
        LocalDateTime startDate = start != null ? start.toLocalDate().atStartOfDay() : null;
        LocalDateTime endDate = end != null ? end.toLocalDate().atTime(23, 59, 59, 999_999_999) : null;
        
        Instant startInstant = toInstant(startDate);
        Instant endInstant = toInstantEndOfDay(endDate);
        
        // Tổng nạp (DEPOSIT với status APPROVED hoặc COMPLETED)
        BigDecimal totalDeposit = safeBigDecimal(transactionRepository.sumNetAmountByFilters(
                Transaction.TransactionType.DEPOSIT,
                Transaction.TransactionStatus.APPROVED,
                startDate,
                endDate
        )).add(safeBigDecimal(transactionRepository.sumNetAmountByFilters(
                Transaction.TransactionType.DEPOSIT,
                Transaction.TransactionStatus.COMPLETED,
                startDate,
                endDate
        )));
        
        // Tổng rút (WITHDRAW với status APPROVED hoặc COMPLETED)
        BigDecimal totalWithdraw = safeBigDecimal(transactionRepository.sumNetAmountByFilters(
                Transaction.TransactionType.WITHDRAW,
                Transaction.TransactionStatus.APPROVED,
                startDate,
                endDate
        )).add(safeBigDecimal(transactionRepository.sumNetAmountByFilters(
                Transaction.TransactionType.WITHDRAW,
                Transaction.TransactionStatus.COMPLETED,
                startDate,
                endDate
        )));
        
        // Tổng hoàn trả từ GameRefundAccrual (hoàn trả theo lịch - status PAID)
        BigDecimal scheduledRefund = safeBigDecimal(gameRefundAccrualRepository.sumPaidRefundByDateRange(
                startInstant,
                endInstant
        ));
        
        // Tổng hoàn trả instant (từ PointTransaction với type BET_REFUND và referenceType SICBO_CASHBACK hoặc XOC_DIA_CASHBACK)
        BigDecimal instantRefund = safeBigDecimal(pointTransactionRepository.sumInstantGameRefundByDateRange(
                startDate,
                endDate
        ));
        
        // Tổng hoàn trả = scheduled + instant
        BigDecimal totalRefund = scheduledRefund.add(instantRefund);
        
        log.debug("Calculated totalRefund: scheduled={}, instant={}, total={} for date range: {} to {}", 
                scheduledRefund, instantRefund, totalRefund, startDate, endDate);
        
        // Tổng hoa hồng đại lý (AgentCommissionPayout với status PAID)
        BigDecimal totalAgentCommission = safeBigDecimal(agentCommissionPayoutRepository.sumPaidCommissionByDateRange(
                startDate,
                endDate
        ));
        
        // Tổng hoàn thua theo ngày (DailyLossRefund với status PAID)
        // Filter theo paidAt (thời gian thực tế hoàn trả) để đảm bảo tính đúng
        BigDecimal totalDailyLossRefund = safeBigDecimal(dailyLossRefundRepository.sumPaidRefundByDateRange(
                startInstant,
                endInstant
        ));
        
        log.debug("Calculated totalDailyLossRefund: {} for date range: {} to {} (instants: {} to {})", 
                totalDailyLossRefund, startDate, endDate, startInstant, endInstant);
        
        // Tổng khuyến mãi (PromotionalMoney)
        BigDecimal totalPromotionalMoney = safeBigDecimal(promotionalMoneyRepository.sumAmountByDateRange(
                startDate,
                endDate
        ));
        log.debug("Calculated totalPromotionalMoney: {} for date range: {} to {}", 
                totalPromotionalMoney, startDate, endDate);
        
        return new FinancialSummary(totalDeposit, totalWithdraw, totalRefund, totalDailyLossRefund, totalAgentCommission, totalPromotionalMoney);
    }

    private static class FinancialSummary {
        final BigDecimal totalDeposit;
        final BigDecimal totalWithdraw;
        final BigDecimal totalRefund;
        final BigDecimal totalDailyLossRefund;
        final BigDecimal totalAgentCommission;
        final BigDecimal totalPromotionalMoney;

        FinancialSummary(BigDecimal totalDeposit, BigDecimal totalWithdraw, BigDecimal totalRefund, BigDecimal totalDailyLossRefund, BigDecimal totalAgentCommission, BigDecimal totalPromotionalMoney) {
            this.totalDeposit = totalDeposit;
            this.totalWithdraw = totalWithdraw;
            this.totalRefund = totalRefund;
            this.totalDailyLossRefund = totalDailyLossRefund;
            this.totalAgentCommission = totalAgentCommission;
            this.totalPromotionalMoney = totalPromotionalMoney;
        }
    }
}


