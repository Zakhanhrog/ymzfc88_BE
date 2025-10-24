package com.xsecret.service;

import com.xsecret.dto.request.MarqueeNotificationRequest;
import com.xsecret.dto.response.MarqueeNotificationResponse;
import com.xsecret.entity.MarqueeNotification;
import com.xsecret.repository.MarqueeNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MarqueeNotificationService {
    
    private final MarqueeNotificationRepository marqueeNotificationRepository;
    
    /**
     * Tạo mới marquee notification
     */
    public MarqueeNotificationResponse createMarqueeNotification(MarqueeNotificationRequest request, String createdBy) {
        log.info("Creating new marquee notification: {}", request.getContent());
        
        MarqueeNotification marqueeNotification = MarqueeNotification.builder()
                .content(request.getContent())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .textColor(request.getTextColor() != null ? request.getTextColor() : "#FF0000")
                .backgroundColor(request.getBackgroundColor() != null ? request.getBackgroundColor() : "#FFFFFF")
                .fontSize(request.getFontSize() != null ? request.getFontSize() : 16)
                .speed(request.getSpeed() != null ? request.getSpeed() : 50)
                .createdBy(createdBy)
                .build();
        
        MarqueeNotification saved = marqueeNotificationRepository.save(marqueeNotification);
        log.info("Marquee notification created successfully with ID: {}", saved.getId());
        
        return MarqueeNotificationResponse.fromEntity(saved);
    }
    
    /**
     * Cập nhật marquee notification
     */
    public MarqueeNotificationResponse updateMarqueeNotification(Long id, MarqueeNotificationRequest request, String updatedBy) {
        log.info("Updating marquee notification with ID: {}", id);
        
        MarqueeNotification existing = marqueeNotificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marquee notification không tồn tại"));
        
        existing.setContent(request.getContent());
        existing.setIsActive(request.getIsActive());
        if (request.getDisplayOrder() != null) {
            existing.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getTextColor() != null) {
            existing.setTextColor(request.getTextColor());
        }
        if (request.getBackgroundColor() != null) {
            existing.setBackgroundColor(request.getBackgroundColor());
        }
        if (request.getFontSize() != null) {
            existing.setFontSize(request.getFontSize());
        }
        if (request.getSpeed() != null) {
            existing.setSpeed(request.getSpeed());
        }
        existing.setUpdatedBy(updatedBy);
        
        MarqueeNotification saved = marqueeNotificationRepository.save(existing);
        log.info("Marquee notification updated successfully with ID: {}", saved.getId());
        
        return MarqueeNotificationResponse.fromEntity(saved);
    }
    
    /**
     * Xóa marquee notification
     */
    public void deleteMarqueeNotification(Long id) {
        log.info("Deleting marquee notification with ID: {}", id);
        
        if (!marqueeNotificationRepository.existsById(id)) {
            throw new RuntimeException("Marquee notification không tồn tại");
        }
        
        marqueeNotificationRepository.deleteById(id);
        log.info("Marquee notification deleted successfully with ID: {}", id);
    }
    
    /**
     * Lấy danh sách marquee notification với phân trang
     */
    @Transactional(readOnly = true)
    public Page<MarqueeNotificationResponse> getAllMarqueeNotifications(int page, int size, String keyword) {
        log.info("Getting marquee notifications - page: {}, size: {}, keyword: {}", page, size, keyword);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending().and(Sort.by("createdAt").ascending()));
        
        Page<MarqueeNotification> marqueeNotifications;
        if (keyword != null && !keyword.trim().isEmpty()) {
            marqueeNotifications = marqueeNotificationRepository.findByContentContainingIgnoreCase(keyword, pageable);
        } else {
            marqueeNotifications = marqueeNotificationRepository.findAllByOrderByDisplayOrderAscCreatedAtAsc(pageable);
        }
        
        return marqueeNotifications.map(MarqueeNotificationResponse::fromEntity);
    }
    
    /**
     * Lấy marquee notification theo ID
     */
    @Transactional(readOnly = true)
    public MarqueeNotificationResponse getMarqueeNotificationById(Long id) {
        log.info("Getting marquee notification by ID: {}", id);
        
        MarqueeNotification marqueeNotification = marqueeNotificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marquee notification không tồn tại"));
        
        return MarqueeNotificationResponse.fromEntity(marqueeNotification);
    }
    
    /**
     * Lấy tất cả marquee notification đang hoạt động (cho public API)
     */
    @Transactional(readOnly = true)
    public List<MarqueeNotificationResponse> getActiveMarqueeNotifications() {
        log.info("Getting active marquee notifications");
        
        List<MarqueeNotification> activeNotifications = marqueeNotificationRepository.findActiveNotifications();
        
        return activeNotifications.stream()
                .map(MarqueeNotificationResponse::fromEntity)
                .toList();
    }
    
    /**
     * Thay đổi trạng thái hoạt động
     */
    public MarqueeNotificationResponse toggleActiveStatus(Long id, String updatedBy) {
        log.info("Toggling active status for marquee notification with ID: {}", id);
        
        MarqueeNotification existing = marqueeNotificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marquee notification không tồn tại"));
        
        existing.setIsActive(!existing.getIsActive());
        existing.setUpdatedBy(updatedBy);
        
        MarqueeNotification saved = marqueeNotificationRepository.save(existing);
        log.info("Marquee notification status toggled successfully with ID: {}", saved.getId());
        
        return MarqueeNotificationResponse.fromEntity(saved);
    }
    
    /**
     * Cập nhật thứ tự hiển thị
     */
    public MarqueeNotificationResponse updateDisplayOrder(Long id, Integer newOrder, String updatedBy) {
        log.info("Updating display order for marquee notification with ID: {} to order: {}", id, newOrder);
        
        MarqueeNotification existing = marqueeNotificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marquee notification không tồn tại"));
        
        existing.setDisplayOrder(newOrder);
        existing.setUpdatedBy(updatedBy);
        
        MarqueeNotification saved = marqueeNotificationRepository.save(existing);
        log.info("Marquee notification display order updated successfully with ID: {}", saved.getId());
        
        return MarqueeNotificationResponse.fromEntity(saved);
    }
}
