package com.xsecret.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {
    
    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Tạo thư mục upload nếu chưa có
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            // Log error but don't fail startup
            System.err.println("Could not create upload directory: " + e.getMessage());
        }
        
        // Serve static files từ upload directory
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
                
        // Handle banner static resources
        registry.addResourceHandler("/banners/**")
                .addResourceLocations("classpath:/static/banners/", "file:" + uploadDir + "/banners/");
    }
}