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
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "referral_code", unique = true, length = 10)
    private String referralCode;

    @Column(name = "invited_by_code", length = 10)
    private String invitedByCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "points")
    @Builder.Default
    private Long points = 0L;

    @Column(name = "kyc_verified")
    @Builder.Default
    private Boolean kycVerified = false;

    @Column(name = "withdrawal_locked")
    @Builder.Default
    private Boolean withdrawalLocked = false;

    @Column(name = "withdrawal_lock_reason")
    private String withdrawalLockReason;

    @Column(name = "withdrawal_locked_at")
    private LocalDateTime withdrawalLockedAt;

    @Column(name = "withdrawal_locked_by")
    private Long withdrawalLockedBy; // Admin user ID who locked

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_role")
    private StaffRole staffRole;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Role {
        USER, ADMIN
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, BANNED
    }

    public enum StaffRole {
        AGENT,
        STAFF_TX1,
        STAFF_TX2,
        STAFF_XD,
        STAFF_MKT,
        STAFF_XNK
    }
}
