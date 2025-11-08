package com.xsecret.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xsecret.entity.XocDiaSession;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XocDiaSessionResponse {

    Long id;
    String status;
    Long startedAt;
    Long endedAt;
    Long serverTime;
    String phase;
    String phaseLabel;
    Long phaseStartedAt;
    Long phaseDurationMs;
    Long phaseRemainingMs;
    Boolean awaitingResult;
    String resultCode;

    public static XocDiaSessionResponse idle(Instant referenceTime) {
        return XocDiaSessionResponse.builder()
                .status("IDLE")
                .phase("idle")
                .phaseLabel("")
                .phaseStartedAt(referenceTime != null ? referenceTime.toEpochMilli() : null)
                .phaseDurationMs(null)
                .phaseRemainingMs(null)
                .awaitingResult(false)
                .serverTime(referenceTime != null ? referenceTime.toEpochMilli() : null)
                .build();
    }

    public static XocDiaSessionResponse fromEntity(XocDiaSession session, Instant referenceTime) {
        if (session == null) {
            return idle(referenceTime);
        }

        XocDiaSession.Phase phase = session.getPhase();
        Instant phaseStartedAtInstant = session.getPhaseStartedAt();

        Long phaseStartedAtMillis = phaseStartedAtInstant != null ? phaseStartedAtInstant.toEpochMilli() : null;
        Long phaseDurationMs = phase != null ? phase.getDurationMillis() : null;
        Long phaseRemainingMs = null;

        if (phaseDurationMs != null && phaseStartedAtMillis != null && referenceTime != null) {
            long elapsed = referenceTime.toEpochMilli() - phaseStartedAtMillis;
            long remaining = phaseDurationMs - elapsed;
            phaseRemainingMs = Math.max(0L, remaining);
        }

        return XocDiaSessionResponse.builder()
                .id(session.getId())
                .status(session.getStatus() != null ? session.getStatus().name() : "IDLE")
                .startedAt(session.getStartedAt() != null ? session.getStartedAt().toEpochMilli() : null)
                .endedAt(session.getEndedAt() != null ? session.getEndedAt().toEpochMilli() : null)
                .serverTime(referenceTime != null ? referenceTime.toEpochMilli() : null)
                .phase(phase != null ? phase.getKey() : null)
                .phaseLabel(phase != null ? phase.getLabel() : "")
                .phaseStartedAt(phaseStartedAtMillis)
                .phaseDurationMs(phaseDurationMs)
                .phaseRemainingMs(phaseRemainingMs)
                .awaitingResult(phase == XocDiaSession.Phase.SHOW_RESULT)
                .resultCode(session.getResultCode())
                .build();
    }
}

