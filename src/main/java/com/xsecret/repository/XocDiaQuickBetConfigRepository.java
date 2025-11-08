package com.xsecret.repository;

import com.xsecret.entity.XocDiaQuickBetConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface XocDiaQuickBetConfigRepository extends JpaRepository<XocDiaQuickBetConfig, Long> {

    Optional<XocDiaQuickBetConfig> findByCode(String code);

    boolean existsByCode(String code);

    List<XocDiaQuickBetConfig> findAllByIsActiveTrueOrderByDisplayOrderAsc();

    List<XocDiaQuickBetConfig> findAllByCodeIn(Iterable<String> codes);

    List<XocDiaQuickBetConfig> findAllByOrderByDisplayOrderAsc();
}


