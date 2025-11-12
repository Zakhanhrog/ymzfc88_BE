package com.xsecret.service;

import com.xsecret.dto.request.CreateUserRequestDto;
import com.xsecret.dto.request.UpdateUserRequestDto;
import com.xsecret.dto.request.UserFilterRequestDto;
import com.xsecret.entity.User;
import com.xsecret.exception.UserAlreadyExistsException;
import com.xsecret.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final List<User.StaffRole> STAFF_ONLY_ROLES = List.of(
            User.StaffRole.STAFF_TX1,
            User.StaffRole.STAFF_TX2,
            User.StaffRole.STAFF_XD,
            User.StaffRole.STAFF_MKT,
            User.StaffRole.STAFF_XNK
    );

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + username));
    }

    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    public List<User> getUsersByStatus(User.UserStatus status) {
        return userRepository.findByStatus(status);
    }

    @Transactional
    public User updateUserStatus(Long userId, User.UserStatus status) {
        User user = getUserById(userId);
        user.setStatus(status);
        return userRepository.save(user);
    }

    // ================ ADVANCED USER MANAGEMENT ================

    /**
     * Tạo người dùng mới bởi admin
     */
    @Transactional
    public User createUser(CreateUserRequestDto request) {
        log.info("Creating new user: {}", request.getUsername());

        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Tên đăng nhập đã tồn tại: " + request.getUsername());
        }

        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email đã tồn tại: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .role(User.Role.valueOf(request.getRole()))
                .status(User.UserStatus.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    /**
     * Cập nhật thông tin người dùng bởi admin
     */
    @Transactional
    public User updateUser(Long userId, UpdateUserRequestDto request) {
        log.info("Updating user: {}", userId);

        User user = getUserById(userId);

        // Cập nhật thông tin
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email đã tồn tại: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        return userRepository.save(user);
    }

    /**
     * Xóa người dùng (soft delete - chuyển trạng thái thành BANNED)
     */
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Deleting user: {}", userId);
        User user = getUserById(userId);
        user.setStatus(User.UserStatus.BANNED);
        userRepository.save(user);
    }

    /**
     * Lấy danh sách người dùng với phân trang và filter
     */
    public Page<User> getUsersWithFilters(UserFilterRequestDto filters) {
        // Thiết lập sort
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // Default sort
        if (filters.getSortBy() != null) {
            Sort.Direction direction = "asc".equalsIgnoreCase(filters.getSortDirection()) 
                ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, filters.getSortBy());
        }

        // Thiết lập pageable
        int page = filters.getPage() != null ? filters.getPage() : 0;
        int size = filters.getSize() != null ? filters.getSize() : 10;
        Pageable pageable = PageRequest.of(page, size, sort);

        // Nếu có search term, filter theo custom query
        if (filters.getSearchTerm() != null && !filters.getSearchTerm().trim().isEmpty()) {
            return userRepository.findBySearchTermWithFilters(
                filters.getSearchTerm(),
                filters.getRole() != null ? User.Role.valueOf(filters.getRole()) : null,
                filters.getStatus() != null ? User.UserStatus.valueOf(filters.getStatus()) : null,
                pageable
            );
        }

        // Filter theo role và status
        if (filters.getRole() != null && filters.getStatus() != null) {
            return userRepository.findByRoleAndStatus(
                User.Role.valueOf(filters.getRole()),
                User.UserStatus.valueOf(filters.getStatus()),
                pageable
            );
        } else if (filters.getRole() != null) {
            return userRepository.findByRole(User.Role.valueOf(filters.getRole()), pageable);
        } else if (filters.getStatus() != null) {
            return userRepository.findByStatus(User.UserStatus.valueOf(filters.getStatus()), pageable);
        }

        // Trả về tất cả
        return userRepository.findAll(pageable);
    }

    /**
     * Reset mật khẩu người dùng
     */
    @Transactional
    public void resetUserPassword(Long userId, String newPassword) {
        log.info("Resetting password for user: {}", userId);
        User user = getUserById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public Page<User> getUsersByStaffRole(User.StaffRole staffRole, Pageable pageable) {
        if (staffRole == null) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByStaffRole(staffRole, pageable);
    }

    public Page<User> getStaffMembers(Pageable pageable) {
        return userRepository.findByStaffRoleIn(STAFF_ONLY_ROLES, pageable);
    }

    @Transactional
    public User updateStaffRole(Long userId, User.StaffRole staffRole) {
        User user = getUserById(userId);
        user.setStaffRole(staffRole);
        return userRepository.save(user);
    }

    /**
     * Thống kê người dùng nâng cao
     */
    public Map<String, Object> getAdvancedUserStats() {
        Map<String, Object> stats = new HashMap<>();

        // Thống kê theo trạng thái
        Map<String, Long> statusStats = new HashMap<>();
        for (User.UserStatus status : User.UserStatus.values()) {
            statusStats.put(status.name(), userRepository.countByStatus(status));
        }
        stats.put("usersByStatus", statusStats);

        // Thống kê theo vai trò
        Map<String, Long> roleStats = new HashMap<>();
        for (User.Role role : User.Role.values()) {
            roleStats.put(role.name(), userRepository.countByRole(role));
        }
        stats.put("usersByRole", roleStats);

        // Người dùng mới trong 30 ngày
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long newUsersLast30Days = userRepository.countByCreatedAtAfter(thirtyDaysAgo);
        stats.put("newUsersLast30Days", newUsersLast30Days);

        // Top users theo points
        List<User> topUsersByPoints = userRepository.findTop10ByOrderByPointsDesc();
        stats.put("topUsersByPoints", topUsersByPoints);

        return stats;
    }

    /**
     * Lấy hoạt động gần đây của người dùng
     */
    public List<User> getRecentActiveUsers(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return userRepository.findByLastLoginIsNotNullOrderByLastLoginDesc(pageable);
    }

    /**
     * Cập nhật thời gian đăng nhập cuối
     */
    @Transactional
    public void updateLastLogin(String username) {
        User user = getUserByUsername(username);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Khóa rút tiền cho người dùng
     */
    @Transactional
    public User lockWithdrawal(Long userId, String reason, Long adminId) {
        log.info("Locking withdrawal for user: {} by admin: {}", userId, adminId);
        User user = getUserById(userId);
        
        user.setWithdrawalLocked(true);
        user.setWithdrawalLockReason(reason);
        user.setWithdrawalLockedAt(LocalDateTime.now());
        user.setWithdrawalLockedBy(adminId);
        
        return userRepository.save(user);
    }

    /**
     * Mở khóa rút tiền cho người dùng
     */
    @Transactional
    public User unlockWithdrawal(Long userId) {
        log.info("Unlocking withdrawal for user: {}", userId);
        User user = getUserById(userId);
        
        user.setWithdrawalLocked(false);
        user.setWithdrawalLockReason(null);
        user.setWithdrawalLockedAt(null);
        user.setWithdrawalLockedBy(null);
        
        return userRepository.save(user);
    }

    /**
     * Kiểm tra xem người dùng có bị khóa rút tiền không
     */
    public boolean isWithdrawalLocked(Long userId) {
        User user = getUserById(userId);
        return user.getWithdrawalLocked() != null && user.getWithdrawalLocked();
    }
}
