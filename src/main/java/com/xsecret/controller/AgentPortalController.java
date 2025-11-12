package com.xsecret.controller;

import com.xsecret.dto.response.AgentCustomerBetHistoryResponse;
import com.xsecret.dto.response.AgentCustomerListResponse;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.entity.User;
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
}

