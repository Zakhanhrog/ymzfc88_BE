package com.xsecret.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionResponse {
    
    private Long id;
    private String transactionCode;
    private String type;
    private String typeDisplayName;
    private BigDecimal points;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String description;
    private String referenceType;
    private Long referenceId;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private UserInfo user;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String fullName;
    }
}