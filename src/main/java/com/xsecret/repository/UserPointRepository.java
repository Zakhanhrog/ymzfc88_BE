package com.xsecret.repository;

import com.xsecret.entity.UserPoint;
import com.xsecret.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    
    Optional<UserPoint> findByUser(User user);
    
    Optional<UserPoint> findByUserId(Long userId);
    
    @Query("SELECT up FROM UserPoint up WHERE up.user.id = :userId")
    Optional<UserPoint> findByUserIdWithQuery(@Param("userId") Long userId);
}