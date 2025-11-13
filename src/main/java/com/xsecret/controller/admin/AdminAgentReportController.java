package com.xsecret.controller.admin;

import com.xsecret.dto.request.AdminAgentCommissionPayoutRequest;
import com.xsecret.dto.response.AdminAgentCommissionReportResponse;
import com.xsecret.dto.response.AdminAgentCommissionReportRowResponse;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.security.UserPrincipal;
import com.xsecret.service.AdminAgentReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/admin/agent/report")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAgentReportController {

    private final AdminAgentReportService adminAgentReportService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminAgentCommissionReportResponse>> getMonthlyReport(
            @RequestParam(value = "month", required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        log.info("Admin requesting agent commission report for {}", month);
        AdminAgentCommissionReportResponse response = adminAgentReportService.getMonthlyReport(month);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{agentId}/payout")
    public ResponseEntity<ApiResponse<AdminAgentCommissionReportRowResponse>> payoutCommission(
            @PathVariable Long agentId,
            @Valid @RequestBody AdminAgentCommissionPayoutRequest request,
            @AuthenticationPrincipal UserPrincipal adminPrincipal
    ) {
        log.info("Admin {} paying commission for agent {} in month {}", adminPrincipal.getId(), agentId, request.getMonth());
        AdminAgentCommissionReportRowResponse response = adminAgentReportService
                .payoutCommission(agentId, request, adminPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Chia hoa hồng thành công", response));
    }
}

