package com.xsecret.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "xoc_dia_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class XocDiaSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", length = 50)
    private Phase phase;

    @Column(name = "phase_started_at")
    private Instant phaseStartedAt;

    @Column(name = "result_code", length = 100)
    private String resultCode;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum Status {
        RUNNING,
        ENDED
    }

    public enum Phase {
        COUNTDOWN("countdown", "", Duration.ofSeconds(30).toMillis()),
        BETTING_CLOSED("betting-closed", "Ngưng cược", 1_500L),
        WAITING_RESULT("waiting-result", "Chờ kết quả", 3_000L),
        SHOW_RESULT("show-result", "Trả kết quả", null),
        PAYOUT("payout", "Trả thưởng", 1_000L),
        INVITE_BET("invite-bet", "Mời đặt cược", 500L);

        private final String key;
        private final String label;
        private final Long durationMillis;

        Phase(String key, String label, Long durationMillis) {
            this.key = key;
            this.label = label;
            this.durationMillis = durationMillis;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        public Long getDurationMillis() {
            return durationMillis;
        }

        public boolean isAutoAdvance() {
            return durationMillis != null;
        }

        public Phase next() {
            Phase[] values = Phase.values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }
}

