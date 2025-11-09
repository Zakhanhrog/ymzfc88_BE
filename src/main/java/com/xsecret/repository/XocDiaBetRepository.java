package com.xsecret.repository;

import com.xsecret.entity.User;
import com.xsecret.entity.XocDiaBet;
import com.xsecret.entity.XocDiaSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface XocDiaBetRepository extends JpaRepository<XocDiaBet, Long> {

    List<XocDiaBet> findBySessionAndStatus(XocDiaSession session, XocDiaBet.Status status);

    List<XocDiaBet> findBySessionAndStatusIn(XocDiaSession session, Collection<XocDiaBet.Status> statuses);

    Page<XocDiaBet> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}

