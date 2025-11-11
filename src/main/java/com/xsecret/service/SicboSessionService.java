package com.xsecret.service;

import com.xsecret.dto.response.SicboSessionResponse;
import com.xsecret.entity.SicboSession;
import com.xsecret.repository.SicboSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class SicboSessionService {

    private static final int MAX_PHASE_ADVANCE_PER_CALL = 12;

    private final SicboSessionRepository sessionRepository;
    private final SicboBetService betService;

    @Transactional
    public SicboSessionResponse getCurrentSession(int tableNumber) {
        Instant now = Instant.now();

        return sessionRepository.findTopByTableNumberOrderByStartedAtDesc(tableNumber)
                .map(session -> {
                    boolean updated = false;
                    if (session.getTableNumber() == null) {
                        session.setTableNumber(tableNumber);
                        updated = true;
                    }
                    if (session.getStatus() != SicboSession.Status.RUNNING) {
                        if (updated) {
                            sessionRepository.save(session);
                        }
                        return SicboSessionResponse.idle(now).toBuilder().tableNumber(tableNumber).build();
                    }

                    updated |= initializePhaseIfNeeded(session, now);
                    updated |= advancePhaseIfNeeded(session, now);

                    SicboSession managed = updated ? sessionRepository.save(session) : session;
                    return SicboSessionResponse.fromEntity(managed, now);
                })
                .orElseGet(() -> SicboSessionResponse.idle(now).toBuilder().tableNumber(tableNumber).build());
    }

    @Transactional
    public SicboSessionResponse startNewSession(int tableNumber) {
        Instant now = Instant.now();

        sessionRepository.findTopByTableNumberOrderByStartedAtDesc(tableNumber)
                .ifPresent(session -> {
                    if (session.getStatus() == SicboSession.Status.RUNNING) {
                        log.info("Kết thúc phiên Sicbo đang chạy trước khi bắt đầu phiên mới. Table: {}, Session ID: {}", tableNumber, session.getId());
                        betService.refundUnsettledBets(session, "Phiên kết thúc khi bắt đầu phiên mới");
                        session.setStatus(SicboSession.Status.ENDED);
                        session.setEndedAt(now);
                        sessionRepository.save(session);
                    }
                });

        SicboSession newSession = SicboSession.builder()
                .startedAt(now)
                .status(SicboSession.Status.RUNNING)
                .phase(SicboSession.Phase.COUNTDOWN)
                .phaseStartedAt(now)
                .resultCode(null)
                .tableNumber(tableNumber)
                .build();

        SicboSession saved = sessionRepository.save(newSession);
        return SicboSessionResponse.fromEntity(saved, now);
    }

    @Transactional
    public SicboSessionResponse submitResult(String resultCode, int tableNumber) {
        Instant now = Instant.now();

        SicboSession session = sessionRepository.findTopByTableNumberOrderByStartedAtDesc(tableNumber)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy phiên Sicbo để lưu kết quả cho bàn " + tableNumber));

        if (session.getStatus() != SicboSession.Status.RUNNING) {
            throw new IllegalStateException("Phiên Sicbo hiện tại không hợp lệ để lưu kết quả");
        }

        initializePhaseIfNeeded(session, now);
        boolean advanced = advancePhaseIfNeeded(session, now);
        if (advanced) {
            session = sessionRepository.save(session);
        }

        if (session.getPhase() != SicboSession.Phase.SHOW_RESULT) {
            throw new IllegalStateException("Chỉ có thể lưu kết quả khi phiên đang ở trạng thái Trả kết quả");
        }

        session.setResultCode(resultCode);
        session.setPhase(SicboSession.Phase.PAYOUT);
        session.setPhaseStartedAt(now);

        SicboSession saved = sessionRepository.save(session);
        betService.settleBets(saved, resultCode);
        log.info("Đã lưu kết quả Sicbo cho session {} (bàn {}) với kết quả {}", saved.getId(), tableNumber, resultCode);
        return SicboSessionResponse.fromEntity(saved, now);
    }

    private boolean initializePhaseIfNeeded(SicboSession session, Instant referenceTime) {
        boolean updated = false;

        if (session.getPhase() == null) {
            session.setPhase(SicboSession.Phase.COUNTDOWN);
            updated = true;
        }

        if (session.getPhaseStartedAt() == null) {
            Instant phaseStart = session.getStartedAt() != null ? session.getStartedAt() : referenceTime;
            session.setPhaseStartedAt(phaseStart);
            updated = true;
        }

        return updated;
    }

    private boolean advancePhaseIfNeeded(SicboSession session, Instant referenceTime) {
        if (session.getPhase() == null || session.getPhaseStartedAt() == null) {
            return false;
        }

        boolean updated = false;
        Instant now = referenceTime != null ? referenceTime : Instant.now();
        int safetyCounter = 0;

        while (session.getPhase() != null
                && session.getPhaseStartedAt() != null
                && session.getPhase().getDurationMillis() != null) {
            long duration = session.getPhase().getDurationMillis();
            Instant phaseEnd = session.getPhaseStartedAt().plusMillis(duration);

            if (phaseEnd.isAfter(now)) {
                break;
            }

            SicboSession.Phase nextPhase = session.getPhase().next();
            session.setPhase(nextPhase);
            session.setPhaseStartedAt(phaseEnd);

            if (nextPhase == SicboSession.Phase.COUNTDOWN) {
                session.setResultCode(null);
            }

            updated = true;
            safetyCounter++;

            if (safetyCounter >= MAX_PHASE_ADVANCE_PER_CALL) {
                log.warn("Đã đạt giới hạn cập nhật phase trong một lần gọi cho phiên Sicbo {}", session.getId());
                break;
            }
        }

        return updated;
    }
}


