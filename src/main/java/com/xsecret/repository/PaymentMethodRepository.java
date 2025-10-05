package com.xsecret.repository;

import com.xsecret.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    
    List<PaymentMethod> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    List<PaymentMethod> findByTypeAndIsActiveTrueOrderByDisplayOrderAsc(PaymentMethod.PaymentType type);
    
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.isActive = true ORDER BY pm.displayOrder ASC")
    List<PaymentMethod> findActivePaymentMethods();
    
    // Check if payment method exists by account number and type
    boolean existsByAccountNumberAndType(String accountNumber, PaymentMethod.PaymentType type);
    
    // Find by account number
    Optional<PaymentMethod> findByAccountNumber(String accountNumber);
    
    // Get max display order
    @Query("SELECT COALESCE(MAX(pm.displayOrder), 0) FROM PaymentMethod pm")
    Integer getMaxDisplayOrder();
}