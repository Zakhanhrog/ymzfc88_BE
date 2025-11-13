package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffMktFinanceOverviewResponse {
    private BigDecimal totalDepositAmount;
    private BigDecimal totalWithdrawAmount;
    private long approvedDepositCount;
    private long approvedWithdrawCount;
    private long pendingDepositCount;
    private long pendingWithdrawCount;
    private List<StaffMktTransactionResponse> recentTransactions;
}

