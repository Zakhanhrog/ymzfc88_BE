package com.xsecret.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "xoc_dia_bet",
        indexes = {
                @Index(name = "idx_xoc_dia_bet_session", columnList = "session_id"),
                @Index(name = "idx_xoc_dia_bet_user", columnList = "user_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class XocDiaBet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private XocDiaSession session;

    @Column(name = "bet_code", nullable = false, length = 100)
    private String betCode;

    @Column(name = "stake", nullable = false, precision = 18, scale = 2)
    private BigDecimal stake;

    @Column(name = "payout_multiplier", nullable = false, precision = 10, scale = 2)
    private BigDecimal payoutMultiplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "win_amount", precision = 18, scale = 2)
    private BigDecimal winAmount;

    @Column(name = "result_code", length = 100)
    private String resultCode;

    @Column(name = "settled_at")
    private Instant settledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum Status {
        PENDING,
        WON,
        LOST,
        REFUNDED
    }
}


