package com.xsecret.dto.response;

import com.xsecret.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private User.Role role;
    private User.UserStatus status;
    private Double balance;
    private Boolean kycVerified;
    private Boolean withdrawalLocked;
    private String withdrawalLockReason;
    private LocalDateTime withdrawalLockedAt;
    private Long withdrawalLockedBy;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
