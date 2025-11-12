package com.xsecret.service;

import com.xsecret.dto.response.SicboResultHistoryResponse;
import com.xsecret.entity.SicboResultHistory;
import com.xsecret.entity.SicboSession;
import com.xsecret.repository.SicboResultHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SicboResultHistoryService {

    private final SicboResultHistoryRepository repository;

    @Transactional
    public void record(SicboSession session, String resultCode, int resultSum, SicboResultHistory.Category category, Instant recordedAt) {
        if (session == null) {
            return;
        }
        SicboResultHistory history = SicboResultHistory.builder()
                .session(session)
                .tableNumber(session.getTableNumber())
                .resultCode(resultCode)
                .resultSum(resultSum)
                .category(category)
                .recordedAt(recordedAt != null ? recordedAt : Instant.now())
                .build();
        repository.save(history);
    }

    @Transactional(readOnly = true)
    public List<SicboResultHistoryResponse> getRecent(Integer tableNumber, int limit) {
        int sanitizedLimit = Math.min(Math.max(limit, 1), 120);
        return repository.findLatest(tableNumber, PageRequest.of(0, sanitizedLimit)).stream()
                .map(SicboResultHistoryResponse::fromEntity)
                .toList();
    }
}


