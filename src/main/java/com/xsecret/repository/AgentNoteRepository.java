package com.xsecret.repository;

import com.xsecret.entity.AgentNote;
import com.xsecret.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentNoteRepository extends JpaRepository<AgentNote, Long> {
    Optional<AgentNote> findByAgentAndPeriodMonth(User agent, String periodMonth);
}

