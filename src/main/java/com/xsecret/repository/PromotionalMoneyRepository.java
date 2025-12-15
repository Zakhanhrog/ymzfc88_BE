package com.xsecret.repository;

import com.xsecret.entity.PromotionalMoney;
import com.xsecret.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionalMoneyRepository extends JpaRepository<PromotionalMoney, Long> {

    /**
     * Tìm theo point transaction
     */
    Optional<PromotionalMoney> findByPointTransactionId(Long pointTransactionId);

    /**
     * Tìm tất cả theo user với phân trang, sắp xếp theo ngày tạo giảm dần
     */
    Page<PromotionalMoney> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Tính tổng tiền khuyến mại của user
     */
    @Query("SELECT COALESCE(SUM(pm.amount), 0) FROM PromotionalMoney pm WHERE pm.user = :user")
    BigDecimal sumAmountByUser(@Param("user") User user);

    /**
     * Tính tổng tiền khuyến mại của user trong khoảng thời gian
     */
    @Query("SELECT COALESCE(SUM(pm.amount), 0) FROM PromotionalMoney pm WHERE pm.user = :user AND pm.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumAmountByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Tính tổng tiền khuyến mại theo date range (tất cả users)
     */
    @Query("SELECT COALESCE(SUM(pm.amount), 0) FROM PromotionalMoney pm WHERE " +
           "(:startDate IS NULL OR pm.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR pm.createdAt <= :endDate)")
    BigDecimal sumAmountByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Đếm số lượng khuyến mại của user
     */
    long countByUser(User user);

    /**
     * Tính tổng tiền khuyến mại theo list users và date range
     */
    @Query("SELECT COALESCE(SUM(pm.amount), 0) FROM PromotionalMoney pm WHERE pm.user IN :users " +
           "AND (:startDate IS NULL OR pm.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR pm.createdAt <= :endDate)")
    BigDecimal sumAmountByUsersAndDateRange(
            @Param("users") List<User> users,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}

