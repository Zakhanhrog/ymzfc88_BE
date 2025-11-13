package com.xsecret.repository;

import com.xsecret.entity.AgentCommissionPayout;
import com.xsecret.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentCommissionPayoutRepository extends JpaRepository<AgentCommissionPayout, Long> {

    Page<AgentCommissionPayout> findByAgentOrderByPeriodStartDesc(User agent, Pageable pageable);

    Page<AgentCommissionPayout> findByAgentAndStatusOrderByPeriodStartDesc(
            User agent,
            AgentCommissionPayout.Status status,
            Pageable pageable
    );

    java.util.Optional<AgentCommissionPayout> findByAgentAndPeriodMonth(User agent, String periodMonth);

    java.util.List<AgentCommissionPayout> findByPeriodMonth(String periodMonth);
}

