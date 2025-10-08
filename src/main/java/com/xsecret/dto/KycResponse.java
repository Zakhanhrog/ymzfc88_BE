package com.xsecret.dto;

import com.xsecret.entity.KycVerification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycResponse {
    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String frontImageUrl;
    private String backImageUrl;
    private String idNumber;
    private String fullName;
    private String status;
    private LocalDateTime submittedAt;
    private LocalDateTime verifiedAt;
    private Long verifiedBy;
    private String rejectedReason;
    private String adminNotes;

    public static KycResponse fromEntity(KycVerification kyc) {
        return KycResponse.builder()
                .id(kyc.getId())
                .userId(kyc.getUser().getId())
                .username(kyc.getUser().getUsername())
                .email(kyc.getUser().getEmail())
                .frontImageUrl(kyc.getFrontImageUrl())
                .backImageUrl(kyc.getBackImageUrl())
                .idNumber(kyc.getIdNumber())
                .fullName(kyc.getFullName())
                .status(kyc.getStatus().name())
                .submittedAt(kyc.getSubmittedAt())
                .verifiedAt(kyc.getVerifiedAt())
                .verifiedBy(kyc.getVerifiedBy())
                .rejectedReason(kyc.getRejectedReason())
                .adminNotes(kyc.getAdminNotes())
                .build();
    }
}

