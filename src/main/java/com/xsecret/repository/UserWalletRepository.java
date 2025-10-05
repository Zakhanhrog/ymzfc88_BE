package com.xsecret.repository;

import com.xsecret.entity.User;
import com.xsecret.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, Long> {
    
    Optional<UserWallet> findByUser(User user);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uw FROM UserWallet uw WHERE uw.user = :user")
    Optional<UserWallet> findByUserWithLock(@Param("user") User user);
    
    @Query("SELECT uw FROM UserWallet uw WHERE uw.user.id = :userId")
    Optional<UserWallet> findByUserId(@Param("userId") Long userId);
}