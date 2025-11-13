package com.xsecret.controller;

import com.xsecret.dto.request.SicboBetRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.SicboBetHistoryPageResponse;
import com.xsecret.dto.response.SicboBetPlacementResponse;
import com.xsecret.entity.User;
import com.xsecret.service.SicboBetService;
import com.xsecret.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sicbo/bets")
@RequiredArgsConstructor
@Slf4j
public class SicboBetController {

    private final SicboBetService betService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<SicboBetPlacementResponse>> placeBets(
            Authentication authentication,
            @Valid @RequestBody SicboBetRequest request
    ) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            SicboBetPlacementResponse response = betService.placeBets(user, request);
            return ResponseEntity.ok(ApiResponse.success("Đặt cược thành công", response));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            log.error("Không thể đặt cược Sicbo", ex);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Không thể đặt cược: " + ex.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<SicboBetHistoryPageResponse>> getBetHistory(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            SicboBetHistoryPageResponse response = betService.getUserBetHistory(user, page, size);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (RuntimeException ex) {
            log.error("Không thể tải lịch sử cược Sicbo", ex);
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (Exception ex) {
            log.error("Không thể tải lịch sử cược Sicbo", ex);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Không thể tải lịch sử cược: " + ex.getMessage()));
        }
    }
}


