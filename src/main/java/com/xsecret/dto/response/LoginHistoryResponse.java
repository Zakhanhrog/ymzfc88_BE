package com.xsecret.dto.response;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class LoginHistoryResponse {

    Long id;
    Long userId;
    String username;
    String fullName;
    String ipAddress;
    String userAgent;
    String portal;
    Boolean success;
    String failureReason;
    Instant loginAt;
}


