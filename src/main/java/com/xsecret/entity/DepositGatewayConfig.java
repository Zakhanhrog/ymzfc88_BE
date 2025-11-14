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
@Table(name = "deposit_gateway_configs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_deposit_gateway_bank_code", columnNames = "bank_code"),
        @UniqueConstraint(name = "uk_deposit_gateway_channel_code", columnNames = "channel_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositGatewayConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_code", nullable = false, length = 50)
    private String bankCode;

    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @Column(name = "channel_code", nullable = false, length = 100)
    private String channelCode;

    @Column(name = "merchant_id", nullable = false, length = 120)
    private String merchantId;

    @Column(name = "api_key", nullable = false, length = 255)
    private String apiKey;

    @Column(name = "notify_url", length = 255)
    private String notifyUrl;

    @Column(name = "return_url", length = 255)
    private String returnUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "priority_order")
    private Integer priorityOrder;

    @Column(name = "min_amount", precision = 15, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

