package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.SicboQuickBetResponse;
import com.xsecret.service.SicboQuickBetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sicbo/quick-bets")
@RequiredArgsConstructor
@Slf4j
public class SicboQuickBetPublicController {

    private final SicboQuickBetService quickBetService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SicboQuickBetResponse>>> getActiveQuickBets() {
        try {
            List<SicboQuickBetResponse> data = quickBetService.getActiveConfigs();
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (RuntimeException ex) {
            log.error("Lỗi lấy danh sách quick bet Sicbo", ex);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Không thể lấy danh sách quick bet: " + ex.getMessage()));
        }
    }
}


