package com.xsecret.repository;

import com.xsecret.entity.BettingOdds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BettingOddsRepository extends JpaRepository<BettingOdds, Long> {
    
    /**
     * Tìm tất cả tỷ lệ cược theo khu vực
     */
    List<BettingOdds> findByRegion(String region);
    
    /**
     * Tìm tất cả tỷ lệ cược đang active theo khu vực
     */
    List<BettingOdds> findByRegionAndIsActive(String region, Boolean isActive);
    
    /**
     * Tìm tỷ lệ cược theo region và betType
     */
    Optional<BettingOdds> findByRegionAndBetType(String region, String betType);
    
    /**
     * Kiểm tra tồn tại theo region và betType
     */
    boolean existsByRegionAndBetType(String region, String betType);
}

