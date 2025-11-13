package com.xsecret.service;

import com.xsecret.dto.response.AdminGameBetHistoryItemResponse;
import com.xsecret.dto.response.AdminGameBetHistoryResponse;
import com.xsecret.entity.Bet;
import com.xsecret.entity.SicboBet;
import com.xsecret.entity.XocDiaBet;
import com.xsecret.repository.BetRepository;
import com.xsecret.repository.SicboBetRepository;
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
}

