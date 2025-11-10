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
 * Entity quản lý cấu hình quick bet cho game Sicbo
 */
@Entity
@Table(
        name = "sicbo_quick_bet_config",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"code"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SicboQuickBetConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã định danh duy nhất cho quick bet (ví dụ: sicbo_small, sicbo_total_4)
     */
    @Column(name = "code", nullable = false, unique = true, length = 120)
    private String code;

    /**
     * Tên hiển thị
     */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Mô tả thêm (tuỳ chọn)
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Tỷ lệ trả thưởng (ví dụ 0.97 nghĩa là 1 ăn 0.97)
     */
    @Column(name = "payout_multiplier", nullable = false, precision = 10, scale = 2)
    private BigDecimal payoutMultiplier;

    /**
     * Nhóm hiển thị trên giao diện (ví dụ: PRIMARY, COMBINATION, TOTAL_TOP, TOTAL_BOTTOM, SINGLE)
     */
    @Column(name = "layout_group", nullable = false, length = 50)
    private String layoutGroup;

    /**
     * Thứ tự hiển thị trong nhóm
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /**
     * Trạng thái sử dụng
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

    public static final String GROUP_PRIMARY = "PRIMARY";
    public static final String GROUP_COMBINATION = "COMBINATION";
    public static final String GROUP_TOTAL_TOP = "TOTAL_TOP";
    public static final String GROUP_TOTAL_BOTTOM = "TOTAL_BOTTOM";
    public static final String GROUP_SINGLE = "SINGLE";
    public static final String GROUP_PARITY = "PARITY";
}


