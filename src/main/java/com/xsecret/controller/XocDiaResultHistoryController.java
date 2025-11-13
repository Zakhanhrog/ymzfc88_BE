package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.XocDiaResultHistoryResponse;
import com.xsecret.service.XocDiaResultHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/xoc-dia/result-history")
@RequiredArgsConstructor
public class XocDiaResultHistoryController {

    private final XocDiaResultHistoryService resultHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<XocDiaResultHistoryResponse>>> getResultHistory(
            @RequestParam(name = "limit", defaultValue = "120") int limit,
            @RequestParam(name = "order", defaultValue = "desc") String order
    ) {
        boolean ascending = "asc".equalsIgnoreCase(order);
        List<XocDiaResultHistoryResponse> histories = resultHistoryService.getRecentHistories(limit, ascending);
        return ResponseEntity.ok(ApiResponse.success(histories));
    }
}


