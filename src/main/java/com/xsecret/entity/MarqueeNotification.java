package com.xsecret.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "marquee_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MarqueeNotification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "content", columnDefinition = "TEXT CHARACTER SET utf8 COLLATE utf8_unicode_ci", nullable = false)
    private String content;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
    
    @Column(name = "text_color", length = 7)
    @Builder.Default
    private String textColor = "#FF0000"; // Màu đỏ mặc định
    
    @Column(name = "background_color", length = 7)
    @Builder.Default
    private String backgroundColor = "#FFFFFF"; // Màu trắng mặc định
    
    @Column(name = "font_size")
    @Builder.Default
    private Integer fontSize = 16;
    
    @Column(name = "speed")
    @Builder.Default
    private Integer speed = 50; // Tốc độ chạy (pixel per second)
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;
}
