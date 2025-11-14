package com.xsecret.repository;

import com.xsecret.entity.User;
import com.xsecret.entity.XocDiaBet;
import com.xsecret.entity.XocDiaSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface XocDiaBetRepository extends JpaRepository<XocDiaBet, Long> {

    List<XocDiaBet> findBySessionAndStatus(XocDiaSession session, XocDiaBet.Status status);

    List<XocDiaBet> findBySessionAndStatusIn(XocDiaSession session, Collection<XocDiaBet.Status> statuses);

    Page<XocDiaBet> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<XocDiaBet> findByStatusAndSettledAtBetween(XocDiaBet.Status status, Instant start, Instant end);

    List<XocDiaBet> findByStatusInAndSettledAtBetween(Collection<XocDiaBet.Status> statuses, Instant start, Instant end);

    @Query("SELECT COALESCE(SUM(b.stake), 0) FROM XocDiaBet b WHERE b.status = :status AND b.settledAt BETWEEN :start AND :end")
    BigDecimal sumStakeByStatusAndSettledAtBetween(@Param("status") XocDiaBet.Status status,
                                                   @Param("start") Instant start,
                                                   @Param("end") Instant end);

    @Query("SELECT b FROM XocDiaBet b JOIN FETCH b.user WHERE b.status IN :statuses AND b.settledAt IS NOT NULL ORDER BY b.settledAt DESC")
    List<XocDiaBet> findRecentSettledBetsByStatuses(@Param("statuses") Collection<XocDiaBet.Status> statuses,
                                                    Pageable pageable);

    @Query("SELECT b FROM XocDiaBet b WHERE (:status IS NULL OR b.status = :status) AND (:start IS NULL OR b.settledAt >= :start) AND (:end IS NULL OR b.settledAt <= :end)")
    Page<XocDiaBet> findForAnalytics(@Param("status") XocDiaBet.Status status,
                                     @Param("start") Instant start,
                                     @Param("end") Instant end,
                                     Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.stake), 0) FROM XocDiaBet b WHERE (:status IS NULL OR b.status = :status) AND (:start IS NULL OR b.settledAt >= :start) AND (:end IS NULL OR b.settledAt <= :end)")
    BigDecimal sumStakeByFilters(@Param("status") XocDiaBet.Status status,
                                 @Param("start") Instant start,
                                 @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(b.winAmount), 0) FROM XocDiaBet b WHERE (:status IS NULL OR b.status = :status) AND (:start IS NULL OR b.settledAt >= :start) AND (:end IS NULL OR b.settledAt <= :end)")
    BigDecimal sumWinAmountByFilters(@Param("status") XocDiaBet.Status status,
                                     @Param("start") Instant start,
                                     @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(b.stake), 0) FROM XocDiaBet b WHERE b.status IN :statuses AND (:start IS NULL OR b.settledAt >= :start) AND (:end IS NULL OR b.settledAt <= :end)")
    BigDecimal sumStakeByStatusesAndDate(@Param("statuses") Collection<XocDiaBet.Status> statuses,
                                         @Param("start") Instant start,
                                         @Param("end") Instant end);

    @Query("""
        SELECT b.user.id,
               COALESCE(SUM(CASE WHEN b.status <> com.xsecret.entity.XocDiaBet$Status.REFUNDED THEN b.stake ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN b.status = com.xsecret.entity.XocDiaBet$Status.LOST THEN b.stake ELSE 0 END), 0)
        FROM XocDiaBet b
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
        SELECT b FROM XocDiaBet b
        WHERE b.user.id = :userId
          AND (:start IS NULL OR b.createdAt >= :start)
          AND (:end IS NULL OR b.createdAt <= :end)
        ORDER BY b.createdAt DESC
    """)
    List<XocDiaBet> findRecentBetsByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );

    @Query("""
        SELECT b FROM XocDiaBet b
        WHERE (:status IS NULL OR b.status = :status)
          AND (:start IS NULL OR b.createdAt >= :start)
          AND (:end IS NULL OR b.createdAt <= :end)
        ORDER BY b.createdAt DESC
    """)
    Page<XocDiaBet> findAdminHistory(
            @Param("status") XocDiaBet.Status status,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );

    @Query("""
        SELECT COALESCE(SUM(b.stake), 0) FROM XocDiaBet b
        WHERE (:status IS NULL OR b.status = :status)
          AND (:start IS NULL OR b.createdAt >= :start)
          AND (:end IS NULL OR b.createdAt <= :end)
    """)
    BigDecimal sumStakeByCreatedAtFilters(
            @Param("status") XocDiaBet.Status status,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
        SELECT COALESCE(SUM(b.winAmount), 0) FROM XocDiaBet b
        WHERE (:status IS NULL OR b.status = :status)
          AND (:start IS NULL OR b.createdAt >= :start)
          AND (:end IS NULL OR b.createdAt <= :end)
    """)
    BigDecimal sumWinAmountByCreatedAtFilters(
            @Param("status") XocDiaBet.Status status,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("""
        SELECT COALESCE(SUM(b.winAmount), 0) FROM XocDiaBet b
        WHERE b.user = :user AND b.status = com.xsecret.entity.XocDiaBet$Status.WON
    """)
    BigDecimal sumWinAmountByUser(@Param("user") User user);

    @Query("""
        SELECT COALESCE(SUM(b.stake), 0) FROM XocDiaBet b
        WHERE b.user = :user AND b.status = com.xsecret.entity.XocDiaBet$Status.LOST
    """)
    BigDecimal sumLostStakeByUser(@Param("user") User user);
}

