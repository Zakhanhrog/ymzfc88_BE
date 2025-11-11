package com.xsecret.repository;

import com.xsecret.entity.SicboSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SicboSessionRepository extends JpaRepository<SicboSession, Long> {

    Optional<SicboSession> findTopByTableNumberOrderByStartedAtDesc(Integer tableNumber);
}


