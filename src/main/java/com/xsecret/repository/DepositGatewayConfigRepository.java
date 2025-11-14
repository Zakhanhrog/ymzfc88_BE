package com.xsecret.repository;

import com.xsecret.entity.DepositGatewayConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DepositGatewayConfigRepository extends JpaRepository<DepositGatewayConfig, Long> {

    boolean existsByBankCodeIgnoreCase(String bankCode);

    boolean existsByChannelCodeIgnoreCase(String channelCode);

    Optional<DepositGatewayConfig> findByBankCodeIgnoreCase(String bankCode);

    Optional<DepositGatewayConfig> findByChannelCodeIgnoreCase(String channelCode);

    @Query("""
            SELECT c FROM DepositGatewayConfig c
            WHERE (:keyword IS NULL OR :keyword = '' OR
                   LOWER(c.bankCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(c.bankName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(c.channelCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:active IS NULL OR c.active = :active)
            """)
    Page<DepositGatewayConfig> search(
            @Param("keyword") String keyword,
            @Param("active") Boolean active,
            Pageable pageable
    );
}

