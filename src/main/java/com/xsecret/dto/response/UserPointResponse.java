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
public class UserPointResponse {
    
    private Long userId;
    private String username;
    private String fullName;
    private BigDecimal totalPoints;
    private BigDecimal lifetimeEarned;
    private BigDecimal lifetimeSpent;
    private LocalDateTime lastUpdated;
}