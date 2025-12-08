package com.xsecret.repository;

import com.xsecret.entity.User;
import com.xsecret.entity.UserLoginHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLoginHistoryRepository extends JpaRepository<UserLoginHistory, Long>, JpaSpecificationExecutor<UserLoginHistory> {
    
    /**
     * Lấy IP lần đầu tiên đăng nhập thành công của user
     */
    @Query("""
        SELECT h.ipAddress FROM UserLoginHistory h
        WHERE h.user = :user
          AND h.success = true
          AND h.ipAddress IS NOT NULL
        ORDER BY h.loginAt ASC
    """)
    List<String> findFirstLoginIpByUser(@Param("user") User user, Pageable pageable);
}


