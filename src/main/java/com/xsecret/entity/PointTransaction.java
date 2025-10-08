package com.xsecret.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "transaction_code", unique = true, nullable = false, length = 20)
    private String transactionCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PointTransactionType type;

    @Column(name = "points", nullable = false, precision = 10, scale = 0)
    private BigDecimal points;

    @Column(name = "balance_before", nullable = false, precision = 10, scale = 0)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 10, scale = 0)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "reference_type", length = 50)
    private String referenceType; // DEPOSIT, ADMIN_ADJUSTMENT, PURCHASE, etc.

    @Column(name = "reference_id")
    private Long referenceId; // ID của transaction gốc (nếu có)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // Admin user nếu là manual adjustment

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum PointTransactionType {
        EARN("Nhận điểm"),
        SPEND("Tiêu điểm"),
        ADMIN_ADD("Admin cộng điểm"),
        ADMIN_SUBTRACT("Admin trừ điểm"),
        DEPOSIT_BONUS("Điểm từ nạp tiền"),
        WITHDRAW_DEDUCTION("Trừ điểm khi rút tiền"),
        REFUND("Hoàn điểm");

        private final String displayName;

        PointTransactionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}