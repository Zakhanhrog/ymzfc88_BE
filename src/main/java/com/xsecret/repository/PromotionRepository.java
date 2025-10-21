package com.xsecret.repository;

import com.xsecret.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    /**
     * Tìm tất cả promotions đang hoạt động, sắp xếp theo displayOrder
     */
    List<Promotion> findByIsActiveTrueOrderByDisplayOrderAscCreatedAtDesc();
    
    /**
     * Tìm tất cả promotions đang hoạt động với phân trang
     */
    Page<Promotion> findByIsActiveTrueOrderByDisplayOrderAscCreatedAtDesc(Pageable pageable);
    
    /**
     * Tìm tất cả promotions (cho admin), sắp xếp theo displayOrder
     */
    List<Promotion> findAllByOrderByDisplayOrderAscCreatedAtDesc();
    
    /**
     * Tìm tất cả promotions (cho admin) với phân trang
     */
    Page<Promotion> findAllByOrderByDisplayOrderAscCreatedAtDesc(Pageable pageable);
    
    /**
     * Đếm số lượng promotions đang hoạt động
     */
    long countByIsActiveTrue();
    
    /**
     * Tìm promotion theo title (cho tìm kiếm)
     */
    @Query("SELECT p FROM Promotion p WHERE p.title LIKE %:title% AND p.isActive = true ORDER BY p.displayOrder ASC, p.createdAt DESC")
    List<Promotion> findByTitleContainingIgnoreCaseAndIsActiveTrue(@Param("title") String title);
}
