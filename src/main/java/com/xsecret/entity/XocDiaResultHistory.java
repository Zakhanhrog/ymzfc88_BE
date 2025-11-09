package com.xsecret.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
        name = "xoc_dia_result_history",
        indexes = {
                @Index(name = "idx_xoc_dia_result_history_recorded_at", columnList = "recorded_at"),
                @Index(name = "idx_xoc_dia_result_history_normalized_code", columnList = "normalized_result_code"),
                @Index(name = "idx_xoc_dia_result_history_session", columnList = "session_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class XocDiaResultHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private XocDiaSession session;

    @Column(name = "result_code", length = 100)
    private String resultCode;

    @Column(name = "normalized_result_code", length = 100)
    private String normalizedResultCode;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "total_bets", nullable = false)
    @Builder.Default
    private Integer totalBets = 0;

    @Column(name = "winning_bets", nullable = false)
    @Builder.Default
    private Integer winningBets = 0;

    @Column(name = "losing_bets", nullable = false)
    @Builder.Default
    private Integer losingBets = 0;

    @Column(name = "total_stake", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalStake = BigDecimal.ZERO;

    @Column(name = "total_payout", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalPayout = BigDecimal.ZERO;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}


