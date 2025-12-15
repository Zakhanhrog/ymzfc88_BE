package com.xsecret.repository;

import com.xsecret.entity.GameRefundAccrual;
import com.xsecret.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface GameRefundAccrualRepository extends JpaRepository<GameRefundAccrual, Long> {

    List<GameRefundAccrual> findTop500ByStatusAndPayoutAtLessThanEqualOrderByPayoutAtAsc(
            GameRefundAccrual.Status status,
            Instant payoutAt
    );
    
    // Lấy tổng refund amount theo user, gameType và sessionId
    @Query("SELECT COALESCE(SUM(gra.amount), 0) FROM GameRefundAccrual gra " +
           "WHERE gra.user = :user AND gra.gameType = :gameType AND gra.referenceSessionId = :sessionId")
    BigDecimal sumRefundByUserAndGameTypeAndSession(
            @Param("user") User user,
            @Param("gameType") GameRefundAccrual.GameType gameType,
            @Param("sessionId") Long sessionId
    );
    
    // Lấy danh sách refund theo user và list sessionIds
    @Query("SELECT gra FROM GameRefundAccrual gra " +
           "WHERE gra.user = :user AND gra.gameType = :gameType AND gra.referenceSessionId IN :sessionIds")
    List<GameRefundAccrual> findByUserAndGameTypeAndSessionIds(
            @Param("user") User user,
            @Param("gameType") GameRefundAccrual.GameType gameType,
            @Param("sessionIds") List<Long> sessionIds
    );
    
    // Tính tổng hoàn trả đã trả (PAID) theo date range
    @Query("SELECT COALESCE(SUM(gra.amount), 0) FROM GameRefundAccrual gra " +
           "WHERE gra.status = com.xsecret.entity.GameRefundAccrual$Status.PAID " +
           "AND (:start IS NULL OR gra.paidAt >= :start) " +
           "AND (:end IS NULL OR gra.paidAt <= :end)")
    BigDecimal sumPaidRefundByDateRange(
            @Param("start") Instant start,
            @Param("end") Instant end
    );
    
    // Tính tổng hoàn trả đã trả (PAID) theo user
    @Query("SELECT COALESCE(SUM(gra.amount), 0) FROM GameRefundAccrual gra " +
           "WHERE gra.user = :user AND gra.status = com.xsecret.entity.GameRefundAccrual$Status.PAID")
    BigDecimal sumPaidRefundByUser(@Param("user") User user);
    
    // Tính tổng hoàn trả đã trả (PAID) theo user - CHỈ tính cho lệnh cược thua
    @Query("SELECT COALESCE(SUM(gra.amount), 0) FROM GameRefundAccrual gra " +
           "WHERE gra.user = :user " +
           "AND gra.status = com.xsecret.entity.GameRefundAccrual$Status.PAID " +
           "AND gra.description LIKE '%(thua)%'")
    BigDecimal sumPaidLossRefundByUser(@Param("user") User user);

    // Tính tổng hoàn trả đã trả (PAID) theo list users và date range
    @Query("SELECT COALESCE(SUM(gra.amount), 0) FROM GameRefundAccrual gra " +
           "WHERE gra.user IN :users " +
           "AND gra.status = com.xsecret.entity.GameRefundAccrual$Status.PAID " +
           "AND (:start IS NULL OR gra.paidAt >= :start) " +
           "AND (:end IS NULL OR gra.paidAt <= :end)")
    BigDecimal sumPaidRefundByUsersAndDateRange(
            @Param("users") List<User> users,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}


