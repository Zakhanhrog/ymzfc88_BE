package com.xsecret.controller;

import com.xsecret.dto.request.BannerRequest;
import com.xsecret.dto.response.ApiResponse;
import com.xsecret.dto.response.BannerResponse;
import com.xsecret.entity.Banner;
import com.xsecret.service.BannerService;
import com.xsecret.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BannerController {
    
    private final BannerService bannerService;
    private final FileStorageService fileStorageService;
    
    // Tạo banner mới (Admin only)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BannerResponse>> createBanner(@Valid @RequestBody BannerRequest request) {
        log.info("Creating banner: {}", request.getTitle());
        ApiResponse<BannerResponse> response = bannerService.createBanner(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Cập nhật banner (Admin only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BannerResponse>> updateBanner(
            @PathVariable Long id, 
            @Valid @RequestBody BannerRequest request) {
        log.info("Updating banner with ID: {}", id);
        ApiResponse<BannerResponse> response = bannerService.updateBanner(id, request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Xóa banner (Admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable Long id) {
        log.info("Deleting banner with ID: {}", id);
        ApiResponse<Void> response = bannerService.deleteBanner(id);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Lấy danh sách banner với phân trang (Admin only)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<BannerResponse>>> getAllBanners(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.info("Getting all banners - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);
        ApiResponse<Page<BannerResponse>> response = bannerService.getAllBanners(page, size, sortBy, sortDir);
        
        return ResponseEntity.ok(response);
    }
    
    // Lấy banner theo ID (Admin only)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BannerResponse>> getBannerById(@PathVariable Long id) {
        log.info("Getting banner with ID: {}", id);
        ApiResponse<BannerResponse> response = bannerService.getBannerById(id);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Lấy banner theo loại cho trang chủ (Public)
    @GetMapping("/public/type/{bannerType}")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getBannersByType(@PathVariable Banner.BannerType bannerType) {
        log.info("Getting banners by type: {}", bannerType);
        ApiResponse<List<BannerResponse>> response = bannerService.getBannersByType(bannerType);
        
        return ResponseEntity.ok(response);
    }
    
    // Lấy banner chính cho trang chủ (Public)
    @GetMapping("/public/main")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getMainBanners() {
        log.info("Getting main banners for homepage");
        ApiResponse<List<BannerResponse>> response = bannerService.getBannersByType(Banner.BannerType.MAIN_CAROUSEL);
        
        return ResponseEntity.ok(response);
    }
    
    // Lấy banner phụ cho trang chủ (Public)
    @GetMapping("/public/side")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> getSideBanners() {
        log.info("Getting side banners for homepage");
        ApiResponse<List<BannerResponse>> response = bannerService.getBannersByType(Banner.BannerType.SIDE_BANNER);
        
        return ResponseEntity.ok(response);
    }
    
    // Cập nhật trạng thái banner (Admin only)
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BannerResponse>> updateBannerStatus(
            @PathVariable Long id, 
            @RequestParam Boolean isActive) {
        log.info("Updating banner status with ID: {} to {}", id, isActive);
        ApiResponse<BannerResponse> response = bannerService.updateBannerStatus(id, isActive);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Cập nhật thứ tự hiển thị banner (Admin only)
    @PutMapping("/order")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<BannerResponse>>> updateBannerOrder(@RequestBody List<Long> bannerIds) {
        log.info("Updating banner order for IDs: {}", bannerIds);
        ApiResponse<List<BannerResponse>> response = bannerService.updateBannerOrder(bannerIds);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Upload banner image (Admin only)
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> uploadBannerImage(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Uploading banner image: {}", file.getOriginalFilename());
            
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("File is empty"));
            }
            
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/"))) {
                return ResponseEntity.badRequest().body(ApiResponse.error("File must be an image"));
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
            String filename = "banner_" + UUID.randomUUID().toString() + extension;
            
            // Save file
            String fileUrl = fileStorageService.saveFile(file, "banners", filename);
            
            log.info("Banner image uploaded successfully: {}", fileUrl);
            return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", fileUrl));
            
        } catch (Exception e) {
            log.error("Error uploading banner image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to upload image: " + e.getMessage()));
        }
    }
}
