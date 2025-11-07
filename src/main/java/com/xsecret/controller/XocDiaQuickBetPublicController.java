package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.XocDiaQuickBetResponse;
import com.xsecret.service.XocDiaQuickBetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/xoc-dia/quick-bets")
@RequiredArgsConstructor
@Slf4j
public class XocDiaQuickBetPublicController {

    private final XocDiaQuickBetService quickBetService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<XocDiaQuickBetResponse>>> getActiveQuickBets() {
        try {
            List<XocDiaQuickBetResponse> data = quickBetService.getActiveConfigs();
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (RuntimeException ex) {
            log.error("Lỗi lấy danh sách quick bet Xóc Đĩa", ex);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Không thể lấy danh sách quick bet: " + ex.getMessage()));
        }
    }
}


