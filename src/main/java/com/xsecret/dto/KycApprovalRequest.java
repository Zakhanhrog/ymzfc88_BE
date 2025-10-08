package com.xsecret.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KycApprovalRequest {
    private Long kycId;
    private String action; // "approve" or "reject"
    private String rejectedReason;
    private String adminNotes;
}

