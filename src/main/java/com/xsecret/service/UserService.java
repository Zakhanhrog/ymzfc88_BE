package com.xsecret.service;

import com.xsecret.dto.request.CreateUserRequestDto;
import com.xsecret.dto.request.UpdateUserRequestDto;
import com.xsecret.dto.request.UserProfileUpdateRequest;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.util.StringUtils;

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
        return userRepository.findByRoleNot(User.Role.ADMIN);
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findByRoleNot(User.Role.ADMIN, pageable);
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

        String normalizedReferral = prepareReferralCode(request.getReferralCode());
        String normalizedInvitedBy = normalizeCode(request.getInvitedByCode());

        User.Role baseRole = User.Role.valueOf(request.getRole());

        User.StaffRole staffRole = null;
        if (baseRole != User.Role.ADMIN && request.getStaffRole() != null && !request.getStaffRole().isBlank()) {
            staffRole = User.StaffRole.valueOf(request.getStaffRole().toUpperCase(Locale.ROOT));
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .role(baseRole)
                .staffRole(staffRole)
                .invitedByCode(normalizedInvitedBy)
                .referralCode(normalizedReferral)
                .status(User.UserStatus.ACTIVE)
                .build();

        boolean isAdminAccount = baseRole == User.Role.ADMIN;
        boolean isStaffAccount = staffRole != null;
        if ((isAdminAccount || isStaffAccount) &&
                (request.getC2Password() == null || request.getC2Password().isBlank())) {
            throw new IllegalArgumentException("Tài khoản quản trị hoặc nhân viên bắt buộc phải có mật khẩu bảo vệ C2");
        }

        if (request.getC2Password() != null && !request.getC2Password().isBlank()) {
            if (!isAdminOrStaff(baseRole, staffRole)) {
                throw new IllegalArgumentException("Chỉ tài khoản quản trị hoặc nhân viên mới được thiết lập mật khẩu bảo vệ C2");
            }
            user.setC2PasswordHash(passwordEncoder.encode(request.getC2Password()));
            user.setC2PasswordUpdatedAt(LocalDateTime.now());
        }

        return userRepository.save(user);
    }

    /**
     * Cập nhật thông tin người dùng bởi admin
     */
    @Transactional
    public User updateUser(Long userId, UpdateUserRequestDto request) {
        log.info("Updating user: {}", userId);

        User user = getUserById(userId);

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UserAlreadyExistsException("Tên đăng nhập đã tồn tại: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }
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

    public String generateUniqueReferralCode() {
        String code;
        int maxAttempts = 10;
        int attempts = 0;
        do {
            code = randomAlphanumeric(5);
            attempts++;
        } while (userRepository.findByReferralCode(code).isPresent() && attempts < maxAttempts);
        if (userRepository.findByReferralCode(code).isPresent()) {
            throw new RuntimeException("Không thể tạo mã mời duy nhất, vui lòng thử lại sau");
        }
        return code;
    }

    private String randomAlphanumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder builder = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            builder.append(chars.charAt(random.nextInt(chars.length())));
        }
        return builder.toString();
    }

    private String prepareReferralCode(String requestedCode) {
        String normalized = normalizeCode(requestedCode);
        if (normalized == null) {
            return generateUniqueReferralCode();
        }
        if (userRepository.findByReferralCode(normalized).isPresent()) {
            throw new RuntimeException("Mã mời đã tồn tại: " + normalized);
        }
        return normalized;
    }

    private String normalizeCode(String code) {
        if (code == null) {
            return null;
        }
        String trimmed = code.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
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

        // Không hiển thị admin trong bất kỳ danh sách nào
        if (filters.getRole() != null) {
            User.Role requestedRole = User.Role.valueOf(filters.getRole());
            if (requestedRole == User.Role.ADMIN) {
                return Page.empty(pageable);
            }
        }

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
            return userRepository.findByStatusAndRoleNot(
                User.UserStatus.valueOf(filters.getStatus()),
                User.Role.ADMIN,
                pageable
            );
        }

        // Trả về tất cả
        return userRepository.findByRoleNot(User.Role.ADMIN, pageable);
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

    @Transactional
    public User updateUserProfile(Long userId, UserProfileUpdateRequest request) {
        User user = getUserById(userId);

        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email đã tồn tại: " + request.getEmail());
            }
            user.setEmail(request.getEmail().trim());
        }

        if (request.getPhoneNumber() != null) {
            String trimmedPhone = request.getPhoneNumber().trim();
            user.setPhoneNumber(trimmedPhone.isEmpty() ? null : trimmedPhone);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void changeUserPassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public void verifyC2Password(Long userId, String rawC2Password) {
        User user = getUserById(userId);
        verifyC2Password(user, rawC2Password);
    }

    @Transactional(readOnly = true)
    public void verifyC2Password(User user, String rawC2Password) {
        if (user.getC2PasswordHash() == null || user.getC2PasswordHash().isBlank()) {
            throw new IllegalStateException("Tài khoản chưa thiết lập mật khẩu bảo vệ C2");
        }
        if (rawC2Password == null || rawC2Password.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu bảo vệ C2");
        }
        if (!passwordEncoder.matches(rawC2Password, user.getC2PasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu bảo vệ C2 không chính xác");
        }
    }

    @Transactional
    public void updateUserC2Password(Long userId, String newPassword) {
        log.info("Updating C2 password for user: {}", userId);
        User user = getUserById(userId);
        if (!isAdminOrStaff(user)) {
            throw new IllegalArgumentException("Chỉ áp dụng mật khẩu C2 cho tài khoản quản trị hoặc nhân viên");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu bảo vệ C2 không được để trống");
        }
        user.setC2PasswordHash(passwordEncoder.encode(newPassword));
        user.setC2PasswordUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private boolean isAdminOrStaff(User user) {
        return isAdminOrStaff(user.getRole(), user.getStaffRole());
    }

    private boolean isAdminOrStaff(User.Role role, User.StaffRole staffRole) {
        if (role == User.Role.ADMIN) {
            return true;
        }
        return staffRole != null;
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
