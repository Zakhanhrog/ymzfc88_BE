package com.xsecret.repository;

import com.xsecret.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {
    
    List<Banner> findByIsActiveTrueAndBannerTypeOrderByDisplayOrderAsc(Banner.BannerType bannerType);
    
    List<Banner> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    List<Banner> findByBannerTypeOrderByDisplayOrderAsc(Banner.BannerType bannerType);
}
