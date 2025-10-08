package com.xsecret.service;

import com.xsecret.dto.request.PointAdjustmentRequest;
import com.xsecret.dto.response.PointTransactionResponse;
import com.xsecret.dto.response.UserPointResponse;
import com.xsecret.entity.*;
import com.xsecret.repository.PointTransactionRepository;
import com.xsecret.repository.UserPointRepository;
import com.xsecret.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final UserRepository userRepository;

    private static final int POINTS_PER_1000_VND = 1;

    @Transactional
    public void initializeUserPoints(User user) {
        if (userPointRepository.findByUser(user).isEmpty()) {
            UserPoint userPoint = UserPoint.builder()
                    .user(user)
                    .totalPoints(BigDecimal.ZERO)
                    .lifetimeEarned(BigDecimal.ZERO)
                    .lifetimeSpent(BigDecimal.ZERO)
                    .build();
            userPointRepository.save(userPoint);
            log.info("Initialized points for user: {}", user.getUsername());
        }
    }

    @Transactional
    public void addPointsFromDeposit(User user, BigDecimal depositAmount, Long transactionId) {
        // 1000 VND = 1 point
        BigDecimal pointsToAdd = depositAmount.divide(BigDecimal.valueOf(1000), 0, java.math.RoundingMode.DOWN);
        
        if (pointsToAdd.compareTo(BigDecimal.ZERO) > 0) {
            addPoints(user, pointsToAdd, PointTransaction.PointTransactionType.DEPOSIT_BONUS, 
                     "Điểm từ nạp tiền: " + depositAmount + " VND", 
                     "DEPOSIT", transactionId, null);
        }
    }

    @Transactional
    public void deductPointsFromWithdraw(User user, Integer pointsToDeduct, Long transactionId) {
        BigDecimal pointsAmount = BigDecimal.valueOf(pointsToDeduct);
        
        // Validate user has enough points
        UserPoint userPoint = userPointRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("User points not found"));
        
        if (userPoint.getTotalPoints().compareTo(pointsAmount) < 0) {
            throw new RuntimeException("Insufficient points. Available: " + userPoint.getTotalPoints() + ", Required: " + pointsAmount);
        }
        
        subtractPoints(user, pointsAmount, PointTransaction.PointTransactionType.WITHDRAW_DEDUCTION, 
                      "Trừ điểm khi rút tiền: " + (pointsToDeduct * 1000) + " VND", 
                      "WITHDRAW", transactionId, null);
    }

    @Transactional
    public void refundPointsFromRejectedWithdraw(User user, Integer pointsToRefund, Long transactionId) {
        BigDecimal pointsAmount = BigDecimal.valueOf(pointsToRefund);
        
        addPoints(user, pointsAmount, PointTransaction.PointTransactionType.REFUND, 
                 "Hoàn điểm do lệnh rút tiền bị từ chối: " + (pointsToRefund * 1000) + " VND", 
                 "WITHDRAW_REJECTED", transactionId, null);
        
        log.info("Refunded {} points to user {} for rejected withdraw transaction", 
                pointsToRefund, user.getUsername());
    }

    public Integer getUserPointsBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserPoint userPoint = userPointRepository.findByUser(user)
                .orElseGet(() -> {
                    // Create and save if doesn't exist
                    UserPoint newUserPoint = UserPoint.builder()
                            .user(user)
                            .totalPoints(BigDecimal.ZERO)
                            .lifetimeEarned(BigDecimal.ZERO)
                            .lifetimeSpent(BigDecimal.ZERO)
                            .build();
                    return userPointRepository.save(newUserPoint);
                });

        return userPoint.getTotalPoints().intValue();
    }

    @Transactional
    public PointTransactionResponse adjustPointsByAdmin(PointAdjustmentRequest request, User adminUser) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal pointsAmount = BigDecimal.valueOf(request.getPoints());
        PointTransaction.PointTransactionType type;
        
        if ("ADD".equals(request.getType())) {
            type = PointTransaction.PointTransactionType.ADMIN_ADD;
            addPoints(user, pointsAmount, type, request.getDescription(), 
                     "ADMIN_ADJUSTMENT", null, adminUser);
        } else if ("SUBTRACT".equals(request.getType())) {
            type = PointTransaction.PointTransactionType.ADMIN_SUBTRACT;
            subtractPoints(user, pointsAmount, type, request.getDescription(), 
                          "ADMIN_ADJUSTMENT", null, adminUser);
        } else {
            throw new RuntimeException("Invalid adjustment type");
        }

        // Get the latest transaction for this user
        Page<PointTransaction> transactions = pointTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(0, 1));
        
        if (!transactions.isEmpty()) {
            return mapToResponse(transactions.getContent().get(0));
        }
        
        throw new RuntimeException("Failed to create point transaction");
    }

    @Transactional
    protected void addPoints(User user, BigDecimal points, PointTransaction.PointTransactionType type, 
                           String description, String referenceType, Long referenceId, User createdBy) {
        UserPoint userPoint = userPointRepository.findByUser(user)
                .orElseGet(() -> {
                    UserPoint newUserPoint = UserPoint.builder()
                            .user(user)
                            .totalPoints(BigDecimal.ZERO)
                            .lifetimeEarned(BigDecimal.ZERO)
                            .lifetimeSpent(BigDecimal.ZERO)
                            .build();
                    return userPointRepository.save(newUserPoint);
                });

        BigDecimal balanceBefore = userPoint.getTotalPoints();
        BigDecimal balanceAfter = balanceBefore.add(points);

        // Update user points
        userPoint.setTotalPoints(balanceAfter);
        userPoint.setLifetimeEarned(userPoint.getLifetimeEarned().add(points));
        userPointRepository.save(userPoint);

        // Update user entity
        user.setPoints(balanceAfter.longValue());
        userRepository.save(user);

        // Create transaction record
        PointTransaction transaction = PointTransaction.builder()
                .user(user)
                .transactionCode(generateTransactionCode())
                .type(type)
                .points(points)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .createdBy(createdBy)
                .build();

        pointTransactionRepository.save(transaction);
        log.info("Added {} points to user: {}, new balance: {}", points, user.getUsername(), balanceAfter);
    }

    @Transactional
    protected void subtractPoints(User user, BigDecimal points, PointTransaction.PointTransactionType type, 
                                String description, String referenceType, Long referenceId, User createdBy) {
        UserPoint userPoint = userPointRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("User points not found"));

        BigDecimal balanceBefore = userPoint.getTotalPoints();
        
        if (balanceBefore.compareTo(points) < 0) {
            throw new RuntimeException("Insufficient points");
        }

        BigDecimal balanceAfter = balanceBefore.subtract(points);

        // Update user points
        userPoint.setTotalPoints(balanceAfter);
        userPoint.setLifetimeSpent(userPoint.getLifetimeSpent().add(points));
        userPointRepository.save(userPoint);

        // Update user entity
        user.setPoints(balanceAfter.longValue());
        userRepository.save(user);

        // Create transaction record
        PointTransaction transaction = PointTransaction.builder()
                .user(user)
                .transactionCode(generateTransactionCode())
                .type(type)
                .points(points.negate()) // Negative for subtract
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .createdBy(createdBy)
                .build();

        pointTransactionRepository.save(transaction);
        log.info("Subtracted {} points from user: {}, new balance: {}", points, user.getUsername(), balanceAfter);
    }

    public UserPointResponse getUserPoints(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserPoint userPoint = userPointRepository.findByUser(user)
                .orElseGet(() -> {
                    // Create and save if doesn't exist
                    UserPoint newUserPoint = UserPoint.builder()
                            .user(user)
                            .totalPoints(BigDecimal.ZERO)
                            .lifetimeEarned(BigDecimal.ZERO)
                            .lifetimeSpent(BigDecimal.ZERO)
                            .build();
                    return userPointRepository.save(newUserPoint);
                });

        return UserPointResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .totalPoints(userPoint.getTotalPoints())
                .lifetimeEarned(userPoint.getLifetimeEarned())
                .lifetimeSpent(userPoint.getLifetimeSpent())
                .lastUpdated(userPoint.getUpdatedAt())
                .build();
    }

    public Page<PointTransactionResponse> getUserPointHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PointTransaction> transactions = pointTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        return transactions.map(this::mapToResponse);
    }

    public Page<PointTransactionResponse> getAllPointHistory(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PointTransaction> transactions = pointTransactionRepository
                .findAllOrderByCreatedAtDesc(pageable);
        
        return transactions.map(this::mapToResponse);
    }

    public Page<PointTransactionResponse> getPointHistoryByDateRange(LocalDateTime startDate, 
                                                                   LocalDateTime endDate, 
                                                                   Long userId, 
                                                                   int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PointTransaction> transactions;
        
        if (userId != null) {
            transactions = pointTransactionRepository
                    .findByUserIdAndDateRange(userId, startDate, endDate, pageable);
        } else {
            transactions = pointTransactionRepository
                    .findByDateRange(startDate, endDate, pageable);
        }
        
        return transactions.map(this::mapToResponse);
    }

    private PointTransactionResponse mapToResponse(PointTransaction transaction) {
        return PointTransactionResponse.builder()
                .id(transaction.getId())
                .transactionCode(transaction.getTransactionCode())
                .type(transaction.getType().name())
                .typeDisplayName(transaction.getType().getDisplayName())
                .points(transaction.getPoints())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .description(transaction.getDescription())
                .referenceType(transaction.getReferenceType())
                .referenceId(transaction.getReferenceId())
                .createdByUsername(transaction.getCreatedBy() != null ? 
                                 transaction.getCreatedBy().getUsername() : null)
                .createdAt(transaction.getCreatedAt())
                .user(PointTransactionResponse.UserInfo.builder()
                        .id(transaction.getUser().getId())
                        .username(transaction.getUser().getUsername())
                        .fullName(transaction.getUser().getFullName())
                        .build())
                .build();
    }

    private String generateTransactionCode() {
        return "PT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}