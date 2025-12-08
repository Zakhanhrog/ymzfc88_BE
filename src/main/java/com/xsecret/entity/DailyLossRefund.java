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
import java.time.LocalDate;

@Entity
@Table(
        name = "daily_loss_refund",
        indexes = {
                @Index(name = "idx_daily_loss_refund_user_date", columnList = "user_id,refund_date"),
                @Index(name = "idx_daily_loss_refund_status", columnList = "status"),
                @Index(name = "idx_daily_loss_refund_date", columnList = "refund_date")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class DailyLossRefund {

    public enum Status {
        PENDING,
        PAID,
        FAILED,
        SKIPPED // Khi tổng thắng > tổng thua (không cần hoàn)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refund_date", nullable = false)
    private LocalDate refundDate;

    @Column(name = "total_win_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalWinAmount;

    @Column(name = "total_loss_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalLossAmount;

    @Column(name = "net_loss_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal netLossAmount; // totalWin - totalLoss (âm nếu thua nhiều hơn thắng)

    @Column(name = "refund_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal refundPercentage;

    @Column(name = "refund_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal refundAmount; // netLossAmount * refundPercentage (chỉ tính khi netLossAmount < 0)

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "payout_at")
    private Instant payoutAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "description", length = 500)
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}

