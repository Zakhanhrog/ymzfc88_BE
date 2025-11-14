package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.LoginHistoryPageResponse;
import com.xsecret.service.UserLoginHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/login-history")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminLoginHistoryController {

    private final UserLoginHistoryService userLoginHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<LoginHistoryPageResponse>> getLoginHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String portal,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Fetching login history with filters userId={}, username={}, ip={}, portal={}, success={}, from={}, to={}, page={}, size={}",
                userId, username, ip, portal, success, from, to, page, size);

        LoginHistoryPageResponse response = userLoginHistoryService.search(
                userId,
                username,
                ip,
                portal,
                success,
                from,
                to,
                Math.max(page, 0),
                Math.max(1, Math.min(size, 200))
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}


