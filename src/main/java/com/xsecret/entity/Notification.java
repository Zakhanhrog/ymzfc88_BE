package com.xsecret.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private NotificationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser; // Null = broadcast to all

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy; // Admin who created the notification

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public enum NotificationPriority {
        URGENT(1, "Khẩn cấp", "#ff4d4f"),      // Đỏ - Mức 1
        WARNING(2, "Cảnh báo", "#faad14"),     // Vàng - Mức 2
        INFO(3, "Thông thường", "#52c41a");    // Xanh - Mức 3

        private final int level;
        private final String label;
        private final String color;

        NotificationPriority(int level, String label, String color) {
            this.level = level;
            this.label = label;
            this.color = color;
        }

        public int getLevel() {
            return level;
        }

        public String getLabel() {
            return label;
        }

        public String getColor() {
            return color;
        }

        public static NotificationPriority fromLevel(int level) {
            for (NotificationPriority priority : values()) {
                if (priority.level == level) {
                    return priority;
                }
            }
            return INFO; // Default
        }
    }

    public enum NotificationType {
        SYSTEM,           // Thông báo hệ thống
        MAINTENANCE,      // Bảo trì
        PROMOTION,        // Khuyến mãi
        SECURITY,         // Bảo mật
        TRANSACTION,      // Giao dịch
        ACCOUNT,          // Tài khoản
        ANNOUNCEMENT      // Thông báo chung
    }
}

