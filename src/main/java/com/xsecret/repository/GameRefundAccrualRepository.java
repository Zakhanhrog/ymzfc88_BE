package com.xsecret.repository;

import com.xsecret.entity.GameRefundAccrual;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface GameRefundAccrualRepository extends JpaRepository<GameRefundAccrual, Long> {

    List<GameRefundAccrual> findTop500ByStatusAndPayoutAtLessThanEqualOrderByPayoutAtAsc(
            GameRefundAccrual.Status status,
            Instant payoutAt
    );
}


