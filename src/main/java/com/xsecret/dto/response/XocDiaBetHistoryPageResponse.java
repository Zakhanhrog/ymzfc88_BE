package com.xsecret.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XocDiaBetHistoryPageResponse {

    private List<XocDiaBetHistoryItemResponse> items;
    private int page;
    private int size;
    private long totalItems;
    private boolean hasMore;
}


