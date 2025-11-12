package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentInviteInfoResponse {
    private String referralCode;
    private String invitePath;
    private long totalReferrals;
    private long activeReferrals;
    private List<AgentInviteReferralResponse> recentReferrals;
}

