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

@Entity
@Table(name = "user_points")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_points", nullable = false, precision = 10, scale = 0)
    @Builder.Default
    private BigDecimal totalPoints = BigDecimal.ZERO;

    @Column(name = "lifetime_earned", nullable = false, precision = 10, scale = 0)
    @Builder.Default
    private BigDecimal lifetimeEarned = BigDecimal.ZERO;

    @Column(name = "lifetime_spent", nullable = false, precision = 10, scale = 0)
    @Builder.Default
    private BigDecimal lifetimeSpent = BigDecimal.ZERO;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}