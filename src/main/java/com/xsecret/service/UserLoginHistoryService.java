package com.xsecret.service;

import com.xsecret.dto.response.LoginHistoryPageResponse;
import com.xsecret.dto.response.LoginHistoryResponse;
import com.xsecret.entity.User;
import com.xsecret.entity.UserLoginHistory;
import com.xsecret.repository.UserLoginHistoryRepository;
import com.xsecret.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserLoginHistoryService {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    private final UserLoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;

    public void recordLoginSuccess(Long userId, String usernameOrEmail, String portal, HttpServletRequest request) {
        recordLoginAttempt(userId, usernameOrEmail, portal, request, true, null);
    }

    public void recordLoginFailure(String usernameOrEmail, String portal, String failureReason, HttpServletRequest request) {
        recordLoginAttempt(null, usernameOrEmail, portal, request, false, failureReason);
    }

    @Transactional(readOnly = true)
    public LoginHistoryPageResponse search(
            Long userId,
            String username,
            String ipAddress,
            String portal,
            Boolean success,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "loginAt"));

        Specification<UserLoginHistory> specification = Specification.where(null);

        if (userId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.join("user").get("id"), userId));
        }

        if (StringUtils.hasText(username)) {
            String likeValue = "%" + username.toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("usernameOrEmail")), likeValue));
        }

        if (StringUtils.hasText(ipAddress)) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("ipAddress"), ipAddress.trim()));
        }

        if (StringUtils.hasText(portal)) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("portal"), normalizePortal(portal)));
        }

        if (success != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("success"), success));
        }

        if (from != null) {
            Instant fromInstant = from.atZone(ZoneId.systemDefault()).toInstant();
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("loginAt"), fromInstant));
        }

        if (to != null) {
            Instant toInstant = to.atZone(ZoneId.systemDefault()).toInstant();
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("loginAt"), toInstant));
        }

        Page<UserLoginHistory> historyPage = loginHistoryRepository.findAll(specification, pageable);

        return LoginHistoryPageResponse.builder()
                .items(historyPage.map(this::mapToResponse).getContent())
                .page(historyPage.getNumber())
                .size(historyPage.getSize())
                .totalItems(historyPage.getTotalElements())
                .hasMore(historyPage.hasNext())
                .build();
    }

    private void recordLoginAttempt(Long userId, String usernameOrEmail, String portal, HttpServletRequest request, boolean success, String failureReason) {
        try {
            UserLoginHistory.UserLoginHistoryBuilder builder = UserLoginHistory.builder()
                    .usernameOrEmail(trimToNull(usernameOrEmail))
                    .portal(normalizePortal(portal))
                    .success(success)
                    .failureReason(trimToLength(failureReason, 255))
                    .ipAddress(resolveClientIp(request))
                    .userAgent(trimToLength(request != null ? request.getHeader("User-Agent") : null, 500))
                    .loginAt(Instant.now());

            if (userId != null) {
                Optional<User> userOptional = userRepository.findById(userId);
                userOptional.ifPresent(builder::user);
            } else if (StringUtils.hasText(usernameOrEmail)) {
                userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                        .ifPresent(builder::user);
            }

            loginHistoryRepository.save(builder.build());
        } catch (Exception ex) {
            log.warn("Không thể ghi lịch sử đăng nhập cho {}: {}", usernameOrEmail, ex.getMessage(), ex);
        }
    }

    private LoginHistoryResponse mapToResponse(UserLoginHistory history) {
        User user = history.getUser();

        return LoginHistoryResponse.builder()
                .id(history.getId())
                .userId(user != null ? user.getId() : null)
                .username(user != null ? user.getUsername() : history.getUsernameOrEmail())
                .fullName(user != null ? user.getFullName() : null)
                .ipAddress(history.getIpAddress())
                .userAgent(history.getUserAgent())
                .portal(history.getPortal())
                .success(history.getSuccess())
                .failureReason(history.getFailureReason())
                .loginAt(history.getLoginAt())
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String header = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (StringUtils.hasText(header)) {
            String[] parts = header.split(",");
            if (parts.length > 0 && StringUtils.hasText(parts[0])) {
                return parts[0].trim();
            }
        }

        header = request.getHeader(HEADER_X_REAL_IP);
        if (StringUtils.hasText(header)) {
            return header.trim();
        }

        return request.getRemoteAddr();
    }

    private String normalizePortal(String portal) {
        if (!StringUtils.hasText(portal)) {
            return "USER";
        }
        return portal.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToLength(String value, int maxLength) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}


