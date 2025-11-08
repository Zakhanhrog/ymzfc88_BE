package com.xsecret.controller;

import com.xsecret.dto.request.XocDiaSessionResultRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.XocDiaSessionResponse;
import com.xsecret.service.XocDiaSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/xoc-dia/session")
@RequiredArgsConstructor
public class XocDiaSessionAdminController {

    private final XocDiaSessionService sessionService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<XocDiaSessionResponse>> startNewSession() {
        try {
            XocDiaSessionResponse data = sessionService.startNewSession();
            return ResponseEntity.ok(ApiResponse.success("Bắt đầu phiên mới thành công", data));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<XocDiaSessionResponse>> getCurrentSession() {
        XocDiaSessionResponse data = sessionService.getCurrentSession();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/result")
    public ResponseEntity<ApiResponse<XocDiaSessionResponse>> submitResult(
            @Valid @RequestBody XocDiaSessionResultRequest request) {
        try {
            XocDiaSessionResponse data = sessionService.submitResult(request.getResultCode());
            return ResponseEntity.ok(ApiResponse.success("Lưu kết quả thành công", data));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }
}

