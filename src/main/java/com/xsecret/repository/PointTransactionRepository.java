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
}