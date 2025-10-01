package com.xsecret.controller;

import com.xsecret.dto.request.LoginRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.JwtResponse;
import com.xsecret.dto.response.UserResponse;
import com.xsecret.entity.User;
import com.xsecret.mapper.UserMapper;
import com.xsecret.security.UserPrincipal;
import com.xsecret.service.AuthService;
import com.xsecret.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService authService;
    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ApiResponse<JwtResponse>> adminLogin(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Admin login attempt for: {}", loginRequest.getUsernameOrEmail());
        
        JwtResponse jwtResponse = authService.login(loginRequest);
        
        // Verify user is admin
        if (!jwtResponse.getUser().getRole().equals(User.Role.ADMIN)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Bạn không có quyền truy cập admin"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập admin thành công", jwtResponse));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<Object>> getDashboardStats() {
        log.info("Getting dashboard stats");
        
        var stats = userService.getDashboardStats();
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        log.info("Getting all users");
        
        List<User> users = userService.getAllUsers();
        List<UserResponse> userResponses = users.stream()
                .map(userMapper::toUserResponse)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(userResponses));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        log.info("Getting user by id: {}", id);
        
        User user = userService.getUserById(id);
        UserResponse userResponse = userMapper.toUserResponse(user);
        
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam User.UserStatus status) {
        log.info("Updating user {} status to {}", id, status);
        
        User user = userService.updateUserStatus(id, status);
        UserResponse userResponse = userMapper.toUserResponse(user);
        
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", userResponse));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getAdminProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Getting admin profile for: {}", userPrincipal.getUsername());
        
        User user = userService.getUserById(userPrincipal.getId());
        UserResponse userResponse = userMapper.toUserResponse(user);
        
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }
}
