package com.xsecret.controller;

import com.xsecret.dto.request.UserPasswordChangeRequest;
import com.xsecret.dto.request.UserProfileUpdateRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.UserResponse;
import com.xsecret.entity.User;
import com.xsecret.mapper.UserMapper;
import com.xsecret.security.UserPrincipal;
import com.xsecret.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/profile")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class UserProfileController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("Fetching profile for user {}", principal.getUsername());
        User user = userService.getUserById(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(userMapper.toUserResponse(user)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        log.info("Updating profile for user {}", principal.getUsername());
        try {
            User updated = userService.updateUserProfile(principal.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", userMapper.toUserResponse(updated)));
        } catch (Exception e) {
            log.error("Error updating user profile", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserPasswordChangeRequest request
    ) {
        log.info("Changing password for user {}", principal.getUsername());
        try {
            userService.changeUserPassword(principal.getId(), request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.<Void>success("Đổi mật khẩu thành công", null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (Exception e) {
            log.error("Error changing user password", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Không thể đổi mật khẩu: " + e.getMessage()));
        }
    }
}

