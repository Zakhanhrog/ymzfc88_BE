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

        long currentPoints = user.getPoints() != null ? user.getPoints() : 0L;
        long pointsToAdjust = request.getPoints();
        long newPoints;
        
        if ("ADD".equals(request.getType())) {
            newPoints = currentPoints + pointsToAdjust;
        } else {
            if (currentPoints < pointsToAdjust) {
                throw new RuntimeException("Insufficient points. Available: " + currentPoints + ", Required: " + pointsToAdjust);
            }
            newPoints = currentPoints - pointsToAdjust;
        }
        
        // Cập nhật điểm trực tiếp vào user
        user.setPoints(newPoints);
        userRepository.save(user);
        
        // Tạo PointTransaction để lưu lịch sử
        PointTransaction.PointTransactionType type = "ADD".equals(request.getType()) ? 
            PointTransaction.PointTransactionType.ADMIN_ADD : 
            PointTransaction.PointTransactionType.ADMIN_SUBTRACT;
            
        PointTransaction transaction = PointTransaction.builder()
                .user(user)
                .transactionCode(generateTransactionCode())
                .type(type)
                .points(BigDecimal.valueOf("ADD".equals(request.getType()) ? pointsToAdjust : -pointsToAdjust))
                .balanceBefore(BigDecimal.valueOf(currentPoints))
                .balanceAfter(BigDecimal.valueOf(newPoints))
                .description(request.getDescription())
                .referenceType("ADMIN_ADJUSTMENT")
                .referenceId(null)
                .createdBy(adminUser)
                .build();
        
        pointTransactionRepository.save(transaction);
        
        log.info("Admin {} adjusted {} points for user {}. Points: {} -> {}", 
                adminUser.getUsername(), 
                "ADD".equals(request.getType()) ? "+" + pointsToAdjust : "-" + pointsToAdjust,
                user.getUsername(), currentPoints, newPoints);

        return mapToResponse(transaction);
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

        // Sử dụng user.points làm chuẩn để tránh không đồng bộ
        BigDecimal balanceBefore = BigDecimal.valueOf(user.getPoints() != null ? user.getPoints() : 0L);
        BigDecimal balanceAfter = balanceBefore.add(points);

        // Update user points first (chuẩn)
        user.setPoints(balanceAfter.longValue());
        userRepository.save(user);

        // Sync userPoint với user.points
        userPoint.setTotalPoints(balanceAfter);
        userPoint.setLifetimeEarned(userPoint.getLifetimeEarned().add(points));
        userPointRepository.save(userPoint);

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
                .orElseGet(() -> userPointRepository.save(UserPoint.builder()
                        .user(user)
                        .totalPoints(BigDecimal.ZERO)
                        .lifetimeEarned(BigDecimal.ZERO)
                        .lifetimeSpent(BigDecimal.ZERO)
                        .build()));

        BigDecimal balanceBefore = BigDecimal.valueOf(user.getPoints() != null ? user.getPoints() : 0L);

        if (balanceBefore.compareTo(points) < 0) {
            throw new RuntimeException("Insufficient points");
        }

        BigDecimal balanceAfter = balanceBefore.subtract(points);

        user.setPoints(balanceAfter.longValue());
        userRepository.save(user);

        userPoint.setTotalPoints(balanceAfter);
        userPoint.setLifetimeSpent(userPoint.getLifetimeSpent().add(points));
        userPointRepository.save(userPoint);

        PointTransaction transaction = PointTransaction.builder()
                .user(user)
                .transactionCode(generateTransactionCode())
                .type(type)
                .points(points.negate())
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

        // Lấy điểm trực tiếp từ user.points (đã đồng bộ)
        long userPoints = user.getPoints() != null ? user.getPoints() : 0L;

        return UserPointResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .totalPoints(BigDecimal.valueOf(userPoints))
                .lifetimeEarned(BigDecimal.ZERO) // Có thể tính từ transaction history sau
                .lifetimeSpent(BigDecimal.ZERO)  // Có thể tính từ transaction history sau
                .lastUpdated(LocalDateTime.now())
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
        // Tạo mã ngắn hơn: PT + timestamp (8 ký tự cuối) + random (4 ký tự)
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5); // 8 ký tự cuối
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "PT" + timestamp + random; // Tổng: 2 + 8 + 4 = 14 ký tự
    }
}