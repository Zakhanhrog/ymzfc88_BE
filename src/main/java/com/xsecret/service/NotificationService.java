package com.xsecret.service;

import com.xsecret.dto.request.NotificationRequest;
import com.xsecret.dto.response.NotificationResponse;
import com.xsecret.entity.Notification;
import com.xsecret.entity.User;
import com.xsecret.repository.NotificationRepository;
import com.xsecret.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Admin: Tạo thông báo mới
     */
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request, String adminUsername) {
        log.info("Creating notification: {} by admin: {}", request.getTitle(), adminUsername);

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // Parse priority từ level (1,2,3)
        Notification.NotificationPriority priority = request.getPriority() != null ?
                Notification.NotificationPriority.fromLevel(request.getPriority()) :
                Notification.NotificationPriority.INFO;

        // Parse type
        Notification.NotificationType type = request.getType() != null ?
                Notification.NotificationType.valueOf(request.getType().toUpperCase()) :
                Notification.NotificationType.SYSTEM;

        // Get target user if specified
        User targetUser = null;
        if (request.getTargetUserId() != null) {
            targetUser = userRepository.findById(request.getTargetUserId())
                    .orElseThrow(() -> new RuntimeException("Target user not found"));
        }

        Notification notification = Notification.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .priority(priority)
                .type(type)
                .targetUser(targetUser)
                .createdBy(admin)
                .isRead(false)
                .expiresAt(request.getExpiresAt())
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        String scope = targetUser != null ? 
                "individual user: " + targetUser.getUsername() : 
                "all users (broadcast)";
        log.info("Created notification ID: {} for {}", savedNotification.getId(), scope);

        return NotificationResponse.fromEntity(savedNotification);
    }

    /**
     * User: Lấy thông báo của mình
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Page<Notification> notifications = notificationRepository.findUserNotifications(
                user,
                LocalDateTime.now(),
                pageable
        );

        return notifications.map(NotificationResponse::fromEntity);
    }

    /**
     * User: Đếm thông báo chưa đọc
     */
    @Transactional(readOnly = true)
    public long countUnreadNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.countUnreadNotifications(user, LocalDateTime.now());
    }

    /**
     * User: Đánh dấu đã đọc
     */
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kiểm tra quyền: chỉ được đánh dấu thông báo của mình
        if (notification.getTargetUser() != null && 
            !notification.getTargetUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("User {} marked notification {} as read", username, notificationId);
        }

        return NotificationResponse.fromEntity(notification);
    }

    /**
     * User: Đánh dấu tất cả đã đọc
     */
    @Transactional
    public void markAllAsRead(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Page<Notification> unreadNotifications = notificationRepository.findUserNotificationsWithFilter(
                user,
                LocalDateTime.now(),
                null,
                false,
                Pageable.unpaged()
        );

        unreadNotifications.getContent().forEach(notification -> {
            if (!notification.getIsRead()) {
                notification.setIsRead(true);
                notification.setReadAt(LocalDateTime.now());
            }
        });

        notificationRepository.saveAll(unreadNotifications.getContent());
        log.info("User {} marked all notifications as read", username);
    }

    /**
     * Admin: Lấy tất cả thông báo
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAllNotifications(Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findAllByOrderByCreatedAtDesc(pageable);
        return notifications.map(NotificationResponse::fromEntity);
    }

    /**
     * Admin: Xóa thông báo
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notificationRepository.delete(notification);
        log.info("Deleted notification ID: {}", notificationId);
    }

    /**
     * Admin: Xóa thông báo hết hạn (scheduled job)
     */
    @Transactional
    public void deleteExpiredNotifications() {
        notificationRepository.deleteExpiredNotifications(LocalDateTime.now());
        log.info("Deleted expired notifications");
    }

    /**
     * User: Lấy chi tiết thông báo
     */
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kiểm tra quyền
        if (notification.getTargetUser() != null && 
            !notification.getTargetUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return NotificationResponse.fromEntity(notification);
    }
}

