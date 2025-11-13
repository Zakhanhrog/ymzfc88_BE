package com.xsecret.repository;

import com.xsecret.entity.SicboBet;
import com.xsecret.entity.SicboSession;
import com.xsecret.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface SicboBetRepository extends JpaRepository<SicboBet, Long> {

    List<SicboBet> findBySessionAndStatus(SicboSession session, SicboBet.Status status);

    List<SicboBet> findBySessionAndStatusIn(SicboSession session, Collection<SicboBet.Status> statuses);

    List<SicboBet> findByUserOrderByCreatedAtDesc(User user);

    Page<SicboBet> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<SicboBet> findByStatusAndSettledAtBetween(SicboBet.Status status, Instant start, Instant end);

    List<SicboBet> findByStatusInAndSettledAtBetween(Collection<SicboBet.Status> statuses, Instant start, Instant end);

    @Query("SELECT COALESCE(SUM(b.stake), 0) FROM SicboBet b WHERE b.status = :status AND b.settledAt BETWEEN :start AND :end")
    BigDecimal sumStakeByStatusAndSettledAtBetween(@Param("status") SicboBet.Status status,
                                                   @Param("start") Instant start,
                                                   @Param("end") Instant end);

    @Query("SELECT b FROM SicboBet b JOIN FETCH b.user WHERE b.status IN :statuses AND b.settledAt IS NOT NULL ORDER BY b.settledAt DESC")
    List<SicboBet> findRecentSettledBetsByStatuses(@Param("statuses") Collection<SicboBet.Status> statuses,
                                                   Pageable pageable);

    @Query("SELECT b FROM SicboBet b WHERE (:status IS NULL OR b.status = :status) AND (:start IS NULL OR b.settledAt >= :start) AND (:end IS NULL OR b.settledAt <= :end)")
    Page<SicboBet> findForAnalytics(@Param("status") SicboBet.Status status,
                                    @Param("start") Instant start,
                                    @Param("end") Instant end,
                                    Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.stake), 0) FROM SicboBet b WHERE (:status IS NULL OR b.status = :status) AND (:start IS NULL OR b.settledAt >= :start) AND (:end IS NULL OR b.settledAt <= :end)")
    BigDecimal sumStakeByFilters(@Param("status") SicboBet.Status status,
                                 @Param("start") Instant start,
                                 @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(b.winAmount), 0) FROM SicboBet b WHERE (:status IS NULL OR b.status = :status) AND (:start IS NULL OR b.settledAt >= :start) AND (:end IS NULL OR b.settledAt <= :end)")
    BigDecimal sumWinAmountByFilters(@Param("status") SicboBet.Status status,
                                     @Param("start") Instant start,
                                     @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(b.stake), 0) FROM SicboBet b WHERE b.status IN :statuses AND (:start IS NULL OR b.settledAt >= :start) AND (:end IS NULL OR b.settledAt <= :end)")
    BigDecimal sumStakeByStatusesAndDate(@Param("statuses") Collection<SicboBet.Status> statuses,
                                         @Param("start") Instant start,
                                         @Param("end") Instant end);

    @Query("""
        SELECT b.user.id,
               COALESCE(SUM(CASE WHEN b.status <> com.xsecret.entity.SicboBet$Status.REFUNDED THEN b.stake ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN b.status = com.xsecret.entity.SicboBet$Status.LOST THEN b.stake ELSE 0 END), 0)
        FROM SicboBet b
        WHERE b.user.id IN :userIds
          AND (:start IS NULL OR b.createdAt >= :start)
          AND (:end IS NULL OR b.createdAt <= :end)
        GROUP BY b.user.id
    """)
    List<Object[]> aggregateTotalsByUsers(
            @Param("userIds") List<Long> userIds,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
        SELECT b FROM SicboBet b
        WHERE b.user.id = :userId
          AND (:start IS NULL OR b.createdAt >= :start)
          AND (:end IS NULL OR b.createdAt <= :end)
        ORDER BY b.createdAt DESC
    """)
    List<SicboBet> findRecentBetsByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );

    @Query("""
        SELECT b FROM SicboBet b
        WHERE (:status IS NULL OR b.status = :status)
          AND (:start IS NULL OR b.createdAt >= :start)
          AND (:end IS NULL OR b.createdAt <= :end)
        ORDER BY b.createdAt DESC
    """)
    Page<SicboBet> findAdminHistory(
            @Param("status") SicboBet.Status status,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );

    @Query("""
        SELECT COALESCE(SUM(b.stake), 0) FROM SicboBet b
        WHERE (:status IS NULL OR b.status = :status)
          AND (:start IS NULL OR b.createdAt >= :start)
          AND (:end IS NULL OR b.createdAt <= :end)
    """)
    BigDecimal sumStakeByCreatedAtFilters(
            @Param("status") SicboBet.Status status,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
        SELECT COALESCE(SUM(b.winAmount), 0) FROM SicboBet b
        WHERE (:status IS NULL OR b.status = :status)
          AND (:start IS NULL OR b.createdAt >= :start)
          AND (:end IS NULL OR b.createdAt <= :end)
    """)
    BigDecimal sumWinAmountByCreatedAtFilters(
            @Param("status") SicboBet.Status status,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}


