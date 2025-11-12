package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.SicboResultHistoryResponse;
import com.xsecret.service.SicboResultHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sicbo/result-history")
@RequiredArgsConstructor
public class SicboResultHistoryController {

    private final SicboResultHistoryService resultHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SicboResultHistoryResponse>>> getHistory(
            @RequestParam(name = "table", required = false) Integer tableNumber,
            @RequestParam(name = "limit", defaultValue = "90") int limit
    ) {
        List<SicboResultHistoryResponse> data = resultHistoryService.getRecent(tableNumber, limit);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}


