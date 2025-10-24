package com.xsecret.service;

import com.xsecret.dto.response.BannerResponse;
import com.xsecret.entity.Banner;
import com.xsecret.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BannerService {

    private final BannerRepository bannerRepository;
    private final FileStorageService fileStorageService;

    public BannerResponse createBanner(Banner.BannerType bannerType, Integer displayOrder, Boolean isActive, MultipartFile image) {
        try {
            log.info("Creating banner with type: {}", bannerType);
            
            // Upload file and get URL
            String imageUrl = fileStorageService.storeFile(image, "banners");
            
            Banner banner = Banner.builder()
                    .imageUrl(imageUrl)
                    .bannerType(bannerType)
                    .displayOrder(displayOrder)
                    .isActive(isActive)
                    .build();
            
            Banner savedBanner = bannerRepository.save(banner);
            log.info("Banner created successfully with id: {}", savedBanner.getId());
            return toResponse(savedBanner);
        } catch (Exception e) {
            log.error("Error creating banner: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create banner: " + e.getMessage());
        }
    }

    public Page<BannerResponse> getAllBanners(int page, int size, String sortBy, String sortDir) {
        try {
            log.info("Getting all banners - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);
            
            Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Banner> bannerPage = bannerRepository.findAll(pageable);
            
            log.info("Found {} banners", bannerPage.getTotalElements());
            return bannerPage.map(this::toResponse);
        } catch (Exception e) {
            log.error("Error getting all banners: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get banners: " + e.getMessage());
        }
    }

    public BannerResponse getBannerById(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found with id: " + id));
        return toResponse(banner);
    }

    public BannerResponse updateBanner(Long id, Banner.BannerType bannerType, Integer displayOrder, Boolean isActive, MultipartFile image) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Banner not found with id: " + id));

        // Update image URL only if new image is provided
        if (image != null && !image.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(image, "banners");
            banner.setImageUrl(imageUrl);
        }
        
        banner.setBannerType(bannerType);
        banner.setDisplayOrder(displayOrder);
        banner.setIsActive(isActive);

        Banner updatedBanner = bannerRepository.save(banner);
        return toResponse(updatedBanner);
    }

    public void deleteBanner(Long id) {
        bannerRepository.deleteById(id);
    }

    public List<BannerResponse> getActiveBannersByType(Banner.BannerType bannerType) {
        return bannerRepository.findByIsActiveTrueAndBannerTypeOrderByDisplayOrderAsc(bannerType)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private BannerResponse toResponse(Banner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .imageUrl(banner.getImageUrl())
                .bannerType(banner.getBannerType())
                .displayOrder(banner.getDisplayOrder())
                .isActive(banner.getIsActive())
                .createdAt(banner.getCreatedAt() != null ? banner.getCreatedAt().toString() : null)
                .updatedAt(banner.getUpdatedAt() != null ? banner.getUpdatedAt().toString() : null)
                .build();
    }
}
