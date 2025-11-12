package com.xsecret.repository;

import com.xsecret.entity.SicboResultHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SicboResultHistoryRepository extends JpaRepository<SicboResultHistory, Long> {

    @Query("select h from SicboResultHistory h where (:tableNumber is null or h.tableNumber = :tableNumber) order by h.recordedAt desc")
    List<SicboResultHistory> findLatest(Integer tableNumber, Pageable pageable);
}


