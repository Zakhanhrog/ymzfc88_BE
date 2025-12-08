package com.xsecret.repository;

import com.xsecret.entity.AgentCommissionPayout;
import com.xsecret.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface AgentCommissionPayoutRepository extends JpaRepository<AgentCommissionPayout, Long> {

    Page<AgentCommissionPayout> findByAgentOrderByPeriodStartDesc(User agent, Pageable pageable);

    Page<AgentCommissionPayout> findByAgentAndStatusOrderByPeriodStartDesc(
            User agent,
            AgentCommissionPayout.Status status,
            Pageable pageable
    );

    java.util.Optional<AgentCommissionPayout> findByAgentAndPeriodMonth(User agent, String periodMonth);

    java.util.List<AgentCommissionPayout> findByPeriodMonth(String periodMonth);
    
    // Lấy tất cả payouts của agent trong tháng
    java.util.List<AgentCommissionPayout> findByAgentAndPeriodMonthOrderByPaidAtDesc(User agent, String periodMonth);
    
    // Tính tổng hoa hồng đã chia (PAID) của agent trong tháng
    @Query("SELECT COALESCE(SUM(acp.commissionAmount), 0) FROM AgentCommissionPayout acp " +
           "WHERE acp.agent = :agent AND acp.periodMonth = :periodMonth " +
           "AND acp.status = com.xsecret.entity.AgentCommissionPayout$Status.PAID")
    BigDecimal sumPaidCommissionByAgentAndMonth(
            @Param("agent") User agent,
            @Param("periodMonth") String periodMonth
    );
    
    // Tính tổng hoa hồng đại lý đã trả (PAID) theo date range
    @Query("SELECT COALESCE(SUM(acp.commissionAmount), 0) FROM AgentCommissionPayout acp " +
           "WHERE acp.status = com.xsecret.entity.AgentCommissionPayout$Status.PAID " +
           "AND (:start IS NULL OR acp.paidAt >= :start) " +
           "AND (:end IS NULL OR acp.paidAt <= :end)")
    BigDecimal sumPaidCommissionByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}

