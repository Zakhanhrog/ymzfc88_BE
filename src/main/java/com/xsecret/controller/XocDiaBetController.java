package com.xsecret.controller;

import com.xsecret.dto.request.XocDiaBetRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.XocDiaBetHistoryPageResponse;
import com.xsecret.dto.response.XocDiaBetPlacementResponse;
import com.xsecret.entity.User;
import com.xsecret.service.UserService;
import com.xsecret.service.XocDiaBetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/xoc-dia/bets")
@RequiredArgsConstructor
public class XocDiaBetController {

    private final XocDiaBetService betService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<XocDiaBetPlacementResponse>> placeBets(
            Authentication authentication,
            @Valid @RequestBody XocDiaBetRequest request
    ) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            XocDiaBetPlacementResponse response = betService.placeBets(user, request);
            return ResponseEntity.ok(ApiResponse.success("Đặt cược thành công", response));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Không thể đặt cược: " + ex.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<XocDiaBetHistoryPageResponse>> getBetHistory(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            XocDiaBetHistoryPageResponse response = betService.getUserBetHistory(user, page, size);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Không thể tải lịch sử cược: " + ex.getMessage()));
        }
    }
}


