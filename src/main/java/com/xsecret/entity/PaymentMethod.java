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
@Table(name = "payment_methods")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;
    
    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;
    
    @Column(name = "bank_code", length = 20)
    private String bankCode;
    
    @Column(name = "min_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal minAmount;
    
    @Column(name = "max_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal maxAmount;
    
    @Column(name = "fee_percent", precision = 5, scale = 2)
    private BigDecimal feePercent;
    
    @Column(name = "fee_fixed", precision = 15, scale = 2)
    private BigDecimal feeFixed;
    
    @Column(name = "processing_time", length = 50)
    private String processingTime;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "display_order")
    private Integer displayOrder;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "qr_code", length = 1000)
    private String qrCode;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum PaymentType {
        MOMO("Ví MoMo"),
        BANK("Ngân hàng"),
        USDT("USDT"),
        ZALO_PAY("ZaloPay"),
        VIET_QR("VietQR");
        
        private final String displayName;
        
        PaymentType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}