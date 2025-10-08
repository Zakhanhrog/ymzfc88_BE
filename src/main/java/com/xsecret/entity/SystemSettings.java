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

@Entity
@Table(name = "system_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SystemSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", unique = true, nullable = false)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "description")
    private String description;

    @Column(name = "category")
    private String category;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Các setting keys mặc định
    public static final String DEFAULT_WITHDRAWAL_LOCK_REASON = "default_withdrawal_lock_reason";
    public static final String SYSTEM_MAINTENANCE_MESSAGE = "system_maintenance_message";
    public static final String MIN_WITHDRAWAL_AMOUNT = "min_withdrawal_amount";
    public static final String MAX_WITHDRAWAL_AMOUNT = "max_withdrawal_amount";
    public static final String MIN_DEPOSIT_AMOUNT = "min_deposit_amount";
    public static final String MAX_DEPOSIT_AMOUNT = "max_deposit_amount";
}

