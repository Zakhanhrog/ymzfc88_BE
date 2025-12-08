package com.xsecret.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity để lưu tiền khuyến mại cho thống kê
 */
@Entity
@Table(name = "promotional_money", indexes = {
        @Index(name = "idx_promotional_money_user", columnList = "user_id"),
        @Index(name = "idx_promotional_money_created", columnList = "created_at"),
        @Index(name = "idx_promotional_money_transaction", columnList = "point_transaction_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PromotionalMoney {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_transaction_id", nullable = false, unique = true)
    private PointTransaction pointTransaction;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount; // Số tiền khuyến mại (điểm)

    @Column(name = "description", length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // Admin user

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

