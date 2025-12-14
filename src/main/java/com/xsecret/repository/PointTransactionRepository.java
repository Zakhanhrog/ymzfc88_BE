package com.xsecret.repository;

import com.xsecret.entity.PointTransaction;
import com.xsecret.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
    
    Page<PointTransaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    Page<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    @Query("SELECT pt FROM PointTransaction pt WHERE pt.user.id = :userId AND pt.createdAt BETWEEN :startDate AND :endDate ORDER BY pt.createdAt DESC")
    Page<PointTransaction> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                                   @Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate, 
                                                   Pageable pageable);
    
    @Query("SELECT pt FROM PointTransaction pt WHERE pt.createdAt BETWEEN :startDate AND :endDate ORDER BY pt.createdAt DESC")
    Page<PointTransaction> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate, 
                                          Pageable pageable);
    
    @Query("SELECT pt FROM PointTransaction pt ORDER BY pt.createdAt DESC")
    Page<PointTransaction> findAllOrderByCreatedAtDesc(Pageable pageable);
    
    List<PointTransaction> findTop10ByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Tính tổng instant game refund (hoàn trả ngay) từ PointTransaction
     * Chỉ tính các transaction với type BET_REFUND và referenceType là SICBO_INSTANT_CASHBACK hoặc XOC_DIA_INSTANT_CASHBACK
     * Chỉ tính các transaction với points > 0 (để tránh tính các transaction âm)
     * Lưu ý: Scheduled refund dùng SICBO_CASHBACK/XOC_DIA_CASHBACK, instant refund dùng SICBO_INSTANT_CASHBACK/XOC_DIA_INSTANT_CASHBACK
     */
    @Query("SELECT COALESCE(SUM(pt.points), 0) FROM PointTransaction pt " +
           "WHERE pt.type = com.xsecret.entity.PointTransaction$PointTransactionType.BET_REFUND " +
           "AND pt.referenceType IN ('SICBO_INSTANT_CASHBACK', 'XOC_DIA_INSTANT_CASHBACK') " +
           "AND pt.points > 0 " +
           "AND (:startDate IS NULL OR pt.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR pt.createdAt <= :endDate)")
    java.math.BigDecimal sumInstantGameRefundByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}