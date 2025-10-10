package com.xsecret.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "region", nullable = false, length = 50)
    private String region; // mienBac, mienTrungNam
    
    @Column(name = "bet_type", nullable = false, length = 50)
    private String betType; // loto2s, loto-xien-2, etc.
    
    @Column(name = "selected_numbers", nullable = false, columnDefinition = "TEXT")
    private String selectedNumbers; // JSON array of numbers
    
    @Column(name = "bet_amount", nullable = false)
    private BigDecimal betAmount; // Số điểm cược
    
    @Column(name = "price_per_point", nullable = false)
    private BigDecimal pricePerPoint; // Đơn giá 1 điểm
    
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount; // Tổng tiền cược
    
    @Column(name = "odds", nullable = false)
    private BigDecimal odds; // Tỷ lệ cược
    
    @Column(name = "potential_win", nullable = false)
    private BigDecimal potentialWin; // Tiền thắng tiềm năng
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BetStatus status;
    
    @Column(name = "is_win")
    private Boolean isWin;
    
    @Column(name = "win_amount")
    private BigDecimal winAmount;
    
    @Column(name = "winning_numbers", columnDefinition = "TEXT")
    private String winningNumbers; // JSON array of winning numbers
    
    @Column(name = "result_date")
    private String resultDate; // Ngày có kết quả (YYYY-MM-DD)
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "result_checked_at")
    private LocalDateTime resultCheckedAt;
    
    public enum BetStatus {
        PENDING,    // Chờ kết quả
        WON,        // Thắng
        LOST,       // Thua
        CANCELLED   // Hủy
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = BetStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
