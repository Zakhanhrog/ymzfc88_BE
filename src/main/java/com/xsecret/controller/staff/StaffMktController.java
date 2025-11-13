package com.xsecret.controller.staff;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.StaffMktFinanceOverviewResponse;
import com.xsecret.dto.response.StaffMktGameOverviewResponse;
import com.xsecret.dto.response.StaffMktUserPageResponse;
import com.xsecret.entity.User;
import com.xsecret.security.UserPrincipal;
import com.xsecret.service.StaffMktService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/staff/mkt")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ROLE_STAFF_PORTAL')")
public class StaffMktController {

    private final StaffMktService staffMktService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<StaffMktUserPageResponse>> getUsers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) User.UserStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        log.info("Staff MKT {} fetching users with search={}, status={}, page={}, size={}",
                principal != null ? principal.getUsername() : "unknown",
                search,
                status,
                page,
                size);
        StaffMktUserPageResponse response = staffMktService.getUsers(principal, search, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/finance/overview")
    public ResponseEntity<ApiResponse<StaffMktFinanceOverviewResponse>> getFinanceOverview(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("Staff MKT {} fetching finance overview start={} end={}",
                principal != null ? principal.getUsername() : "unknown",
                startDate,
                endDate);
        StaffMktFinanceOverviewResponse response = staffMktService.getFinanceOverview(principal, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/game/overview")
    public ResponseEntity<ApiResponse<StaffMktGameOverviewResponse>> getGameOverview(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        log.info("Staff MKT {} fetching game overview", principal != null ? principal.getUsername() : "unknown");
        StaffMktGameOverviewResponse response = staffMktService.getGameOverview(principal);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

