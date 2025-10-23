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
import java.util.Optional;

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
     * CHỈ LẤY BET CỦA HÔM NAY (resultDate = currentDate), không check các ngày trước
     * JOIN FETCH user để tránh LazyInitializationException
     */
    @Query("SELECT b FROM Bet b JOIN FETCH b.user WHERE b.status = 'PENDING' AND b.resultDate = :currentDate ORDER BY b.createdAt ASC")
    List<Bet> findPendingBetsToCheck(@Param("currentDate") String currentDate);
    
    /**
     * Tìm bet chưa được kiểm tra kết quả cho ngày cụ thể
     * Dùng khi admin publish kết quả cho ngày cụ thể
     * JOIN FETCH user để tránh LazyInitializationException
     */
    @Query("SELECT b FROM Bet b JOIN FETCH b.user WHERE b.status = 'PENDING' AND b.resultDate = :targetDate ORDER BY b.createdAt ASC")
    List<Bet> findPendingBetsToCheckForDate(@Param("targetDate") String targetDate);
    
    /**
     * Tìm bet theo ID và eager fetch user
     * Dùng trong checkBetResult() để tránh LazyInitializationException khi access user.points
     */
    @Query("SELECT b FROM Bet b JOIN FETCH b.user WHERE b.id = :id")
    Optional<Bet> findByIdWithUser(@Param("id") Long id);
    
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
    
    // ======================== ADMIN QUERIES ========================
    
    /**
     * Lấy tất cả bet với filter (for admin)
     */
    @Query("SELECT b FROM Bet b WHERE " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:betType IS NULL OR b.betType = :betType) AND " +
           "(:region IS NULL OR b.region = :region) AND " +
           "(:userId IS NULL OR b.user.id = :userId) " +
           "ORDER BY b.createdAt DESC")
    Page<Bet> findAllBetsWithFilters(
        @Param("status") Bet.BetStatus status,
        @Param("betType") String betType,
        @Param("region") String region,
        @Param("userId") Long userId,
        Pageable pageable);
    
    /**
     * Tìm kiếm bet theo username hoặc betId
     */
    @Query("SELECT b FROM Bet b WHERE " +
           "LOWER(b.user.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "CAST(b.id AS string) LIKE CONCAT('%', :searchTerm, '%') " +
           "ORDER BY b.createdAt DESC")
    Page<Bet> searchBets(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /**
     * Đếm tổng số bet trong hệ thống
     */
    @Query("SELECT COUNT(b) FROM Bet b")
    long countAllBets();
    
    /**
     * Đếm bet theo status
     */
    long countByStatus(Bet.BetStatus status);
    
    /**
     * Tính tổng tiền cược trong hệ thống
     */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bet b")
    double getTotalBetAmount();
    
    /**
     * Tính tổng tiền thắng trong hệ thống
     */
    @Query("SELECT COALESCE(SUM(b.winAmount), 0) FROM Bet b WHERE b.isWin = true")
    double getTotalWinAmount();
}
