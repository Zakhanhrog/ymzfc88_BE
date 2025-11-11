package com.xsecret.repository;

import com.xsecret.entity.SicboBet;
import com.xsecret.entity.SicboSession;
import com.xsecret.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SicboBetRepository extends JpaRepository<SicboBet, Long> {

    List<SicboBet> findBySessionAndStatus(SicboSession session, SicboBet.Status status);

    List<SicboBet> findBySessionAndStatusIn(SicboSession session, Collection<SicboBet.Status> statuses);

    List<SicboBet> findByUserOrderByCreatedAtDesc(User user);
}


