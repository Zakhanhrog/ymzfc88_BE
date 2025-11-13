package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffMktTransactionResponse {
    private Long id;
    private String transactionCode;
    private String username;
    private String fullName;
    private String type;
    private String status;
    private BigDecimal amount;
    private BigDecimal netAmount;
    private LocalDateTime createdAt;
}

