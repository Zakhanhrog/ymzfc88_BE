package com.xsecret.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
     * Lưu ảnh promotion base64 thành file và trả về URL
     */
    public String savePromotionImage(String base64Data, String originalFileName) {
        try {
            // Tạo thư mục nếu chưa có
            Path uploadPath = Paths.get(uploadDir, "promotions");
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
            String imageUrl = "/uploads/promotions/" + fileName;
            log.info("Saved promotion image: {}", imageUrl);
            
            return imageUrl;
            
        } catch (IOException e) {
            log.error("Error saving promotion image", e);
            throw new RuntimeException("Failed to save promotion image: " + e.getMessage());
        }
    }
    
    /**
     * Xóa file ảnh
     */
    public boolean deleteImage(String imageUrl) {
        try {
            if (imageUrl == null || (!imageUrl.startsWith("/uploads/bills/") && !imageUrl.startsWith("/uploads/promotions/"))) {
                return false;
            }
            
            String fileName;
            String folder;
            
            if (imageUrl.startsWith("/uploads/bills/")) {
                fileName = imageUrl.substring("/uploads/bills/".length());
                folder = "bills";
            } else {
                fileName = imageUrl.substring("/uploads/promotions/".length());
                folder = "promotions";
            }
            
            Path filePath = Paths.get(uploadDir, folder, fileName);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted image: {}", imageUrl);
                return true;
            }
            
            return false;
            
        } catch (IOException e) {
            log.error("Error deleting image: {}", imageUrl, e);
            return false;
        }
    }
    
    /**
     * Kiểm tra file có tồn tại không
     */
    public boolean imageExists(String imageUrl) {
        if (imageUrl == null || (!imageUrl.startsWith("/uploads/bills/") && !imageUrl.startsWith("/uploads/promotions/"))) {
            return false;
        }
        
        String fileName;
        String folder;
        
        if (imageUrl.startsWith("/uploads/bills/")) {
            fileName = imageUrl.substring("/uploads/bills/".length());
            folder = "bills";
        } else {
            fileName = imageUrl.substring("/uploads/promotions/".length());
            folder = "promotions";
        }
        
        Path filePath = Paths.get(uploadDir, folder, fileName);
        
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
    
    /**
     * Lưu file MultipartFile và trả về URL
     */
    public String saveFile(MultipartFile file, String subDirectory, String filename) throws IOException {
        // Tạo thư mục nếu chưa có
        Path uploadPath = Paths.get(uploadDir, subDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Lưu file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);
        
        // Trả về URL
        return "/uploads/" + subDirectory + "/" + filename;
    }
}