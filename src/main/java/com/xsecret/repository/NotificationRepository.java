package com.xsecret.repository;

import com.xsecret.entity.Notification;
import com.xsecret.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Lấy thông báo của user cụ thể (bao gồm cả broadcast)
    @Query("SELECT n FROM Notification n WHERE " +
           "(n.targetUser IS NULL OR n.targetUser = :user) " +
           "AND (n.expiresAt IS NULL OR n.expiresAt > :now) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findUserNotifications(
            @Param("user") User user,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    // Đếm thông báo chưa đọc của user
    @Query("SELECT COUNT(n) FROM Notification n WHERE " +
           "(n.targetUser IS NULL OR n.targetUser = :user) " +
           "AND n.isRead = false " +
           "AND (n.expiresAt IS NULL OR n.expiresAt > :now)")
    long countUnreadNotifications(
            @Param("user") User user,
            @Param("now") LocalDateTime now);

    // Lấy thông báo của user với filter
    @Query("SELECT n FROM Notification n WHERE " +
           "(n.targetUser IS NULL OR n.targetUser = :user) " +
           "AND (n.expiresAt IS NULL OR n.expiresAt > :now) " +
           "AND (:priority IS NULL OR n.priority = :priority) " +
           "AND (:isRead IS NULL OR n.isRead = :isRead) " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findUserNotificationsWithFilter(
            @Param("user") User user,
            @Param("now") LocalDateTime now,
            @Param("priority") Notification.NotificationPriority priority,
            @Param("isRead") Boolean isRead,
            Pageable pageable);

    // Admin: Lấy tất cả thông báo
    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Admin: Lấy thông báo broadcast
    Page<Notification> findByTargetUserIsNullOrderByCreatedAtDesc(Pageable pageable);

    // Admin: Lấy thông báo của user cụ thể
    Page<Notification> findByTargetUserOrderByCreatedAtDesc(User targetUser, Pageable pageable);

    // Xóa thông báo hết hạn
    @Query("DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    void deleteExpiredNotifications(@Param("now") LocalDateTime now);

    // Lấy thông báo theo type
    List<Notification> findByType(Notification.NotificationType type);
}

