package com.xsecret.service;

import com.xsecret.dto.request.LoginRequest;
import com.xsecret.dto.request.RefreshTokenRequest;
import com.xsecret.dto.request.RegisterRequest;
import com.xsecret.dto.response.JwtResponse;
import com.xsecret.dto.response.UserResponse;
import com.xsecret.entity.RefreshToken;
import com.xsecret.entity.User;
import com.xsecret.exception.TokenRefreshException;
import com.xsecret.exception.UserAlreadyExistsException;
import com.xsecret.mapper.UserMapper;
import com.xsecret.repository.UserRepository;
import com.xsecret.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final TelegramNotificationService telegramNotificationService;
    private final UserMapper userMapper;
    private final UserService userService;

    public JwtResponse login(LoginRequest loginRequest) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token
        String jwt = jwtUtils.generateJwtToken(authentication);

        // Get user details
        String username = authentication.getName();
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // Convert to response
        UserResponse userResponse = userMapper.toUserResponse(user);

        return JwtResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtUtils.getJwtExpirationMs())
                .user(userResponse)
                .build();
    }

    public JwtResponse register(RegisterRequest registerRequest) {
        // Validate passwords match
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        // Check if username exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UserAlreadyExistsException("Tên đăng nhập đã tồn tại");
        }

        // Check if email exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new UserAlreadyExistsException("Email đã được sử dụng");
        }

        String normalizedInvite = normalizeCode(registerRequest.getInviteCode());
        if (normalizedInvite != null && userRepository.findByReferralCode(normalizedInvite).isEmpty()) {
            throw new IllegalArgumentException("Mã mời không hợp lệ");
        }

        // Create new user
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName())
                .phoneNumber(registerRequest.getPhoneNumber())
                .role(User.Role.USER)
                .invitedByCode(normalizedInvite)
                .referralCode(userService.generateUniqueReferralCode())
                .status(User.UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Đăng ký thành công cho user: {}", savedUser.getUsername());
        
        // Send Telegram notification
        try {
            log.info("Attempting to send Telegram notification for user registration: {}", savedUser.getUsername());
            String message = "Người dùng " + telegramNotificationService.formatBoldText(savedUser.getUsername()) + " đã đăng ký tài khoản thành công.";
            telegramNotificationService.sendMessage(message);
            log.info("Telegram notification sent successfully for user: {}", savedUser.getUsername());
        } catch (Exception e) {
            log.error("Failed to send Telegram notification for user registration: {}", e.getMessage(), e);
        }

        // Auto login after registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.getUsername(),
                        registerRequest.getPassword())
        );

        String jwt = jwtUtils.generateJwtToken(authentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);
        UserResponse userResponse = userMapper.toUserResponse(savedUser);

        return JwtResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtUtils.getJwtExpirationMs())
                .user(userResponse)
                .build();
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    public JwtResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtils.generateJwtToken(user.getUsername());
                    UserResponse userResponse = userMapper.toUserResponse(user);
                    
                    return JwtResponse.builder()
                            .accessToken(token)
                            .refreshToken(requestRefreshToken)
                            .expiresIn(jwtUtils.getJwtExpirationMs())
                            .user(userResponse)
                            .build();
                })
                .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token không hợp lệ"));
    }

    public void logout(String refreshToken) {
        refreshTokenService.deleteByToken(refreshToken);
        SecurityContextHolder.clearContext();
    }
}
