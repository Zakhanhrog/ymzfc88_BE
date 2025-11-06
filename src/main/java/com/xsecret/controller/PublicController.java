package com.xsecret.controller;

import com.xsecret.dto.response.ApiResponse;
import com.xsecret.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Slf4j
public class PublicController {

    private final SystemSettingsService systemSettingsService;

    /**
     * Lấy tất cả contact links cho trang contact public
     */
    @GetMapping("/contact-links")
    public ResponseEntity<ApiResponse<Map<String, String>>> getContactLinks() {
        try {
            log.info("Getting public contact links - Request received");
            Map<String, String> contactLinks = systemSettingsService.getContactLinks();
            log.info("Public contact links retrieved successfully: {}", contactLinks);
            return ResponseEntity.ok(ApiResponse.success(contactLinks));
        } catch (Exception e) {
            log.error("Error getting public contact links", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
