package com.xsecret.repository;

import com.xsecret.entity.DailyLossRefund;
import com.xsecret.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyLossRefundRepository extends JpaRepository<DailyLossRefund, Long> {

    Optional<DailyLossRefund> findByUserAndRefundDate(User user, LocalDate refundDate);

    List<DailyLossRefund> findByUserOrderByRefundDateDesc(User user, Pageable pageable);

    List<DailyLossRefund> findTop500ByStatusAndPayoutAtLessThanEqualOrderByPayoutAtAsc(
            DailyLossRefund.Status status,
            Instant payoutAt
    );

    @Query("SELECT d FROM DailyLossRefund d WHERE d.refundDate = :date AND d.status = :status")
    List<DailyLossRefund> findByRefundDateAndStatus(
            @Param("date") LocalDate date,
            @Param("status") DailyLossRefund.Status status
    );

    @Query("SELECT COUNT(d) FROM DailyLossRefund d WHERE d.user = :user AND d.refundDate = :date")
    long countByUserAndRefundDate(@Param("user") User user, @Param("date") LocalDate date);
    
    // Tính tổng hoàn thua theo ngày đã trả (PAID) theo user
    @Query("SELECT COALESCE(SUM(d.refundAmount), 0) FROM DailyLossRefund d " +
           "WHERE d.user = :user AND d.status = com.xsecret.entity.DailyLossRefund$Status.PAID")
    BigDecimal sumPaidRefundByUser(@Param("user") User user);
    
    // Tính tổng hoàn thua theo ngày đã trả (PAID) theo date range
    // Filter theo paidAt (thời gian thực tế hoàn trả) chứ không phải payoutAt (thời gian dự kiến)
    @Query("SELECT COALESCE(SUM(d.refundAmount), 0) FROM DailyLossRefund d " +
           "WHERE d.status = com.xsecret.entity.DailyLossRefund$Status.PAID " +
           "AND d.paidAt IS NOT NULL " +
           "AND (:startInstant IS NULL OR d.paidAt >= :startInstant) " +
           "AND (:endInstant IS NULL OR d.paidAt <= :endInstant)")
    BigDecimal sumPaidRefundByDateRange(
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant
    );

    // Tính tổng hoàn thua theo ngày đã trả (PAID) theo list users và date range
    @Query("SELECT COALESCE(SUM(d.refundAmount), 0) FROM DailyLossRefund d " +
           "WHERE d.user IN :users " +
           "AND d.status = com.xsecret.entity.DailyLossRefund$Status.PAID " +
           "AND d.paidAt IS NOT NULL " +
           "AND (:startInstant IS NULL OR d.paidAt >= :startInstant) " +
           "AND (:endInstant IS NULL OR d.paidAt <= :endInstant)")
    BigDecimal sumPaidRefundByUsersAndDateRange(
            @Param("users") List<User> users,
            @Param("startInstant") Instant startInstant,
            @Param("endInstant") Instant endInstant
    );
}

