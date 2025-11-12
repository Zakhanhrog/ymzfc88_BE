package com.xsecret.dto.response;

import com.xsecret.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentInviteReferralResponse {
    private Long id;
    private String username;
    private String email;
    private User.UserStatus status;
    private LocalDateTime joinedAt;
}

