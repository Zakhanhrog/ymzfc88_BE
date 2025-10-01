package com.xsecret.service;

import com.xsecret.dto.response.UserResponse;
import com.xsecret.entity.User;
import com.xsecret.mapper.UserMapper;
import com.xsecret.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<User> getAllUsers() {
        return userRepository.findAll();
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

    @Transactional
    public User updateUserBalance(Long userId, Double balance) {
        User user = getUserById(userId);
        user.setBalance(balance);
        return userRepository.save(user);
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalUsers = userRepository.countUsers();
        long totalAdmins = userRepository.countAdmins();
        long activeUsers = userRepository.findByRoleAndStatus(User.Role.USER, User.UserStatus.ACTIVE).size();
        
        stats.put("totalUsers", totalUsers);
        stats.put("totalAdmins", totalAdmins);
        stats.put("activeUsers", activeUsers);
        stats.put("totalBalance", calculateTotalBalance());
        
        return stats;
    }

    private Double calculateTotalBalance() {
        return userRepository.findAll().stream()
                .mapToDouble(user -> user.getBalance() != null ? user.getBalance() : 0.0)
                .sum();
    }
}
