package com.xsecret.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AdminUserBetSummaryResponse {

    List<AdminUserBetSummaryItemResponse> items;
    int page;
    int size;
    long totalItems;
    boolean hasMore;
}


