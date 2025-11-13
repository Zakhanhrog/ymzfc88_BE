package com.xsecret.controller;

import com.xsecret.dto.request.XocDiaSessionResultRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.XocDiaSessionResponse;
import com.xsecret.entity.User;
import com.xsecret.security.UserPrincipal;
import com.xsecret.service.XocDiaSessionService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/xoc-dia/session")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_STAFF_XD')")
public class XocDiaSessionAdminController {

    private final XocDiaSessionService sessionService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<XocDiaSessionResponse>> startNewSession(
            @AuthenticationPrincipal UserPrincipal principal) {
        assertXocDiaAccess(principal);
        try {
            XocDiaSessionResponse data = sessionService.startNewSession();
            return ResponseEntity.ok(ApiResponse.success("Bắt đầu phiên mới thành công", data));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<XocDiaSessionResponse>> getCurrentSession(
            @AuthenticationPrincipal UserPrincipal principal) {
        assertXocDiaAccess(principal);
        XocDiaSessionResponse data = sessionService.getCurrentSession();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/result")
    public ResponseEntity<ApiResponse<XocDiaSessionResponse>> submitResult(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody XocDiaSessionResultRequest request) {
        assertXocDiaAccess(principal);
        try {
            XocDiaSessionResponse data = sessionService.submitResult(request.getResultCode());
            return ResponseEntity.ok(ApiResponse.success("Lưu kết quả thành công", data));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    private void assertXocDiaAccess(UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Bạn không có quyền thao tác bàn Xóc Đĩa");
        }
        if (principal.getRole() == User.Role.ADMIN) {
            return;
        }
        if (principal.getStaffRole() == User.StaffRole.STAFF_XD) {
            return;
        }
        throw new AccessDeniedException("Bạn không có quyền thao tác bàn Xóc Đĩa");
    }
}

