package com.xsecret.repository;

import com.xsecret.entity.UserPaymentMethod;
import com.xsecret.entity.User;
import com.xsecret.entity.PaymentMethod.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPaymentMethodRepository extends JpaRepository<UserPaymentMethod, Long> {
    
    /**
     * Tìm tất cả phương thức thanh toán của một user
     */
    List<UserPaymentMethod> findByUserOrderByIsDefaultDescCreatedAtDesc(User user);
    
    /**
     * Tìm phương thức thanh toán mặc định của user
     */
    Optional<UserPaymentMethod> findByUserAndIsDefaultTrue(User user);
    
    /**
     * Tìm phương thức thanh toán theo user và id
     */
    Optional<UserPaymentMethod> findByUserAndId(User user, Long id);
    
    /**
     * Đếm số lượng phương thức thanh toán của user
     */
    long countByUser(User user);
    
    /**
     * Kiểm tra xem user có phương thức thanh toán nào không
     */
    boolean existsByUser(User user);
    
    /**
     * Tìm phương thức thanh toán theo loại
     */
    List<UserPaymentMethod> findByUserAndType(User user, PaymentType type);
    
    /**
     * Reset tất cả phương thức về không phải mặc định cho user
     */
    @Modifying
    @Query("UPDATE UserPaymentMethod upm SET upm.isDefault = false WHERE upm.user = :user")
    void resetDefaultPaymentMethods(@Param("user") User user);
    
    /**
     * Kiểm tra xem số tài khoản đã tồn tại cho user chưa
     */
    boolean existsByUserAndAccountNumberAndType(User user, String accountNumber, PaymentType type);
    
    /**
     * Tìm phương thức thanh toán đã xác thực của user
     */
    List<UserPaymentMethod> findByUserAndIsVerifiedTrueOrderByIsDefaultDescCreatedAtDesc(User user);
}