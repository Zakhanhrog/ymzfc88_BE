package com.xsecret.controller;

import com.xsecret.dto.request.SicboBetRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.SicboBetPlacementResponse;
import com.xsecret.entity.User;
import com.xsecret.service.SicboBetService;
import com.xsecret.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}


