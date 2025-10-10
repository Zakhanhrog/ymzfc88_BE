package com.xsecret.service;

import com.xsecret.dto.request.BettingOddsRequest;
import com.xsecret.dto.response.BettingOddsResponse;
import com.xsecret.entity.BettingOdds;
import com.xsecret.repository.BettingOddsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BettingOddsService {

    private final BettingOddsRepository bettingOddsRepository;

    /**
     * Lấy tất cả tỷ lệ cược
     */
    @Transactional(readOnly = true)
    public List<BettingOddsResponse> getAllBettingOdds() {
        log.debug("Getting all betting odds");
        return bettingOddsRepository.findAll().stream()
                .map(BettingOddsResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tỷ lệ cược theo region
     */
    @Transactional(readOnly = true)
    public List<BettingOddsResponse> getBettingOddsByRegion(String region) {
        log.debug("Getting betting odds for region: {}", region);
        return bettingOddsRepository.findByRegion(region).stream()
                .map(BettingOddsResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tỷ lệ cược đang active theo region (cho user)
     */
    @Transactional(readOnly = true)
    public List<BettingOddsResponse> getActiveBettingOddsByRegion(String region) {
        log.debug("Getting active betting odds for region: {}", region);
        return bettingOddsRepository.findByRegionAndIsActive(region, true).stream()
                .map(BettingOddsResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tỷ lệ cược theo ID
     */
    @Transactional(readOnly = true)
    public BettingOddsResponse getBettingOddsById(Long id) {
        log.debug("Getting betting odds by id: {}", id);
        BettingOdds bettingOdds = bettingOddsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tỷ lệ cược với ID: " + id));
        return BettingOddsResponse.fromEntity(bettingOdds);
    }

    /**
     * Tạo mới tỷ lệ cược
     */
    @Transactional
    public BettingOddsResponse createBettingOdds(BettingOddsRequest request) {
        log.info("Creating new betting odds for region: {}, betType: {}", request.getRegion(), request.getBetType());

        // Kiểm tra đã tồn tại chưa
        if (bettingOddsRepository.existsByRegionAndBetType(request.getRegion(), request.getBetType())) {
            throw new RuntimeException("Tỷ lệ cược đã tồn tại cho region " + request.getRegion() + " và betType " + request.getBetType());
        }

        BettingOdds bettingOdds = BettingOdds.builder()
                .region(request.getRegion())
                .betType(request.getBetType())
                .betName(request.getBetName())
                .description(request.getDescription())
                .odds(request.getOdds())
                .pricePerPoint(request.getPricePerPoint())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        BettingOdds savedBettingOdds = bettingOddsRepository.save(bettingOdds);
        log.info("Successfully created betting odds with ID: {}", savedBettingOdds.getId());
        
        return BettingOddsResponse.fromEntity(savedBettingOdds);
    }

    /**
     * Cập nhật tỷ lệ cược
     */
    @Transactional
    public BettingOddsResponse updateBettingOdds(Long id, BettingOddsRequest request) {
        log.info("Updating betting odds with ID: {}", id);

        BettingOdds bettingOdds = bettingOddsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tỷ lệ cược với ID: " + id));

        // Cập nhật các trường
        bettingOdds.setBetName(request.getBetName());
        bettingOdds.setDescription(request.getDescription());
        bettingOdds.setOdds(request.getOdds());
        bettingOdds.setPricePerPoint(request.getPricePerPoint());
        
        if (request.getIsActive() != null) {
            bettingOdds.setIsActive(request.getIsActive());
        }

        BettingOdds updatedBettingOdds = bettingOddsRepository.save(bettingOdds);
        log.info("Successfully updated betting odds with ID: {}", id);
        
        return BettingOddsResponse.fromEntity(updatedBettingOdds);
    }

    /**
     * Cập nhật hàng loạt tỷ lệ cược
     */
    @Transactional
    public List<BettingOddsResponse> batchUpdateBettingOdds(List<BettingOddsRequest> requests) {
        log.info("Batch updating {} betting odds", requests.size());
        
        List<BettingOdds> updatedOdds = requests.stream().map(request -> {
            BettingOdds bettingOdds = bettingOddsRepository
                    .findByRegionAndBetType(request.getRegion(), request.getBetType())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy tỷ lệ cược cho region " + request.getRegion() + " và betType " + request.getBetType()));

            bettingOdds.setBetName(request.getBetName());
            bettingOdds.setDescription(request.getDescription());
            bettingOdds.setOdds(request.getOdds());
            bettingOdds.setPricePerPoint(request.getPricePerPoint());
            
            if (request.getIsActive() != null) {
                bettingOdds.setIsActive(request.getIsActive());
            }

            return bettingOdds;
        }).collect(Collectors.toList());

        List<BettingOdds> savedOdds = bettingOddsRepository.saveAll(updatedOdds);
        log.info("Successfully batch updated {} betting odds", savedOdds.size());
        
        return savedOdds.stream()
                .map(BettingOddsResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Xóa tỷ lệ cược
     */
    @Transactional
    public void deleteBettingOdds(Long id) {
        log.info("Deleting betting odds with ID: {}", id);
        
        BettingOdds bettingOdds = bettingOddsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tỷ lệ cược với ID: " + id));
        
        bettingOddsRepository.delete(bettingOdds);
        log.info("Successfully deleted betting odds with ID: {}", id);
    }
}

