package com.xsecret.controller;

import com.xsecret.dto.request.SicboSessionResultRequest;
import com.xsecret.dto.request.SicboSessionStartRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.SicboSessionResponse;
import com.xsecret.service.SicboSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/sicbo/session")
@RequiredArgsConstructor
public class SicboSessionAdminController {

    private final SicboSessionService sessionService;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<SicboSessionResponse>> startNewSession(
            @Valid @RequestBody SicboSessionStartRequest request) {
        try {
            SicboSessionResponse data = sessionService.startNewSession(request.getTableNumber());
            return ResponseEntity.ok(ApiResponse.success("Bắt đầu phiên Sicbo mới thành công", data));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SicboSessionResponse>> getCurrentSession(
            @RequestParam(name = "table", defaultValue = "1") Integer tableNumber) {
        SicboSessionResponse data = sessionService.getCurrentSession(tableNumber);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/result")
    public ResponseEntity<ApiResponse<SicboSessionResponse>> submitResult(
            @Valid @RequestBody SicboSessionResultRequest request) {
        try {
            SicboSessionResponse data = sessionService.submitResult(request.getResultCode(), request.getTableNumber());
            return ResponseEntity.ok(ApiResponse.success("Lưu kết quả Sicbo thành công", data));
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }
}


