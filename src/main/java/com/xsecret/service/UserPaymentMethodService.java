package com.xsecret.service;

import com.xsecret.dto.UserPaymentMethodRequestDto;
import com.xsecret.dto.UserPaymentMethodResponseDto;
import com.xsecret.entity.User;
import com.xsecret.entity.UserPaymentMethod;
import com.xsecret.entity.PaymentMethod;
import com.xsecret.repository.UserPaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPaymentMethodService {
    
    private final UserPaymentMethodRepository userPaymentMethodRepository;
    
    /**
     * Lấy tất cả phương thức thanh toán của user
     */
    public List<UserPaymentMethodResponseDto> getUserPaymentMethods(User user) {
        log.info("Getting payment methods for user: {}", user.getId());
        
        List<UserPaymentMethod> paymentMethods = userPaymentMethodRepository
            .findByUserOrderByIsDefaultDescCreatedAtDesc(user);
        
        return paymentMethods.stream()
            .map(this::convertToResponseDto)
            .collect(Collectors.toList());
    }
    
    /**
     * Tạo phương thức thanh toán mới
     */
    @Transactional
    public UserPaymentMethodResponseDto createUserPaymentMethod(User user, UserPaymentMethodRequestDto requestDto) {
        log.info("Creating payment method for user: {}", user.getId());
        
        // Validate input
        validatePaymentMethodRequest(requestDto);
        
        // Kiểm tra trùng lặp số tài khoản
        if (userPaymentMethodRepository.existsByUserAndAccountNumberAndType(
                user, requestDto.getAccountNumber(), requestDto.getType())) {
            throw new RuntimeException("Số tài khoản này đã được đăng ký cho loại phương thức thanh toán này");
        }
        
        // Kiểm tra số lượng phương thức thanh toán tối đa (giới hạn 10)
        long currentCount = userPaymentMethodRepository.countByUser(user);
        if (currentCount >= 10) {
            throw new RuntimeException("Bạn chỉ có thể tạo tối đa 10 phương thức thanh toán");
        }
        
        // Tạo entity mới
        UserPaymentMethod paymentMethod = UserPaymentMethod.builder()
            .user(user)
            .name(requestDto.getName().trim())
            .type(requestDto.getType())
            .accountNumber(requestDto.getAccountNumber().trim())
            .accountName(requestDto.getAccountName().trim())
            .bankCode(requestDto.getBankCode() != null ? requestDto.getBankCode().trim().toUpperCase() : null)
            .note(requestDto.getNote() != null ? requestDto.getNote().trim() : null)
            .isDefault(false) // Mặc định không phải là default
            .isVerified(true) // Mặc định đã xác thực
            .createdAt(LocalDateTime.now())
            .build();
        
        // Nếu đây là phương thức đầu tiên thì set làm default
        if (currentCount == 0) {
            paymentMethod.setIsDefault(true);
        }
        
        UserPaymentMethod savedMethod = userPaymentMethodRepository.save(paymentMethod);
        
        log.info("Created payment method with ID: {} for user: {}", savedMethod.getId(), user.getId());
        
        return convertToResponseDto(savedMethod);
    }
    
    /**
     * Cập nhật phương thức thanh toán
     */
    @Transactional
    public UserPaymentMethodResponseDto updateUserPaymentMethod(
            User user, Long paymentMethodId, UserPaymentMethodRequestDto requestDto) {
        log.info("Updating payment method ID: {} for user: {}", paymentMethodId, user.getId());
        
        // Validate input
        validatePaymentMethodRequest(requestDto);
        
        // Tìm phương thức thanh toán
        UserPaymentMethod paymentMethod = userPaymentMethodRepository
            .findByUserAndId(user, paymentMethodId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy phương thức thanh toán"));
        
        // Kiểm tra trùng lặp số tài khoản (trừ chính nó)
        boolean isDuplicate = userPaymentMethodRepository
            .findByUserAndType(user, requestDto.getType())
            .stream()
            .anyMatch(pm -> !pm.getId().equals(paymentMethodId) && 
                          pm.getAccountNumber().equals(requestDto.getAccountNumber()));
        
        if (isDuplicate) {
            throw new RuntimeException("Số tài khoản này đã được đăng ký cho loại phương thức thanh toán này");
        }
        
        // Cập nhật thông tin
        paymentMethod.setName(requestDto.getName().trim());
        paymentMethod.setType(requestDto.getType());
        paymentMethod.setAccountNumber(requestDto.getAccountNumber().trim());
        paymentMethod.setAccountName(requestDto.getAccountName().trim());
        paymentMethod.setBankCode(requestDto.getBankCode() != null ? requestDto.getBankCode().trim().toUpperCase() : null);
        paymentMethod.setNote(requestDto.getNote() != null ? requestDto.getNote().trim() : null);
        paymentMethod.setUpdatedAt(LocalDateTime.now());
        
        UserPaymentMethod updatedMethod = userPaymentMethodRepository.save(paymentMethod);
        
        log.info("Updated payment method ID: {} for user: {}", paymentMethodId, user.getId());
        
        return convertToResponseDto(updatedMethod);
    }
    
    /**
     * Xóa phương thức thanh toán
     */
    @Transactional
    public void deleteUserPaymentMethod(User user, Long paymentMethodId) {
        log.info("Deleting payment method ID: {} for user: {}", paymentMethodId, user.getId());
        
        UserPaymentMethod paymentMethod = userPaymentMethodRepository
            .findByUserAndId(user, paymentMethodId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy phương thức thanh toán"));
        
        // Không cho phép xóa phương thức mặc định nếu còn phương thức khác
        if (paymentMethod.getIsDefault()) {
            long totalMethods = userPaymentMethodRepository.countByUser(user);
            if (totalMethods > 1) {
                throw new RuntimeException("Không thể xóa phương thức mặc định. Vui lòng đặt phương thức khác làm mặc định trước.");
            }
        }
        
        userPaymentMethodRepository.delete(paymentMethod);
        
        log.info("Deleted payment method ID: {} for user: {}", paymentMethodId, user.getId());
    }
    
    /**
     * Đặt phương thức thanh toán làm mặc định
     */
    @Transactional
    public UserPaymentMethodResponseDto setDefaultPaymentMethod(User user, Long paymentMethodId) {
        log.info("Setting payment method ID: {} as default for user: {}", paymentMethodId, user.getId());
        
        UserPaymentMethod paymentMethod = userPaymentMethodRepository
            .findByUserAndId(user, paymentMethodId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy phương thức thanh toán"));
        
        // Reset tất cả phương thức về không phải mặc định
        userPaymentMethodRepository.resetDefaultPaymentMethods(user);
        
        // Đặt phương thức này làm mặc định
        paymentMethod.setIsDefault(true);
        paymentMethod.setUpdatedAt(LocalDateTime.now());
        
        UserPaymentMethod updatedMethod = userPaymentMethodRepository.save(paymentMethod);
        
        log.info("Set payment method ID: {} as default for user: {}", paymentMethodId, user.getId());
        
        return convertToResponseDto(updatedMethod);
    }
    
    /**
     * Lấy phương thức thanh toán mặc định
     */
    public UserPaymentMethodResponseDto getDefaultPaymentMethod(User user) {
        return userPaymentMethodRepository.findByUserAndIsDefaultTrue(user)
            .map(this::convertToResponseDto)
            .orElse(null);
    }
    
    /**
     * Lấy phương thức thanh toán theo ID
     */
    public UserPaymentMethodResponseDto getPaymentMethodById(User user, Long paymentMethodId) {
        UserPaymentMethod paymentMethod = userPaymentMethodRepository
            .findByUserAndId(user, paymentMethodId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy phương thức thanh toán"));
        
        return convertToResponseDto(paymentMethod);
    }
    
    /**
     * Validate payment method request
     */
    private void validatePaymentMethodRequest(UserPaymentMethodRequestDto requestDto) {
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên phương thức không được để trống");
        }
        
        if (requestDto.getType() == null) {
            throw new RuntimeException("Loại phương thức không được để trống");
        }
        
        if (requestDto.getAccountNumber() == null || requestDto.getAccountNumber().trim().isEmpty()) {
            throw new RuntimeException("Số tài khoản không được để trống");
        }
        
        if (requestDto.getAccountName() == null || requestDto.getAccountName().trim().isEmpty()) {
            throw new RuntimeException("Tên chủ tài khoản không được để trống");
        }
        
        // Validate bank code for bank type
        if (PaymentMethod.PaymentType.BANK.equals(requestDto.getType())) {
            if (requestDto.getBankCode() == null || requestDto.getBankCode().trim().isEmpty()) {
                throw new RuntimeException("Mã ngân hàng không được để trống đối với phương thức ngân hàng");
            }
        }
        
        // Validate account number format
        String accountNumber = requestDto.getAccountNumber().trim();
        if (PaymentMethod.PaymentType.BANK.equals(requestDto.getType())) {
            // Bank account: 8-20 digits
            if (!accountNumber.matches("\\d{8,20}")) {
                throw new RuntimeException("Số tài khoản ngân hàng phải có từ 8-20 chữ số");
            }
        } else if (PaymentMethod.PaymentType.MOMO.equals(requestDto.getType()) || 
                   PaymentMethod.PaymentType.ZALO_PAY.equals(requestDto.getType())) {
            // Mobile wallet: phone number format
            if (!accountNumber.matches("^(0|\\+84)[3-9]\\d{8}$")) {
                throw new RuntimeException("Số điện thoại không đúng định dạng");
            }
        }
    }
    
    /**
     * Convert entity to response DTO
     */
    private UserPaymentMethodResponseDto convertToResponseDto(UserPaymentMethod paymentMethod) {
        return UserPaymentMethodResponseDto.builder()
            .id(paymentMethod.getId())
            .name(paymentMethod.getName())
            .type(paymentMethod.getType())
            .accountNumber(paymentMethod.getAccountNumber())
            .accountName(paymentMethod.getAccountName())
            .bankCode(paymentMethod.getBankCode())
            .note(paymentMethod.getNote())
            .isDefault(paymentMethod.getIsDefault())
            .isVerified(paymentMethod.getIsVerified())
            .createdAt(paymentMethod.getCreatedAt())
            .updatedAt(paymentMethod.getUpdatedAt())
            .build();
    }
}