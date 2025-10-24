package com.xsecret.repository;

import com.xsecret.entity.MarqueeNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarqueeNotificationRepository extends JpaRepository<MarqueeNotification, Long> {
    
    /**
     * Lấy tất cả marquee notification đang hoạt động, sắp xếp theo displayOrder
     */
    @Query("SELECT m FROM MarqueeNotification m WHERE m.isActive = true ORDER BY m.displayOrder ASC, m.createdAt ASC")
    List<MarqueeNotification> findActiveNotifications();
    
    /**
     * Lấy marquee notification với phân trang
     */
    Page<MarqueeNotification> findAllByOrderByDisplayOrderAscCreatedAtAsc(Pageable pageable);
    
    /**
     * Tìm kiếm marquee notification theo nội dung
     */
    @Query("SELECT m FROM MarqueeNotification m WHERE m.content LIKE %:keyword% ORDER BY m.displayOrder ASC, m.createdAt ASC")
    Page<MarqueeNotification> findByContentContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * Đếm số lượng marquee notification đang hoạt động
     */
    @Query("SELECT COUNT(m) FROM MarqueeNotification m WHERE m.isActive = true")
    Long countActiveNotifications();
    
    /**
     * Lấy marquee notification theo trạng thái
     */
    List<MarqueeNotification> findByIsActiveOrderByDisplayOrderAscCreatedAtAsc(Boolean isActive);
}
