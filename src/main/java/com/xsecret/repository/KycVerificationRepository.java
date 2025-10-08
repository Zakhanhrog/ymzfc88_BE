package com.xsecret.repository;

import com.xsecret.entity.KycVerification;
import com.xsecret.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {
    
    Optional<KycVerification> findByUser(User user);
    
    Optional<KycVerification> findByUserAndStatus(User user, KycVerification.KycStatus status);
    
    List<KycVerification> findByStatus(KycVerification.KycStatus status);
    
    List<KycVerification> findAllByOrderBySubmittedAtDesc();
    
    boolean existsByUserAndStatus(User user, KycVerification.KycStatus status);
}

