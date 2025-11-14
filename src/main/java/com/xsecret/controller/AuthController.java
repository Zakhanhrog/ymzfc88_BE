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
import com.xsecret.service.UserLoginHistoryService;
import com.xsecret.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
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
    private final UserLoginHistoryService userLoginHistoryService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                          HttpServletRequest request) {
        log.info("Login attempt for: {}", loginRequest.getUsernameOrEmail());

        try {
            JwtResponse jwtResponse = authService.login(loginRequest);
            Long userId = jwtResponse.getUser() != null ? jwtResponse.getUser().getId() : null;
            userLoginHistoryService.recordLoginSuccess(userId, loginRequest.getUsernameOrEmail(), loginRequest.getPortal(), request);
            return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", jwtResponse));
        } catch (AuthenticationException ex) {
            log.warn("Authentication failed for {}: {}", loginRequest.getUsernameOrEmail(), ex.getMessage());
            userLoginHistoryService.recordLoginFailure(
                    loginRequest.getUsernameOrEmail(),
                    loginRequest.getPortal(),
                    "AUTHENTICATION_FAILED",
                    request
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Tên đăng nhập hoặc mật khẩu không đúng"));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.warn("Login failed for {}: {}", loginRequest.getUsernameOrEmail(), ex.getMessage());
            userLoginHistoryService.recordLoginFailure(
                    loginRequest.getUsernameOrEmail(),
                    loginRequest.getPortal(),
                    ex.getMessage(),
                    request
            );
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
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
