package com.xsecret.repository;

import com.xsecret.entity.LotteryResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LotteryResultRepository extends JpaRepository<LotteryResult, Long> {
    
    /**
     * Tìm kết quả theo region, province và drawDate
     * Province có thể null (Miền Bắc)
     */
    Optional<LotteryResult> findByRegionAndProvinceAndDrawDate(String region, String province, LocalDate drawDate);
    
    /**
     * Tìm kết quả đã published theo region, province và drawDate (dùng để check bet)
     */
    @Query("SELECT lr FROM LotteryResult lr WHERE lr.region = :region AND " +
           "((:province IS NULL AND lr.province IS NULL) OR lr.province = :province) AND " +
           "lr.drawDate = :drawDate AND lr.status = 'PUBLISHED'")
    Optional<LotteryResult> findPublishedResult(
        @Param("region") String region, 
        @Param("province") String province, 
        @Param("drawDate") LocalDate drawDate
    );
    
    /**
     * Tìm kết quả mới nhất đã published theo region và province
     */
    @Query("SELECT lr FROM LotteryResult lr WHERE lr.region = :region AND " +
           "((:province IS NULL AND lr.province IS NULL) OR lr.province = :province) AND " +
           "lr.status = 'PUBLISHED' ORDER BY lr.drawDate DESC")
    Optional<LotteryResult> findLatestPublishedResult(
        @Param("region") String region, 
        @Param("province") String province
    );
    
    /**
     * Tìm tất cả kết quả theo region
     */
    Page<LotteryResult> findByRegionOrderByDrawDateDesc(String region, Pageable pageable);
    
    /**
     * Tìm tất cả kết quả theo region và province
     */
    Page<LotteryResult> findByRegionAndProvinceOrderByDrawDateDesc(String region, String province, Pageable pageable);
    
    /**
     * Tìm tất cả kết quả theo province (bỏ qua region)
     */
    Page<LotteryResult> findByProvinceOrderByDrawDateDesc(String province, Pageable pageable);
    
    /**
     * Tìm tất cả kết quả của Miền Bắc
     */
    @Query("SELECT lr FROM LotteryResult lr WHERE lr.region = 'mienBac' AND lr.province IS NULL ORDER BY lr.drawDate DESC")
    Page<LotteryResult> findAllMienBacResults(Pageable pageable);
    
    /**
     * Tìm tất cả kết quả theo status
     */
    Page<LotteryResult> findByStatusOrderByDrawDateDesc(LotteryResult.ResultStatus status, Pageable pageable);
    
    /**
     * Tìm tất cả kết quả trong khoảng thời gian
     */
    @Query("SELECT lr FROM LotteryResult lr WHERE lr.drawDate BETWEEN :startDate AND :endDate ORDER BY lr.drawDate DESC")
    List<LotteryResult> findByDrawDateBetween(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * Đếm số kết quả theo region
     */
    long countByRegion(String region);
    
    /**
     * Đếm số kết quả theo region và province
     */
    long countByRegionAndProvince(String region, String province);
    
    /**
     * Xóa kết quả cũ (trước một ngày nhất định)
     */
    @Query("DELETE FROM LotteryResult lr WHERE lr.drawDate < :beforeDate")
    void deleteResultsBeforeDate(@Param("beforeDate") LocalDate beforeDate);
}

