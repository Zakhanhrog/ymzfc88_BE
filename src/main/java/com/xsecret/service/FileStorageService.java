package com.xsecret.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {
    
    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;
    
    /**
     * Lưu ảnh base64 thành file và trả về URL
     */
    public String saveBase64Image(String base64Data, String originalFileName) {
        try {
            // Tạo thư mục nếu chưa có
            Path uploadPath = Paths.get(uploadDir, "bills");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Decode base64
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            
            // Tạo tên file duy nhất
            String fileName = generateUniqueFileName(originalFileName);
            Path filePath = uploadPath.resolve(fileName);
            
            // Lưu file
            Files.write(filePath, imageBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            
            // Trả về URL tương đối
            String imageUrl = "/uploads/bills/" + fileName;
            log.info("Saved bill image: {}", imageUrl);
            
            return imageUrl;
            
        } catch (IOException e) {
            log.error("Error saving bill image", e);
            throw new RuntimeException("Failed to save bill image: " + e.getMessage());
        }
    }
    
    /**
     * Xóa file ảnh
     */
    public boolean deleteImage(String imageUrl) {
        try {
            if (imageUrl == null || !imageUrl.startsWith("/uploads/bills/")) {
                return false;
            }
            
            // Lấy tên file từ URL
            String fileName = imageUrl.substring("/uploads/bills/".length());
            Path filePath = Paths.get(uploadDir, "bills", fileName);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted bill image: {}", imageUrl);
                return true;
            }
            
            return false;
            
        } catch (IOException e) {
            log.error("Error deleting bill image: {}", imageUrl, e);
            return false;
        }
    }
    
    /**
     * Kiểm tra file có tồn tại không
     */
    public boolean imageExists(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/bills/")) {
            return false;
        }
        
        String fileName = imageUrl.substring("/uploads/bills/".length());
        Path filePath = Paths.get(uploadDir, "bills", fileName);
        
        return Files.exists(filePath);
    }
    
    /**
     * Tạo tên file duy nhất
     */
    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        } else {
            extension = ".jpg"; // Default extension
        }
        
        return UUID.randomUUID().toString() + "_" + System.currentTimeMillis() + extension;
    }
    
    /**
     * Validate base64 image
     */
    public boolean isValidBase64Image(String base64Data) {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            return false;
        }
        
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            return imageBytes.length > 0 && imageBytes.length <= 5 * 1024 * 1024; // Max 5MB
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}