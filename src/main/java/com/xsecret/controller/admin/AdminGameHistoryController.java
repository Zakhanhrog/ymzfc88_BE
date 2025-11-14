package com.xsecret.controller.admin;

import com.xsecret.dto.response.AdminGameBetHistoryResponse;
import com.xsecret.dto.response.AdminUserBetDetailResponse;
import com.xsecret.dto.response.AdminUserBetSummaryResponse;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.service.AdminGameHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/game-history")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_STAFF_XNK')")
public class AdminGameHistoryController {

    private final AdminGameHistoryService adminGameHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminGameBetHistoryResponse>> getGameHistory(
            @RequestParam(value = "gameType", required = false) String gameType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        log.info("Admin request game history - gameType={}, status={}, start={}, end={}, page={}, size={}",
                gameType, status, startDate, endDate, page, size);
        AdminGameBetHistoryResponse response = adminGameHistoryService.getGameHistory(
                gameType,
                status,
                startDate,
                endDate,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user-summary")
    public ResponseEntity<ApiResponse<AdminUserBetSummaryResponse>> getUserBetSummary(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        log.info("Admin request user bet summary - search={}, page={}, size={}", search, page, size);
        AdminUserBetSummaryResponse response = adminGameHistoryService.getUserBetSummaries(search, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user-detail")
    public ResponseEntity<ApiResponse<AdminUserBetDetailResponse>> getUserBetDetail(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "gameType", required = false) String gameType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        log.info("Admin request user bet detail - userId={}, gameType={}, page={}, size={}", userId, gameType, page, size);
        AdminUserBetDetailResponse response = adminGameHistoryService.getUserBetDetail(userId, gameType, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

