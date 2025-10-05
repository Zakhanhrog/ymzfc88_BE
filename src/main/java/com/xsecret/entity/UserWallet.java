package com.xsecret.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_wallets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWallet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Column(name = "total_deposit", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalDeposit = BigDecimal.ZERO;
    
    @Column(name = "total_withdraw", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalWithdraw = BigDecimal.ZERO;
    
    @Column(name = "total_bonus", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalBonus = BigDecimal.ZERO;
    
    @Column(name = "frozen_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal frozenAmount = BigDecimal.ZERO;
    
    @Column(name = "last_transaction_at")
    private LocalDateTime lastTransactionAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}