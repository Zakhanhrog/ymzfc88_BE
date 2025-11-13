package com.xsecret.service;

import com.xsecret.dto.response.XocDiaSessionResponse;
import com.xsecret.entity.XocDiaSession;
import com.xsecret.repository.XocDiaSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class XocDiaSessionService {

    private static final int MAX_PHASE_ADVANCE_PER_CALL = 12;

    private final XocDiaSessionRepository sessionRepository;
    private final XocDiaBetService betService;
    private final XocDiaResultHistoryService resultHistoryService;

    @Transactional
    public XocDiaSessionResponse getCurrentSession() {
        Instant now = Instant.now();

        return sessionRepository.findTopByOrderByStartedAtDesc()
                .map(session -> {
                    if (session.getStatus() != XocDiaSession.Status.RUNNING) {
                        return XocDiaSessionResponse.idle(now);
                    }

                    boolean updated = initializePhaseIfNeeded(session, now);
                    updated |= advancePhaseIfNeeded(session, now);

                    XocDiaSession managed = updated ? sessionRepository.save(session) : session;
                    return XocDiaSessionResponse.fromEntity(managed, now);
                })
                .orElseGet(() -> XocDiaSessionResponse.idle(now));
    }

    @Transactional
    public XocDiaSessionResponse startNewSession() {
        Instant now = Instant.now();

        sessionRepository.findTopByOrderByStartedAtDesc()
                .ifPresent(session -> {
                    if (session.getStatus() == XocDiaSession.Status.RUNNING) {
                        betService.refundUnsettledBets(session, "Phiên kết thúc khi bắt đầu phiên mới");
                        session.setStatus(XocDiaSession.Status.ENDED);
                        session.setEndedAt(now);
                    }
                    sessionRepository.save(session);
                });

        XocDiaSession newSession = XocDiaSession.builder()
                .startedAt(now)
                .status(XocDiaSession.Status.RUNNING)
                .phase(XocDiaSession.Phase.COUNTDOWN)
                .phaseStartedAt(now)
                .resultCode(null)
                .build();

        XocDiaSession saved = sessionRepository.save(newSession);
        return XocDiaSessionResponse.fromEntity(saved, now);
    }

    @Transactional
    public XocDiaSessionResponse submitResult(String resultCode) {
        Instant now = Instant.now();

        XocDiaSession session = sessionRepository.findTopByOrderByStartedAtDesc()
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy phiên Xóc Đĩa để lưu kết quả"));

        if (session.getStatus() != XocDiaSession.Status.RUNNING) {
            throw new IllegalStateException("Phiên Xóc Đĩa hiện tại không hợp lệ để lưu kết quả");
        }

        initializePhaseIfNeeded(session, now);
        boolean advanced = advancePhaseIfNeeded(session, now);
        if (advanced) {
            session = sessionRepository.save(session);
        }

        if (session.getPhase() != XocDiaSession.Phase.SHOW_RESULT) {
            throw new IllegalStateException("Chỉ có thể lưu kết quả khi phiên đang ở trạng thái Trả kết quả");
        }

        session.setResultCode(resultCode);
        session.setPhase(XocDiaSession.Phase.PAYOUT);
        session.setPhaseStartedAt(now);

        XocDiaSession saved = sessionRepository.save(session);
        betService.settleBets(saved, resultCode);
        resultHistoryService.record(saved, resultCode, now);
        return XocDiaSessionResponse.fromEntity(saved, now);
    }

    private boolean initializePhaseIfNeeded(XocDiaSession session, Instant referenceTime) {
        boolean updated = false;

        if (session.getPhase() == null) {
            session.setPhase(XocDiaSession.Phase.COUNTDOWN);
            updated = true;
        }

        if (session.getPhaseStartedAt() == null) {
            Instant phaseStart = session.getStartedAt() != null ? session.getStartedAt() : referenceTime;
            session.setPhaseStartedAt(phaseStart);
            updated = true;
        }

        return updated;
    }

    private boolean advancePhaseIfNeeded(XocDiaSession session, Instant referenceTime) {
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

            XocDiaSession.Phase nextPhase = session.getPhase().next();
            session.setPhase(nextPhase);
            session.setPhaseStartedAt(phaseEnd);

            if (nextPhase == XocDiaSession.Phase.COUNTDOWN) {
                session.setResultCode(null);
            }

            updated = true;
            safetyCounter++;

            if (safetyCounter >= MAX_PHASE_ADVANCE_PER_CALL) {
                break;
            }
        }

        return updated;
    }
}
