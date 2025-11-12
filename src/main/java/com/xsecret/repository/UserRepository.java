package com.xsecret.repository;

import com.xsecret.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRole(User.Role role);

    List<User> findByStatus(User.UserStatus status);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.status = :status")
    List<User> findByRoleAndStatus(@Param("role") User.Role role, @Param("status") User.UserStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'USER'")
    long countUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'ADMIN'")
    long countAdmins();

    // ================ NEW METHODS FOR ADMIN USER MANAGEMENT ================

    // Pagination methods
    Page<User> findByRole(User.Role role, Pageable pageable);

    Page<User> findByStatus(User.UserStatus status, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.status = :status")
    Page<User> findByRoleAndStatus(@Param("role") User.Role role, @Param("status") User.UserStatus status, Pageable pageable);

    // Search functionality
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<User> findBySearchTermWithFilters(@Param("searchTerm") String searchTerm,
                                         @Param("role") User.Role role,
                                         @Param("status") User.UserStatus status,
                                         Pageable pageable);

    // Statistics methods
    long countByStatus(User.UserStatus status);

    long countByRole(User.Role role);

    long countByCreatedAtAfter(LocalDateTime dateTime);

    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status AND u.lastLogin IS NOT NULL AND u.lastLogin >= :since")
    long countOnlineUsers(@Param("status") User.UserStatus status, @Param("since") LocalDateTime since);

    @Query("SELECT DATE(u.createdAt) AS day, COUNT(u) FROM User u WHERE u.createdAt BETWEEN :start AND :end GROUP BY DATE(u.createdAt)")
    List<Object[]> countNewUsersByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Top users by points
    @Query("SELECT u FROM User u WHERE u.points > 0 ORDER BY u.points DESC")
    List<User> findTop10ByOrderByPointsDesc();

    // Recent active users
    @Query("SELECT u FROM User u WHERE u.lastLogin IS NOT NULL ORDER BY u.lastLogin DESC")
    List<User> findByLastLoginIsNotNullOrderByLastLoginDesc(Pageable pageable);

    // Users with transactions in date range
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN Transaction t ON t.user.id = u.id " +
           "WHERE t.createdAt BETWEEN :startDate AND :endDate")
    List<User> findUsersWithTransactionsBetween(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    // Users by points range
    @Query("SELECT u FROM User u WHERE u.points BETWEEN :minPoints AND :maxPoints")
    List<User> findByPointsBetween(@Param("minPoints") Long minPoints, @Param("maxPoints") Long maxPoints);

    Page<User> findByStaffRole(User.StaffRole staffRole, Pageable pageable);

    List<User> findByStaffRole(User.StaffRole staffRole);

    Page<User> findByStaffRoleIn(Collection<User.StaffRole> roles, Pageable pageable);

    Optional<User> findByReferralCode(String referralCode);

    long countByInvitedByCodeIgnoreCase(String invitedByCode);

    List<User> findTop20ByInvitedByCodeIgnoreCaseOrderByCreatedAtDesc(String invitedByCode);

    long countByInvitedByCodeIgnoreCaseAndStatus(String invitedByCode, User.UserStatus status);

    List<User> findByInvitedByCodeIgnoreCase(String invitedByCode);

    @Query("""
        SELECT u FROM User u
        WHERE UPPER(u.invitedByCode) = UPPER(:referralCode)
          AND (:status IS NULL OR u.status = :status)
          AND (:searchTerm IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        ORDER BY u.createdAt DESC
    """)
    Page<User> findAgentCustomers(
            @Param("referralCode") String referralCode,
            @Param("status") User.UserStatus status,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );
}
