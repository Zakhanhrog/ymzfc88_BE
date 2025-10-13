package com.xsecret.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsecret.dto.request.LotteryResultRequest;
import com.xsecret.dto.response.LotteryResultResponse;
import com.xsecret.entity.LotteryResult;
import com.xsecret.repository.LotteryResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsecret.event.LotteryResultPublishedEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LotteryResultService {
    
    private final LotteryResultRepository lotteryResultRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Tạo kết quả xổ số mới
     */
    @Transactional
    public LotteryResultResponse createLotteryResult(LotteryResultRequest request) {
        log.info("Creating lottery result: region={}, province={}, drawDate={}", 
                request.getRegion(), request.getProvince(), request.getDrawDate());

        // Validate region
        if (!"mienBac".equals(request.getRegion()) && !"mienTrungNam".equals(request.getRegion())) {
            throw new RuntimeException("Region không hợp lệ. Chỉ chấp nhận: mienBac, mienTrungNam");
        }

        // Validate province
        if ("mienBac".equals(request.getRegion()) && request.getProvince() != null) {
            throw new RuntimeException("Miền Bắc không có province, phải để null");
        }
        if ("mienTrungNam".equals(request.getRegion()) && request.getProvince() == null) {
            throw new RuntimeException("Miền Trung Nam phải có province (gialai, binhduong, ninhthuan, travinh, vinhlong)");
        }

        // Parse drawDate
        LocalDate drawDate = LocalDate.parse(request.getDrawDate(), DateTimeFormatter.ISO_LOCAL_DATE);

        // Kiểm tra đã tồn tại chưa
        lotteryResultRepository.findByRegionAndProvinceAndDrawDate(
            request.getRegion(), 
            request.getProvince(), 
            drawDate
        ).ifPresent(existing -> {
            throw new RuntimeException("Kết quả cho ngày này đã tồn tại. ID: " + existing.getId());
        });

        // Validate JSON results
        validateResults(request.getResults(), request.getRegion());

        // Tạo entity
        LotteryResult.ResultStatus status = LotteryResult.ResultStatus.DRAFT;
        if (request.getStatus() != null) {
            try {
                status = LotteryResult.ResultStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}, using DRAFT", request.getStatus());
            }
        }

        LotteryResult entity = LotteryResult.builder()
                .region(request.getRegion())
                .province(request.getProvince())
                .drawDate(drawDate)
                .results(request.getResults())
                .status(status)
                .build();

        LotteryResult saved = lotteryResultRepository.save(entity);
        log.info("Lottery result created with ID: {}", saved.getId());

        return LotteryResultResponse.fromEntity(saved);
    }

    /**
     * Cập nhật kết quả xổ số
     */
    @Transactional
    public LotteryResultResponse updateLotteryResult(Long id, LotteryResultRequest request) {
        log.info("Updating lottery result ID: {}", id);

        LotteryResult entity = lotteryResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả với ID: " + id));

        // Cập nhật drawDate nếu có
        if (request.getDrawDate() != null && !request.getDrawDate().trim().isEmpty()) {
            try {
                LocalDate newDrawDate = LocalDate.parse(request.getDrawDate(), DateTimeFormatter.ISO_LOCAL_DATE);
                entity.setDrawDate(newDrawDate);
                log.info("Updated drawDate to: {}", newDrawDate);
            } catch (Exception e) {
                log.warn("Invalid drawDate format: {}, keeping current drawDate", request.getDrawDate());
            }
        }

        // Cập nhật region nếu có
        if (request.getRegion() != null && !request.getRegion().trim().isEmpty()) {
            entity.setRegion(request.getRegion());
        }

        // Cập nhật province nếu có
        if (request.getProvince() != null) {
            entity.setProvince(request.getProvince());
        }

        // Validate JSON results nếu có cập nhật
        if (request.getResults() != null && !request.getResults().trim().isEmpty()) {
            validateResults(request.getResults(), entity.getRegion());
            entity.setResults(request.getResults());
        }

        // Cập nhật status nếu có
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                LotteryResult.ResultStatus status = LotteryResult.ResultStatus.valueOf(request.getStatus().toUpperCase());
                entity.setStatus(status);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}, keeping current status", request.getStatus());
            }
        }

        LotteryResult saved = lotteryResultRepository.save(entity);
        log.info("Lottery result updated: ID={}, status={}, drawDate={}", 
                saved.getId(), saved.getStatus(), saved.getDrawDate());

        return LotteryResultResponse.fromEntity(saved);
    }

    /**
     * Xóa kết quả xổ số
     */
    @Transactional
    public void deleteLotteryResult(Long id) {
        log.info("Deleting lottery result ID: {}", id);

        LotteryResult entity = lotteryResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả với ID: " + id));

        lotteryResultRepository.delete(entity);
        log.info("Lottery result deleted: ID={}", id);
    }

    /**
     * Lấy kết quả theo ID
     */
    @Transactional(readOnly = true)
    public LotteryResultResponse getLotteryResult(Long id) {
        LotteryResult entity = lotteryResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả với ID: " + id));
        return LotteryResultResponse.fromEntity(entity);
    }

    /**
     * Lấy tất cả kết quả với phân trang
     */
    @Transactional(readOnly = true)
    public Page<LotteryResultResponse> getAllLotteryResults(Pageable pageable) {
        return lotteryResultRepository.findAll(pageable)
                .map(LotteryResultResponse::fromEntity);
    }

    /**
     * Lấy kết quả theo region
     */
    @Transactional(readOnly = true)
    public Page<LotteryResultResponse> getLotteryResultsByRegion(String region, Pageable pageable) {
        return lotteryResultRepository.findByRegionOrderByDrawDateDesc(region, pageable)
                .map(LotteryResultResponse::fromEntity);
    }

    /**
     * Lấy kết quả theo region và province
     */
    @Transactional(readOnly = true)
    public Page<LotteryResultResponse> getLotteryResultsByRegionAndProvince(
            String region, String province, Pageable pageable) {
        if (province == null || province.trim().isEmpty()) {
            return lotteryResultRepository.findAllMienBacResults(pageable)
                    .map(LotteryResultResponse::fromEntity);
        }
        return lotteryResultRepository.findByRegionAndProvinceOrderByDrawDateDesc(region, province, pageable)
                .map(LotteryResultResponse::fromEntity);
    }

    /**
     * Lấy kết quả theo province (bỏ qua region)
     */
    @Transactional(readOnly = true)
    public Page<LotteryResultResponse> getLotteryResultsByProvince(String province, Pageable pageable) {
        return lotteryResultRepository.findByProvinceOrderByDrawDateDesc(province, pageable)
                .map(LotteryResultResponse::fromEntity);
    }

    /**
     * Lấy kết quả đã published để check bet
     * Đây là method quan trọng nhất - dùng để check kết quả cược
     */
    @Transactional(readOnly = true)
    public LotteryResult getPublishedResultForBetCheck(String region, String province, LocalDate drawDate) {
        return lotteryResultRepository.findPublishedResult(region, province, drawDate)
                .orElse(null);
    }

    /**
     * Lấy kết quả mới nhất đã published (dùng khi không chỉ định ngày)
     */
    @Transactional(readOnly = true)
    public LotteryResult getLatestPublishedResult(String region, String province) {
        return lotteryResultRepository.findLatestPublishedResult(region, province)
                .orElse(null);
    }

    /**
     * Publish kết quả (chuyển từ DRAFT sang PUBLISHED)
     */
    @Transactional
    public LotteryResultResponse publishResult(Long id) {
        log.info("Publishing lottery result ID: {}", id);

        LotteryResult entity = lotteryResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả với ID: " + id));

        entity.setStatus(LotteryResult.ResultStatus.PUBLISHED);
        LotteryResult saved = lotteryResultRepository.save(entity);

        log.info("Lottery result published: ID={}, region={}, province={}, drawDate={}", 
                saved.getId(), saved.getRegion(), saved.getProvince(), saved.getDrawDate());
        
        // Publish event để trigger auto bet check
        eventPublisher.publishEvent(new LotteryResultPublishedEvent(
                saved.getId(), 
                saved.getRegion(), 
                saved.getProvince(), 
                saved.getDrawDate().toString()));
        
        return LotteryResultResponse.fromEntity(saved);
    }

    /**
     * Unpublish kết quả (chuyển từ PUBLISHED về DRAFT)
     */
    @Transactional
    public LotteryResultResponse unpublishResult(Long id) {
        log.info("Unpublishing lottery result ID: {}", id);

        LotteryResult entity = lotteryResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả với ID: " + id));

        entity.setStatus(LotteryResult.ResultStatus.DRAFT);
        LotteryResult saved = lotteryResultRepository.save(entity);

        log.info("Lottery result unpublished: ID={}", saved.getId());
        return LotteryResultResponse.fromEntity(saved);
    }


    /**
     * Validate JSON results format
     */
    private void validateResults(String results, String region) {
        try {
            Map<String, Object> resultsMap = objectMapper.readValue(results, Map.class);

            if ("mienBac".equals(region)) {
                // Miền Bắc phải có 7 giải
                String[] requiredPrizes = {"dac-biet", "giai-nhat", "giai-nhi", "giai-ba", "giai-tu", "giai-nam", "giai-sau", "giai-bay"};
                for (String prize : requiredPrizes) {
                    if (!resultsMap.containsKey(prize)) {
                        throw new RuntimeException("Miền Bắc thiếu giải: " + prize);
                    }
                }
            } else if ("mienTrungNam".equals(region)) {
                // Miền Trung Nam phải có 8 giải
                String[] requiredPrizes = {"dac-biet", "giai-nhat", "giai-nhi", "giai-ba", "giai-tu", "giai-nam", "giai-sau", "giai-bay", "giai-tam"};
                for (String prize : requiredPrizes) {
                    if (!resultsMap.containsKey(prize)) {
                        throw new RuntimeException("Miền Trung Nam thiếu giải: " + prize);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("JSON results không hợp lệ: " + e.getMessage());
        }
    }
}

