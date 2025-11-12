package com.xsecret.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentCustomerListResponse {
    private List<AgentCustomerSummaryResponse> items;
    private long totalItems;
    private int totalPages;
    private int page;
    private int size;
    private double commissionRate;
}

