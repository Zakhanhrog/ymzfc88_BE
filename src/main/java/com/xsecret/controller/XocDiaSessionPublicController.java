package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.XocDiaSessionResponse;
import com.xsecret.service.XocDiaSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/xoc-dia/session")
@RequiredArgsConstructor
public class XocDiaSessionPublicController {

    private final XocDiaSessionService sessionService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<XocDiaSessionResponse>> getCurrentSession() {
        XocDiaSessionResponse data = sessionService.getCurrentSession();
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}

