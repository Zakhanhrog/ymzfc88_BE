package com.xsecret.dto.response;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class LoginHistoryPageResponse {

    List<LoginHistoryResponse> items;
    int page;
    int size;
    long totalItems;
    boolean hasMore;
}


