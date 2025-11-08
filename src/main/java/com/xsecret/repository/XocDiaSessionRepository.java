package com.xsecret.repository;

import com.xsecret.entity.XocDiaSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface XocDiaSessionRepository extends JpaRepository<XocDiaSession, Long> {

    Optional<XocDiaSession> findTopByOrderByStartedAtDesc();
}

