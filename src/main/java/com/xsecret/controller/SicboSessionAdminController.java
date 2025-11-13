package com.xsecret.controller;

import com.xsecret.dto.request.SicboSessionResultRequest;
import com.xsecret.dto.request.SicboSessionStartRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.SicboSessionResponse;
import com.xsecret.entity.User;
import com.xsecret.security.UserPrincipal;
import com.xsecret.service.SicboSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/sicbo/session")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_STAFF_TX1','ROLE_STAFF_TX2')")
public class SicboSessionAdminController {

    private final SicboSessionService sessionService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<SicboSessionResponse>> startNewSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SicboSessionStartRequest request) {
        assertTableAccess(principal, request.getTableNumber());
        try {
            SicboSessionResponse data = sessionService.startNewSession(request.getTableNumber());
            return ResponseEntity.ok(ApiResponse.success("Bắt đầu phiên Sicbo mới thành công", data));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SicboSessionResponse>> getCurrentSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name = "table", defaultValue = "1") Integer tableNumber) {
        assertTableAccess(principal, tableNumber);
        SicboSessionResponse data = sessionService.getCurrentSession(tableNumber);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/result")
    public ResponseEntity<ApiResponse<SicboSessionResponse>> submitResult(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SicboSessionResultRequest request) {
        assertTableAccess(principal, request.getTableNumber());
        try {
            SicboSessionResponse data = sessionService.submitResult(request.getResultCode(), request.getTableNumber());
            return ResponseEntity.ok(ApiResponse.success("Lưu kết quả Sicbo thành công", data));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    private void assertTableAccess(UserPrincipal principal, Integer tableNumber) {
        if (principal == null) {
            throw new AccessDeniedException("Bạn không có quyền thao tác bàn này");
        }
        if (tableNumber == null) {
            throw new AccessDeniedException("Thiếu thông tin bàn");
        }
        if (principal.getRole() == User.Role.ADMIN) {
            return;
        }
        User.StaffRole staffRole = principal.getStaffRole();
        if (staffRole == User.StaffRole.STAFF_TX1 && tableNumber == 1) {
            return;
        }
        if (staffRole == User.StaffRole.STAFF_TX2 && tableNumber == 2) {
            return;
        }
        throw new AccessDeniedException("Bạn không có quyền thao tác bàn này");
    }
}


