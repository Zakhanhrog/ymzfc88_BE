package com.xsecret.controller;

import com.xsecret.dto.response.AgentCommissionChartPointResponse;
import com.xsecret.dto.response.AgentCommissionPayoutResponse;
import com.xsecret.dto.response.AgentCommissionSummaryResponse;
import com.xsecret.dto.response.AgentCustomerBetHistoryResponse;
import com.xsecret.dto.response.AgentCustomerListResponse;
import com.xsecret.dto.response.AgentDashboardSummaryResponse;
import com.xsecret.dto.response.AgentInviteInfoResponse;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.entity.User;
import com.xsecret.entity.AgentCommissionPayout;
import com.xsecret.security.UserPrincipal;
import com.xsecret.service.AgentPortalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

@RestController
@RequestMapping("/admin/agent")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ROLE_AGENT_PORTAL')")
public class AgentPortalController {

    private final AgentPortalService agentPortalService;

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<AgentCustomerListResponse>> getAgentCustomers(
            @AuthenticationPrincipal UserPrincipal agentPrincipal,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) User.UserStatus status,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        log.info("Agent {} fetching customers with filters search={}, status={}, startDate={}, endDate={}, page={}, size={}",
                agentPrincipal.getUsername(), search, status, startDate, endDate, page, size);
        AgentCustomerListResponse response = agentPortalService.getAgentCustomers(
                agentPrincipal.getId(),
                search,
                status,
                startDate,
                endDate,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customers/{customerId}/bet-history")
    public ResponseEntity<ApiResponse<AgentCustomerBetHistoryResponse>> getCustomerBetHistory(
            @AuthenticationPrincipal UserPrincipal agentPrincipal,
            @PathVariable Long customerId,
            @RequestParam(value = "gameType", required = false) String gameType,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "size", defaultValue = "50") int size
    ) {
        log.info("Agent {} fetching bet history for customer {} with gameType={}, start={}, end={}, size={}",
                agentPrincipal.getUsername(), customerId, gameType, startDate, endDate, size);
        AgentCustomerBetHistoryResponse response = agentPortalService.getCustomerBetHistory(
                agentPrincipal.getId(),
                customerId,
                gameType,
                startDate,
                endDate,
                size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/invite-info")
    public ResponseEntity<ApiResponse<AgentInviteInfoResponse>> getInviteInfo(
            @AuthenticationPrincipal UserPrincipal agentPrincipal
    ) {
        log.info("Agent {} fetching invite info", agentPrincipal.getUsername());
        AgentInviteInfoResponse response = agentPortalService.getAgentInviteInfo(agentPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<AgentDashboardSummaryResponse>> getDashboardSummary(
            @AuthenticationPrincipal UserPrincipal agentPrincipal,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("Agent {} fetching dashboard summary", agentPrincipal.getUsername());
        AgentDashboardSummaryResponse response = agentPortalService.getAgentDashboardSummary(
                agentPrincipal.getId(),
                startDate,
                endDate
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/dashboard/chart")
    public ResponseEntity<ApiResponse<List<AgentCommissionChartPointResponse>>> getDashboardChart(
            @AuthenticationPrincipal UserPrincipal agentPrincipal,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("Agent {} fetching dashboard chart", agentPrincipal.getUsername());
        List<AgentCommissionChartPointResponse> chart = agentPortalService.getAgentCommissionChart(
                agentPrincipal.getId(),
                startDate,
                endDate
        );
        return ResponseEntity.ok(ApiResponse.success(chart));
    }

    @GetMapping("/commission/summary")
    public ResponseEntity<ApiResponse<AgentCommissionSummaryResponse>> getCommissionSummary(
            @AuthenticationPrincipal UserPrincipal agentPrincipal,
            @RequestParam(value = "month", required = false)
            @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        YearMonth targetMonth = month != null ? month : YearMonth.now();
        log.info("Agent {} fetching commission summary for {}", agentPrincipal.getUsername(), targetMonth);
        AgentCommissionSummaryResponse response = agentPortalService.getCommissionSummary(agentPrincipal.getId(), targetMonth);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/commission/history")
    public ResponseEntity<ApiResponse<Object>> getCommissionHistory(
            @AuthenticationPrincipal UserPrincipal agentPrincipal,
            @RequestParam(value = "status", required = false) AgentCommissionPayout.Status status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)));
        log.info("Agent {} fetching commission history status={}, page={}, size={}",
                agentPrincipal.getUsername(), status, page, size);
        Page<AgentCommissionPayoutResponse> payouts = agentPortalService.getCommissionPayoutHistory(
                agentPrincipal.getId(),
                status,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.success(buildPagedResponse(payouts)));
    }

    private Map<String, Object> buildPagedResponse(Page<?> page) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("items", page.getContent());
        payload.put("totalItems", page.getTotalElements());
        payload.put("totalPages", page.getTotalPages());
        payload.put("page", page.getNumber());
        payload.put("size", page.getSize());
        return payload;
    }
}

