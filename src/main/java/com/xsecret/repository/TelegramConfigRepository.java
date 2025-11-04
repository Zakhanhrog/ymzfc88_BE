package com.xsecret.repository;

import com.xsecret.entity.TelegramConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelegramConfigRepository extends JpaRepository<TelegramConfig, Long> {
    
    Optional<TelegramConfig> findFirstByEnabledTrueOrderByCreatedAtDesc();
    
    Optional<TelegramConfig> findFirstByOrderByCreatedAtDesc();
}
