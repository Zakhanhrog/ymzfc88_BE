package com.xsecret.controller;

import com.xsecret.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileController {
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @GetMapping("/bills/{filename}")
    public ResponseEntity<byte[]> getBillImage(@PathVariable String filename) {
        try {
            // Đường dẫn đến file ảnh
            Path filePath = Paths.get("uploads/bills/" + filename);
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            // Đọc file thành byte array
            byte[] imageBytes = Files.readAllBytes(filePath);
            
            // Xác định content type dựa trên extension
            String contentType = getContentType(filename);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache 1 hour
                    .body(imageBytes);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/kyc/{filename}")
    public ResponseEntity<byte[]> getKycImage(@PathVariable String filename) {
        try {
            // Đường dẫn đến file ảnh KYC
            Path filePath = Paths.get("uploads/kyc/" + filename);
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            // Đọc file thành byte array
            byte[] imageBytes = Files.readAllBytes(filePath);
            
            // Xác định content type dựa trên extension
            String contentType = getContentType(filename);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache 1 hour
                    .body(imageBytes);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/promotions/{filename}")
    public ResponseEntity<byte[]> getPromotionImage(@PathVariable String filename) {
        try {
            // Đường dẫn đến file ảnh promotion
            Path filePath = Paths.get("uploads/promotions/" + filename);
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            // Đọc file thành byte array
            byte[] imageBytes = Files.readAllBytes(filePath);
            
            // Xác định content type dựa trên extension
            String contentType = getContentType(filename);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache 1 hour
                    .body(imageBytes);
                    
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Upload ảnh promotion (base64)
     */
    @PostMapping("/promotions/upload")
    public ResponseEntity<Map<String, String>> uploadPromotionImage(@RequestBody Map<String, String> request) {
        try {
            String base64Data = request.get("image");
            String fileName = request.get("fileName");
            
            if (base64Data == null || fileName == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Missing image or fileName");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Validate base64 image
            if (!fileStorageService.isValidBase64Image(base64Data)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid image format");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Save image
            String imageUrl = fileStorageService.savePromotionImage(base64Data, fileName);
            
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("message", "Image uploaded successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload image: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG_VALUE;
            case "png":
                return MediaType.IMAGE_PNG_VALUE;
            case "gif":
                return MediaType.IMAGE_GIF_VALUE;
            default:
                return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}