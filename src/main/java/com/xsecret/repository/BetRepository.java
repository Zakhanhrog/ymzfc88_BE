package com.xsecret.repository;

import com.xsecret.entity.Bet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BetRepository extends JpaRepository<Bet, Long> {
    
    /**
     * Tìm tất cả bet của user
     */
    Page<Bet> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * Tìm bet theo user và status
     */
    Page<Bet> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Bet.BetStatus status, Pageable pageable);
    
    /**
     * Tìm bet theo region
     */
    Page<Bet> findByRegionOrderByCreatedAtDesc(String region, Pageable pageable);
    
    /**
     * Tìm bet theo bet type
     */
    Page<Bet> findByBetTypeOrderByCreatedAtDesc(String betType, Pageable pageable);
    
    /**
     * Tìm bet đang chờ kết quả
     */
    List<Bet> findByStatusAndResultDate(Bet.BetStatus status, String resultDate);
    
    /**
     * Tìm bet chưa được kiểm tra kết quả
     */
    @Query("SELECT b FROM Bet b WHERE b.status = 'PENDING' AND b.resultDate <= :currentDate AND b.resultCheckedAt IS NULL")
    List<Bet> findPendingBetsToCheck(@Param("currentDate") String currentDate);
    
    /**
     * Đếm tổng số bet của user
     */
    long countByUserId(Long userId);
    
    /**
     * Đếm số bet thắng của user
     */
    long countByUserIdAndIsWinTrue(Long userId);
    
    /**
     * Tính tổng tiền cược của user
     */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bet b WHERE b.user.id = :userId")
    double getTotalBetAmountByUserId(@Param("userId") Long userId);
    
    /**
     * Tính tổng tiền thắng của user
     */
    @Query("SELECT COALESCE(SUM(b.winAmount), 0) FROM Bet b WHERE b.user.id = :userId AND b.isWin = true")
    double getTotalWinAmountByUserId(@Param("userId") Long userId);
    
    /**
     * Tìm bet theo khoảng thời gian
     */
    @Query("SELECT b FROM Bet b WHERE b.user.id = :userId AND b.createdAt BETWEEN :startDate AND :endDate ORDER BY b.createdAt DESC")
    Page<Bet> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                      @Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate, 
                                      Pageable pageable);
    
    /**
     * Tìm bet gần đây của user
     */
    @Query("SELECT b FROM Bet b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    List<Bet> findRecentBetsByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Thống kê bet theo ngày
     */
    @Query("SELECT DATE(b.createdAt) as betDate, COUNT(b) as betCount, SUM(b.totalAmount) as totalAmount " +
           "FROM Bet b WHERE b.user.id = :userId AND b.createdAt >= :startDate " +
           "GROUP BY DATE(b.createdAt) ORDER BY betDate DESC")
    List<Object[]> getBetStatisticsByDate(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);
}
