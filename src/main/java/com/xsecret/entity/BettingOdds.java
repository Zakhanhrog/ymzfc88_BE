package com.xsecret.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity để quản lý tỷ lệ cược cho các loại hình xổ số
 */
@Entity
@Table(name = "betting_odds", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"region", "bet_type"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class BettingOdds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Khu vực: MIEN_BAC hoặc MIEN_TRUNG_NAM
     */
    @Column(name = "region", nullable = false, length = 50)
    private String region;

    /**
     * Loại cược (ví dụ: loto2s, loto-xien-2, dac-biet, etc.)
     */
    @Column(name = "bet_type", nullable = false, length = 100)
    private String betType;

    /**
     * Tên hiển thị của loại cược
     */
    @Column(name = "bet_name", nullable = false, length = 200)
    private String betName;

    /**
     * Mô tả loại cược
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Tỷ lệ cược (1 ăn bao nhiêu)
     * Ví dụ: 99 nghĩa là 1 ăn 99
     */
    @Column(name = "odds", nullable = false)
    private Integer odds;

    /**
     * Đơn giá 1 điểm (đơn vị: VNĐ)
     */
    @Column(name = "price_per_point", nullable = false)
    private Integer pricePerPoint;

    /**
     * Trạng thái active/inactive
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constants cho region
    public static final String REGION_MIEN_BAC = "MIEN_BAC";
    public static final String REGION_MIEN_TRUNG_NAM = "MIEN_TRUNG_NAM";
}

