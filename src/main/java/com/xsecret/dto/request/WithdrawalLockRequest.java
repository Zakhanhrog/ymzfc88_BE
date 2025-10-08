package com.xsecret.dto.request;

import lombok.Data;

@Data
public class WithdrawalLockRequest {
    private Long userId;
    private Boolean locked; // true = lock, false = unlock
    private String reason; // Required when locking
}

