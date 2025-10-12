package com.xsecret.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity lưu kết quả xổ số
 * - Miền Bắc: 1 bảng chung (province = null)
 * - Miền Trung Nam: mỗi tỉnh 1 bảng riêng (province = tên tỉnh)
 */
@Entity
@Table(name = "lottery_results", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"region", "province", "draw_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LotteryResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Vùng miền: mienBac, mienTrungNam
     */
    @Column(name = "region", nullable = false, length = 50)
    private String region;
    
    /**
     * Tỉnh (chỉ dùng cho Miền Trung Nam)
     * - NULL: Miền Bắc
     * - gialai, binhduong, ninhthuan, travinh, vinhlong: Miền Trung Nam
     */
    @Column(name = "province", length = 50)
    private String province;
    
    /**
     * Ngày quay thưởng
     */
    @Column(name = "draw_date", nullable = false)
    private LocalDate drawDate;
    
    /**
     * Kết quả các giải (JSON format)
     * 
     * Miền Bắc (7 giải):
     * {
     *   "dac-biet": "00943",
     *   "giai-nhat": "43213",
     *   "giai-nhi": ["66146", "15901"],
     *   "giai-ba": ["22906", "04955", "93893", "32538", "25660", "85773"],
     *   "giai-tu": ["8964", "0803", "4867", "2405"],
     *   "giai-nam": ["9122", "6281", "8813", "6672", "8101", "7293"],
     *   "giai-sau": ["803", "301", "325"],
     *   "giai-bay": ["84", "09", "69", "79"]
     * }
     * 
     * Miền Trung Nam (8 giải):
     * {
     *   "dac-biet": "042293",
     *   "giai-nhat": "02518",
     *   "giai-nhi": ["49226"],
     *   "giai-ba": ["03856", "04216"],
     *   "giai-tu": ["00810", "02321", "00681", "51728", "24507", "58068", "96136"],
     *   "giai-nam": ["8877"],
     *   "giai-sau": ["5934", "7442", "3430"],
     *   "giai-bay": ["884"],
     *   "giai-tam": ["40"]
     * }
     */
    @Column(name = "results", nullable = false, columnDefinition = "TEXT")
    private String results;
    
    /**
     * Trạng thái kết quả
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ResultStatus status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ResultStatus {
        DRAFT,      // Nháp - admin đang nhập
        PUBLISHED   // Đã công bố - dùng để check kết quả
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ResultStatus.DRAFT;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

