package com.xsecret.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "front_image_url", nullable = false)
    private String frontImageUrl;

    @Column(name = "back_image_url", nullable = false)
    private String backImageUrl;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private KycStatus status = KycStatus.PENDING;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by")
    private Long verifiedBy; // Admin user ID

    @Column(name = "rejected_reason")
    private String rejectedReason;

    @Column(name = "admin_notes")
    private String adminNotes;

    public enum KycStatus {
        PENDING,      // Chờ duyệt
        APPROVED,     // Đã duyệt
        REJECTED      // Từ chối
    }

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        if (status == null) {
            status = KycStatus.PENDING;
        }
    }
}

