package com.xsecret.service;

import com.xsecret.dto.response.XocDiaResultHistoryResponse;
import com.xsecret.dto.response.XocDiaResultHistoryResponse.Parity;
import com.xsecret.entity.XocDiaResultHistory;
import com.xsecret.entity.XocDiaSession;
import com.xsecret.repository.XocDiaResultHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class XocDiaResultHistoryService {

    private static final Map<String, Integer> RED_COUNT_MAP = new HashMap<>();

    static {
        RED_COUNT_MAP.put("four-white", 0);
        RED_COUNT_MAP.put("four-red", 4);
        RED_COUNT_MAP.put("three-white-one-red", 1);
        RED_COUNT_MAP.put("three-red-one-white", 3);
        RED_COUNT_MAP.put("two-two", 2);
    }

    private final XocDiaResultHistoryRepository resultHistoryRepository;

    @Transactional(readOnly = true)
    public List<XocDiaResultHistoryResponse> getRecentHistories(int limit, boolean ascending) {
        int sanitizedLimit = Math.min(Math.max(limit, 1), 240);
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, "recordedAt").and(
                Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, "id")
        );
        Pageable pageable = PageRequest.of(0, sanitizedLimit, sort);
        Page<XocDiaResultHistory> page = resultHistoryRepository.findAll(pageable);

        return page.getContent().stream()
                .map(history -> {
                    int redCount = resolveRedCount(history.getNormalizedResultCode());
                    Parity parity = redCount % 2 == 0 ? Parity.CHAN : Parity.LE;
                    return XocDiaResultHistoryResponse.fromEntity(history, redCount, parity);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void record(XocDiaSession session, String resultCode, Instant recordedAt) {
        if (session == null || resultCode == null || resultCode.isBlank()) {
            return;
        }
        String normalized = resultCode.trim().toLowerCase(Locale.ROOT);

        XocDiaResultHistory history = XocDiaResultHistory.builder()
                .session(session)
                .resultCode(resultCode)
                .normalizedResultCode(normalized)
                .recordedAt(recordedAt != null ? recordedAt : Instant.now())
                .build();
        resultHistoryRepository.save(history);
    }

    private int resolveRedCount(String normalizedCode) {
        if (normalizedCode == null || normalizedCode.isBlank()) {
            return 0;
        }

        String key = normalizedCode.trim().toLowerCase(Locale.ROOT);
        if (RED_COUNT_MAP.containsKey(key)) {
            return RED_COUNT_MAP.get(key);
        }

        if (key.contains("four-white")) {
            return 0;
        }
        if (key.contains("four-red")) {
            return 4;
        }
        if (key.contains("three-white-one-red")) {
            return 1;
        }
        if (key.contains("three-red-one-white") || key.contains("three-red")) {
            return 3;
        }
        if (key.contains("two-two")) {
            return 2;
        }
        if (key.contains("one-red")) {
            return 1;
        }

        // Fallback: count occurrences of "red" if pattern is unexpected
        int count = 0;
        int index = key.indexOf("red");
        while (index != -1) {
            count++;
            index = key.indexOf("red", index + 3);
        }
        return Math.min(Math.max(count, 0), 4);
    }
}


