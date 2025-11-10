package com.xsecret.repository;

import com.xsecret.entity.SicboQuickBetConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SicboQuickBetConfigRepository extends JpaRepository<SicboQuickBetConfig, Long> {

    Optional<SicboQuickBetConfig> findByCode(String code);

    boolean existsByCode(String code);

    List<SicboQuickBetConfig> findAllByIsActiveTrueOrderByDisplayOrderAsc();

    List<SicboQuickBetConfig> findAllByOrderByDisplayOrderAsc();
}


