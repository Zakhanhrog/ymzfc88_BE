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
public class StaffMktUserPageResponse {
    private List<StaffMktUserResponse> items;
    private long totalItems;
    private int totalPages;
    private int page;
    private int size;
}

