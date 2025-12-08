package com.xsecret.mapper;

import com.xsecret.dto.response.UserResponse;
import com.xsecret.entity.User;
import com.xsecret.repository.DailyLossRefundRepository;
import com.xsecret.repository.GameRefundAccrualRepository;
import com.xsecret.repository.UserLoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final UserLoginHistoryRepository userLoginHistoryRepository;
    private final GameRefundAccrualRepository gameRefundAccrualRepository;
    private final DailyLossRefundRepository dailyLossRefundRepository;

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        // Lấy IP lần đầu tiên đăng nhập
        List<String> firstLoginIps = userLoginHistoryRepository.findFirstLoginIpByUser(user, PageRequest.of(0, 1));
        String firstLoginIp = firstLoginIps.isEmpty() ? null : firstLoginIps.get(0);

        // Tính tổng hoàn trả (hoàn trả cược thua)
        BigDecimal totalRefund = safe(gameRefundAccrualRepository.sumPaidLossRefundByUser(user));
        // Tính tổng hoàn thua theo ngày
        BigDecimal totalDailyLossRefund = safe(dailyLossRefundRepository.sumPaidRefundByUser(user));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .staffRole(user.getStaffRole())
                .points(user.getPoints())
                .kycVerified(user.getKycVerified())
                .withdrawalLocked(user.getWithdrawalLocked())
                .withdrawalLockReason(user.getWithdrawalLockReason())
                .withdrawalLockedAt(user.getWithdrawalLockedAt())
                .withdrawalLockedBy(user.getWithdrawalLockedBy())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .referralCode(user.getReferralCode())
                .invitedByCode(user.getInvitedByCode())
                .hasC2Password(user.getC2PasswordHash() != null && !user.getC2PasswordHash().isBlank())
                .c2PasswordUpdatedAt(user.getC2PasswordUpdatedAt())
                .firstLoginIp(firstLoginIp)
                .totalRefund(totalRefund)
                .totalDailyLossRefund(totalDailyLossRefund)
                .build();
    }
}
