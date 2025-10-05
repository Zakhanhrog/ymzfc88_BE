package com.xsecret.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_payment_methods")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPaymentMethod {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PaymentMethod.PaymentType type;
    
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;
    
    @Column(name = "account_name", nullable = false, length = 255)
    private String accountName;
    
    @Column(name = "bank_code", length = 20)
    private String bankCode;
    
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
    
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;
    
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = true; // Mặc định là đã xác thực
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Helper method để kiểm tra xem có phải phương thức ngân hàng không
    public boolean isBankMethod() {
        return PaymentMethod.PaymentType.BANK.equals(this.type);
    }
    
    // Helper method để lấy tên hiển thị đầy đủ
    public String getDisplayName() {
        if (isBankMethod() && bankCode != null) {
            return String.format("%s - %s (%s)", name, bankCode, accountNumber);
        }
        return String.format("%s (%s)", name, accountNumber);
    }
}