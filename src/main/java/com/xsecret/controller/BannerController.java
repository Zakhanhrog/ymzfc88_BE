package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.BannerResponse;
import com.xsecret.entity.Banner;
import com.xsecret.service.BannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
@Slf4j
public class BannerController {

    private final BannerService bannerService;

    // Admin Endpoints
    @PostMapping(value = "/admin", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BannerResponse>> createBanner(
            @RequestParam("bannerType") Banner.BannerType bannerType,
            @RequestParam("displayOrder") Integer displayOrder,
            @RequestParam("isActive") Boolean isActive,
            @RequestParam("image") MultipartFile image) {
        BannerResponse response = bannerService.createBanner(bannerType, displayOrder, isActive, image);
        return ResponseEntity.ok(ApiResponse.success("Banner created successfully", response));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<BannerResponse>>> getAllBanners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Page<BannerResponse> banners = bannerService.getAllBanners(page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success("Banners fetched successfully", banners));
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BannerResponse>> getBannerById(@PathVariable Long id) {
        BannerResponse response = bannerService.getBannerById(id);
        return ResponseEntity.ok(ApiResponse.success("Banner fetched successfully", response));
    }

    @PutMapping(value = "/admin/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BannerResponse>> updateBanner(
            @PathVariable Long id,
            @RequestParam("bannerType") Banner.BannerType bannerType,
            @RequestParam("displayOrder") Integer displayOrder,
            @RequestParam("isActive") Boolean isActive,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        BannerResponse response = bannerService.updateBanner(id, bannerType, displayOrder, isActive, image);
        return ResponseEntity.ok(ApiResponse.success("Banner updated successfully", response));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok(ApiResponse.success("Banner deleted successfully", null));
    }

    // Public Endpoints
    @GetMapping("/public/active")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getActiveBanners() {
        List<BannerResponse> banners = bannerService.getActiveBanners();
        return ResponseEntity.ok(ApiResponse.success("Active banners fetched successfully", banners));
    }

    @GetMapping("/public/type/{bannerType}")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getActiveBannersByType(@PathVariable Banner.BannerType bannerType) {
        List<BannerResponse> banners = bannerService.getActiveBannersByType(bannerType);
        return ResponseEntity.ok(ApiResponse.success("Active banners by type fetched successfully", banners));
    }
}
