package com.xsecret.controller;

import com.xsecret.dto.request.LoginRequest;
import com.xsecret.dto.request.RefreshTokenRequest;
import com.xsecret.dto.request.RegisterRequest;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login attempt for: {}", loginRequest.getUsernameOrEmail());
        
        JwtResponse jwtResponse = authService.login(loginRequest);
        
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", jwtResponse));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<JwtResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration attempt for username: {}", registerRequest.getUsername());
        
        JwtResponse jwtResponse = authService.register(registerRequest);
        
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công", jwtResponse));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Refresh token request");
        
        JwtResponse jwtResponse = authService.refreshToken(request);
        
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", jwtResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestBody RefreshTokenRequest request) {
        log.info("Logout request");
        
        authService.logout(request.getRefreshToken());
        
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Get current user info request: {}", userPrincipal.getUsername());
        
        User user = userService.getUserById(userPrincipal.getId());
        UserResponse userResponse = userMapper.toUserResponse(user);
        
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }
}
