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
        
        // Chỉ cho phép BANK và E_WALLET
        if (requestDto.getType() != PaymentMethod.PaymentType.BANK && 
            requestDto.getType() != PaymentMethod.PaymentType.E_WALLET) {
            throw new RuntimeException("Chỉ được phép thêm phương thức Ngân hàng hoặc Ví điện tử");
        }
        
        // Normalize accountNumber: 
        // - E_WALLET: chỉ trim, không normalize (giữ nguyên 10 số)
        // - BANK: normalize (bỏ khoảng trắng, dấu chấm, gạch)
        String originalAccountNumber = requestDto.getAccountNumber();
        String normalizedAccountNumber;
        if (PaymentMethod.PaymentType.E_WALLET.equals(requestDto.getType())) {
            // E_WALLET: chỉ trim, không normalize
            normalizedAccountNumber = originalAccountNumber != null ? originalAccountNumber.trim() : null;
        } else {
            // BANK: normalize (bỏ khoảng trắng, dấu chấm, gạch)
            normalizedAccountNumber = normalizeAccountNumber(originalAccountNumber, requestDto.getType());
        }
        requestDto.setAccountNumber(normalizedAccountNumber);

        // Validate input
        validatePaymentMethodRequest(requestDto);
        
        // Kiểm tra trùng lặp số tài khoản
        if (userPaymentMethodRepository.existsByUserAndAccountNumberAndType(
                user, requestDto.getAccountNumber(), requestDto.getType())) {
            throw new RuntimeException("Số tài khoản này đã được đăng ký cho loại phương thức thanh toán này");
        }
        
        // Kiểm tra mỗi loại chỉ được thêm 1 lần
        List<UserPaymentMethod> existingMethods = userPaymentMethodRepository.findByUserAndType(user, requestDto.getType());
        if (!existingMethods.isEmpty()) {
            throw new RuntimeException("Bạn đã có phương thức " + 
                (requestDto.getType() == PaymentMethod.PaymentType.BANK ? "Ngân hàng" : "Ví điện tử") + 
                ". Mỗi loại chỉ được thêm 1 lần.");
        }
        
        // Đếm số lượng phương thức hiện tại
        long currentCount = userPaymentMethodRepository.countByUser(user);
        
        // Normalize phoneNumber: chỉ trim, không normalize
        String normalizedPhoneNumber = requestDto.getPhoneNumber() != null ? requestDto.getPhoneNumber().trim() : null;
        
        // Tạo entity mới
        UserPaymentMethod paymentMethod = UserPaymentMethod.builder()
            .user(user)
            .name(requestDto.getName().trim())
            .type(requestDto.getType())
            .accountNumber(requestDto.getAccountNumber().trim())
            .phoneNumber(normalizedPhoneNumber)
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
     * KHÔNG CHO PHÉP: Người dùng không được phép chỉnh sửa thông tin
     */
    @Transactional
    public UserPaymentMethodResponseDto updateUserPaymentMethod(
            User user, Long paymentMethodId, UserPaymentMethodRequestDto requestDto) {
        log.info("Updating payment method ID: {} for user: {}", paymentMethodId, user.getId());
        
        // Không cho phép người dùng chỉnh sửa thông tin
        throw new RuntimeException("Bạn không được phép chỉnh sửa thông tin phương thức thanh toán. Vui lòng xóa và tạo mới nếu cần thay đổi.");
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
        
        if (requestDto.getPhoneNumber() == null || requestDto.getPhoneNumber().trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }
        
        if (requestDto.getAccountName() == null || requestDto.getAccountName().trim().isEmpty()) {
            throw new RuntimeException("Tên chủ tài khoản không được để trống");
        }
        
        // Validate phone number format: phải có đúng 10 số và bắt đầu bằng 0
        String phoneNumber = requestDto.getPhoneNumber().trim();
        if (!phoneNumber.matches("^0\\d{9}$")) {
            throw new RuntimeException("Số điện thoại phải có đúng 10 số và bắt đầu bằng 0 (ví dụ: 0912345678)");
        }
        
        // Validate bank code for bank type
        if (PaymentMethod.PaymentType.BANK.equals(requestDto.getType())) {
            if (requestDto.getBankCode() == null || requestDto.getBankCode().trim().isEmpty()) {
                throw new RuntimeException("Mã ngân hàng không được để trống đối với phương thức ngân hàng");
            }
        }
        
        // Validate account number format (số đã được normalize trước đó)
        String accountNumber = requestDto.getAccountNumber(); // Đã được normalize, không cần trim nữa
        // Cả 2 loại đều có thể có chữ và số, tối đa 60 ký tự
        if (accountNumber.length() > 60) {
            throw new RuntimeException("Số tài khoản không được vượt quá 60 ký tự");
        }
        if (accountNumber.isEmpty()) {
            throw new RuntimeException("Số tài khoản không được để trống");
        }
    }

    /**
     * Chuẩn hóa số tài khoản/điện thoại: bỏ khoảng trắng, dấu chấm, gạch
     */
    private String normalizeAccountNumber(String raw, PaymentMethod.PaymentType type) {
        if (raw == null) return null;
        // Với E_WALLET, giữ nguyên dấu + ở đầu nếu có
        if (PaymentMethod.PaymentType.E_WALLET.equals(type)) {
            // Giữ dấu + ở đầu, bỏ khoảng trắng, dấu chấm, gạch ở giữa
            String cleaned = raw.replaceAll("[\\s\\-\\.]", "");
            return cleaned;
        } else {
            // Với BANK, chỉ bỏ khoảng trắng, dấu chấm, gạch
            return raw.replaceAll("[\\s\\-\\.]", "");
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
            .phoneNumber(paymentMethod.getPhoneNumber())
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