package com.xsecret.service;

import com.xsecret.dto.request.PromotionRequest;
import com.xsecret.dto.response.PromotionResponse;
import com.xsecret.entity.Promotion;
import com.xsecret.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {
    
    private final PromotionRepository promotionRepository;
    
    /**
     * Lấy tất cả promotions đang hoạt động (public)
     */
    public List<PromotionResponse> getActivePromotions() {
        log.info("Getting active promotions");
        List<Promotion> promotions = promotionRepository.findByIsActiveTrueOrderByDisplayOrderAscCreatedAtDesc();
        return promotions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy tất cả promotions đang hoạt động với phân trang (public)
     */
    public Page<PromotionResponse> getActivePromotions(Pageable pageable) {
        log.info("Getting active promotions with pagination");
        Page<Promotion> promotions = promotionRepository.findByIsActiveTrueOrderByDisplayOrderAscCreatedAtDesc(pageable);
        return promotions.map(this::convertToResponse);
    }
    
    /**
     * Lấy tất cả promotions (admin)
     */
    public List<PromotionResponse> getAllPromotions() {
        log.info("Getting all promotions for admin");
        List<Promotion> promotions = promotionRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc();
        return promotions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy tất cả promotions với phân trang (admin)
     */
    public Page<PromotionResponse> getAllPromotions(Pageable pageable) {
        log.info("Getting all promotions with pagination for admin");
        Page<Promotion> promotions = promotionRepository.findAllByOrderByDisplayOrderAscCreatedAtDesc(pageable);
        return promotions.map(this::convertToResponse);
    }
    
    /**
     * Lấy promotion theo ID
     */
    public PromotionResponse getPromotionById(Long id) {
        log.info("Getting promotion by ID: {}", id);
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found with id: " + id));
        return convertToResponse(promotion);
    }
    
    /**
     * Tạo promotion mới
     */
    @Transactional
    public PromotionResponse createPromotion(PromotionRequest request) {
        log.info("Creating new promotion: {}", request.getTitle());
        
        Promotion promotion = Promotion.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();
        
        Promotion savedPromotion = promotionRepository.save(promotion);
        log.info("Successfully created promotion with ID: {}", savedPromotion.getId());
        
        return convertToResponse(savedPromotion);
    }
    
    /**
     * Cập nhật promotion
     */
    @Transactional
    public PromotionResponse updatePromotion(Long id, PromotionRequest request) {
        log.info("Updating promotion with ID: {}", id);
        
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found with id: " + id));
        
        promotion.setTitle(request.getTitle());
        promotion.setDescription(request.getDescription());
        promotion.setImageUrl(request.getImageUrl());
        if (request.getIsActive() != null) {
            promotion.setIsActive(request.getIsActive());
        }
        if (request.getDisplayOrder() != null) {
            promotion.setDisplayOrder(request.getDisplayOrder());
        }
        
        Promotion updatedPromotion = promotionRepository.save(promotion);
        log.info("Successfully updated promotion with ID: {}", updatedPromotion.getId());
        
        return convertToResponse(updatedPromotion);
    }
    
    /**
     * Xóa promotion
     */
    @Transactional
    public void deletePromotion(Long id) {
        log.info("Deleting promotion with ID: {}", id);
        
        if (!promotionRepository.existsById(id)) {
            throw new RuntimeException("Promotion not found with id: " + id);
        }
        
        promotionRepository.deleteById(id);
        log.info("Successfully deleted promotion with ID: {}", id);
    }
    
    /**
     * Toggle trạng thái active của promotion
     */
    @Transactional
    public PromotionResponse togglePromotionStatus(Long id) {
        log.info("Toggling promotion status for ID: {}", id);
        
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found with id: " + id));
        
        promotion.setIsActive(!promotion.getIsActive());
        Promotion updatedPromotion = promotionRepository.save(promotion);
        
        log.info("Successfully toggled promotion status for ID: {} to {}", id, updatedPromotion.getIsActive());
        return convertToResponse(updatedPromotion);
    }
    
    /**
     * Chuyển đổi Promotion entity thành PromotionResponse
     */
    private PromotionResponse convertToResponse(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getId())
                .title(promotion.getTitle())
                .description(promotion.getDescription())
                .imageUrl(promotion.getImageUrl())
                .isActive(promotion.getIsActive())
                .displayOrder(promotion.getDisplayOrder())
                .createdAt(promotion.getCreatedAt())
                .updatedAt(promotion.getUpdatedAt())
                .build();
    }
}
