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
 * Entity quản lý cấu hình quick bet cho game Xóc Đĩa
 */
@Entity
@Table(
        name = "xoc_dia_quick_bet_config",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"code"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class XocDiaQuickBetConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã định danh duy nhất cho quick bet (ví dụ: even, odd, two-two)
     */
    @Column(name = "code", nullable = false, unique = true, length = 100)
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
     * Tỷ lệ trả thưởng (ví dụ 1.96 nghĩa là 1 ăn 1.96)
     */
    @Column(name = "payout_multiplier", nullable = false, precision = 10, scale = 2)
    private BigDecimal payoutMultiplier;

    /**
     * Mẫu chấm trắng/đỏ hiển thị (ví dụ: white,white,red,red)
     */
    @Column(name = "pattern", length = 200)
    private String pattern;

    /**
     * Nhóm hiển thị trên giao diện (ví dụ: TOP, BOTTOM)
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

    public static final String GROUP_TOP = "TOP";
    public static final String GROUP_BOTTOM = "BOTTOM";
}


