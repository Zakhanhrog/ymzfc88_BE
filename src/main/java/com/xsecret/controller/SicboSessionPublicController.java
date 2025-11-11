package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.SicboSessionResponse;
import com.xsecret.service.SicboSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sicbo/session")
@RequiredArgsConstructor
public class SicboSessionPublicController {

    private final SicboSessionService sessionService;

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SicboSessionResponse>> getCurrentSession(
            @RequestParam(name = "table", defaultValue = "1") Integer tableNumber) {
        SicboSessionResponse data = sessionService.getCurrentSession(tableNumber);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}


