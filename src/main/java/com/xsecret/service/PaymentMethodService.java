package com.xsecret.service;

import com.xsecret.dto.request.PaymentMethodRequestDto;
import com.xsecret.dto.response.PaymentMethodResponseDto;
import com.xsecret.entity.PaymentMethod;
import com.xsecret.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentMethodService {
    
    private final PaymentMethodRepository paymentMethodRepository;
    
    /**
     * Lấy tất cả payment methods đang active cho user
     */
    public List<PaymentMethodResponseDto> getActivePaymentMethods() {
        List<PaymentMethod> paymentMethods = paymentMethodRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return paymentMethods.stream()
                .map(PaymentMethodResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy payment methods theo type
     */
    public List<PaymentMethodResponseDto> getPaymentMethodsByType(PaymentMethod.PaymentType type) {
        List<PaymentMethod> paymentMethods = paymentMethodRepository.findByTypeAndIsActiveTrueOrderByDisplayOrderAsc(type);
        return paymentMethods.stream()
                .map(PaymentMethodResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Admin: Lấy tất cả payment methods
     */
    public List<PaymentMethodResponseDto> getAllPaymentMethods() {
        List<PaymentMethod> paymentMethods = paymentMethodRepository.findAll();
        return paymentMethods.stream()
                .map(PaymentMethodResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Admin: Tạo payment method mới
     */
    public PaymentMethodResponseDto createPaymentMethod(PaymentMethodRequestDto request) {
        // Kiểm tra account number đã tồn tại
        if (paymentMethodRepository.existsByAccountNumberAndType(request.getAccountNumber(), request.getType())) {
            throw new RuntimeException("Payment method with this account number and type already exists");
        }
        
        // Validate amount
        if (request.getMaxAmount().compareTo(request.getMinAmount()) <= 0) {
            throw new RuntimeException("Maximum amount must be greater than minimum amount");
        }
        
        // Set display order nếu chưa có
        if (request.getDisplayOrder() == null) {
            Integer maxOrder = paymentMethodRepository.getMaxDisplayOrder();
            request.setDisplayOrder(maxOrder + 1);
        }
        
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .type(request.getType())
                .name(request.getName())
                .accountNumber(request.getAccountNumber())
                .accountName(request.getAccountName())
                .bankCode(request.getBankCode())
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .feePercent(request.getFeePercent() != null ? request.getFeePercent() : BigDecimal.ZERO)
                .feeFixed(request.getFeeFixed() != null ? request.getFeeFixed() : BigDecimal.ZERO)
                .processingTime(request.getProcessingTime())
                .isActive(request.getIsActive())
                .displayOrder(request.getDisplayOrder())
                .description(request.getDescription())
                .qrCode(request.getQrCode())
                .build();
        
        PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);
        log.info("Created new payment method: {} - {}", savedPaymentMethod.getType(), savedPaymentMethod.getName());
        
        return PaymentMethodResponseDto.fromEntity(savedPaymentMethod);
    }
    
    /**
     * Admin: Cập nhật payment method
     */
    public PaymentMethodResponseDto updatePaymentMethod(Long id, PaymentMethodRequestDto request) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found"));
        
        // Kiểm tra account number conflict (trừ chính nó)
        paymentMethodRepository.findByAccountNumber(request.getAccountNumber())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(id) && existing.getType().equals(request.getType())) {
                        throw new RuntimeException("Payment method with this account number and type already exists");
                    }
                });
        
        // Validate amount
        if (request.getMaxAmount().compareTo(request.getMinAmount()) <= 0) {
            throw new RuntimeException("Maximum amount must be greater than minimum amount");
        }
        
        // Update fields
        paymentMethod.setType(request.getType());
        paymentMethod.setName(request.getName());
        paymentMethod.setAccountNumber(request.getAccountNumber());
        paymentMethod.setAccountName(request.getAccountName());
        paymentMethod.setBankCode(request.getBankCode());
        paymentMethod.setMinAmount(request.getMinAmount());
        paymentMethod.setMaxAmount(request.getMaxAmount());
        paymentMethod.setFeePercent(request.getFeePercent() != null ? request.getFeePercent() : BigDecimal.ZERO);
        paymentMethod.setFeeFixed(request.getFeeFixed() != null ? request.getFeeFixed() : BigDecimal.ZERO);
        paymentMethod.setProcessingTime(request.getProcessingTime());
        paymentMethod.setIsActive(request.getIsActive());
        paymentMethod.setDisplayOrder(request.getDisplayOrder());
        paymentMethod.setDescription(request.getDescription());
        paymentMethod.setQrCode(request.getQrCode());
        
        PaymentMethod updatedPaymentMethod = paymentMethodRepository.save(paymentMethod);
        log.info("Updated payment method: {} - {}", updatedPaymentMethod.getType(), updatedPaymentMethod.getName());
        
        return PaymentMethodResponseDto.fromEntity(updatedPaymentMethod);
    }
    
    /**
     * Admin: Xóa payment method
     */
    public void deletePaymentMethod(Long id) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found"));
        
        paymentMethodRepository.delete(paymentMethod);
        log.info("Deleted payment method: {} - {}", paymentMethod.getType(), paymentMethod.getName());
    }
    
    /**
     * Admin: Toggle trạng thái active/inactive
     */
    public PaymentMethodResponseDto togglePaymentMethodStatus(Long id) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found"));
        
        paymentMethod.setIsActive(!paymentMethod.getIsActive());
        PaymentMethod updatedPaymentMethod = paymentMethodRepository.save(paymentMethod);
        
        log.info("Toggled payment method status: {} - {} -> {}", 
                updatedPaymentMethod.getType(), 
                updatedPaymentMethod.getName(), 
                updatedPaymentMethod.getIsActive() ? "ACTIVE" : "INACTIVE");
        
        return PaymentMethodResponseDto.fromEntity(updatedPaymentMethod);
    }
    
    /**
     * Lấy payment method by ID
     */
    public PaymentMethodResponseDto getPaymentMethodById(Long id) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found"));
        
        return PaymentMethodResponseDto.fromEntity(paymentMethod);
    }
    
    /**
     * Tính toán fee cho transaction
     */
    public BigDecimal calculateFee(Long paymentMethodId, BigDecimal amount) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .orElseThrow(() -> new RuntimeException("Payment method not found"));
        
        BigDecimal fee = BigDecimal.ZERO;
        
        // Tính fee theo phần trăm
        if (paymentMethod.getFeePercent() != null && paymentMethod.getFeePercent().compareTo(BigDecimal.ZERO) > 0) {
            fee = fee.add(amount.multiply(paymentMethod.getFeePercent()).divide(BigDecimal.valueOf(100)));
        }
        
        // Cộng thêm fee cố định
        if (paymentMethod.getFeeFixed() != null && paymentMethod.getFeeFixed().compareTo(BigDecimal.ZERO) > 0) {
            fee = fee.add(paymentMethod.getFeeFixed());
        }
        
        return fee;
    }
}