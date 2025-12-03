package com.xsecret.repository;

import com.xsecret.entity.StreamConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StreamConfigRepository extends JpaRepository<StreamConfig, Long> {
    
    Optional<StreamConfig> findByGameTypeAndTableNumber(StreamConfig.GameType gameType, Integer tableNumber);
    
    Optional<StreamConfig> findByGameTypeAndTableNumberIsNull(StreamConfig.GameType gameType);
    
    List<StreamConfig> findByGameType(StreamConfig.GameType gameType);
    
    List<StreamConfig> findByIsActiveTrue();
    
    List<StreamConfig> findByGameTypeAndIsActiveTrue(StreamConfig.GameType gameType);
    
    Optional<StreamConfig> findByStreamKey(String streamKey);
}

