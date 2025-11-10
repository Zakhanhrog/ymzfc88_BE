package com.xsecret.service;

import com.xsecret.dto.request.SicboQuickBetRequest;
import com.xsecret.dto.response.SicboQuickBetResponse;
import com.xsecret.entity.SicboQuickBetConfig;
import com.xsecret.repository.SicboQuickBetConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SicboQuickBetService {

    private final SicboQuickBetConfigRepository repository;

    @Transactional(readOnly = true)
    public List<SicboQuickBetResponse> getAllConfigs() {
        return repository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(SicboQuickBetResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SicboQuickBetResponse> getActiveConfigs() {
        return repository.findAllByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(SicboQuickBetResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SicboQuickBetResponse getById(Long id) {
        SicboQuickBetConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình quick bet với ID: " + id));
        return SicboQuickBetResponse.fromEntity(config);
    }

    @Transactional
    public SicboQuickBetResponse create(SicboQuickBetRequest request) {
        if (repository.existsByCode(request.getCode())) {
            throw new RuntimeException("Quick bet với mã " + request.getCode() + " đã tồn tại");
        }

        SicboQuickBetConfig config = SicboQuickBetConfig.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .payoutMultiplier(request.getPayoutMultiplier())
                .layoutGroup(normalizeGroup(request.getLayoutGroup()))
                .displayOrder(request.getDisplayOrder())
                .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
                .build();

        SicboQuickBetConfig saved = repository.save(config);
        log.info("Đã tạo quick bet Sicbo mới: {} ({})", saved.getName(), saved.getCode());
        return SicboQuickBetResponse.fromEntity(saved);
    }

    @Transactional
    public SicboQuickBetResponse update(Long id, SicboQuickBetRequest request) {
        SicboQuickBetConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình quick bet với ID: " + id));

        if (!config.getCode().equals(request.getCode())
                && repository.existsByCode(request.getCode())) {
            throw new RuntimeException("Quick bet với mã " + request.getCode() + " đã tồn tại");
        }

        config.setCode(request.getCode());
        config.setName(request.getName());
        config.setDescription(request.getDescription());
        config.setPayoutMultiplier(request.getPayoutMultiplier());
        config.setLayoutGroup(normalizeGroup(request.getLayoutGroup()));
        config.setDisplayOrder(request.getDisplayOrder());
        if (request.getIsActive() != null) {
            config.setIsActive(request.getIsActive());
        }

        SicboQuickBetConfig updated = repository.save(config);
        log.info("Đã cập nhật quick bet Sicbo: {} ({})", updated.getName(), updated.getCode());
        return SicboQuickBetResponse.fromEntity(updated);
    }

    @Transactional
    public List<SicboQuickBetResponse> batchUpdate(List<SicboQuickBetRequest> requests) {
        List<SicboQuickBetConfig> configs = requests.stream()
                .map(request -> {
                    if (request.getId() == null) {
                        throw new RuntimeException("Thiếu ID quick bet khi cập nhật hàng loạt");
                    }
                    SicboQuickBetConfig config = repository.findById(request.getId())
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy quick bet với ID: " + request.getId()));

                    if (!config.getCode().equals(request.getCode())
                            && repository.existsByCode(request.getCode())) {
                        throw new RuntimeException("Quick bet với mã " + request.getCode() + " đã tồn tại");
                    }

                    config.setCode(request.getCode());
                    config.setName(request.getName());
                    config.setDescription(request.getDescription());
                    config.setPayoutMultiplier(request.getPayoutMultiplier());
                    config.setLayoutGroup(normalizeGroup(request.getLayoutGroup()));
                    config.setDisplayOrder(request.getDisplayOrder());
                    if (request.getIsActive() != null) {
                        config.setIsActive(request.getIsActive());
                    }

                    return config;
                })
                .collect(Collectors.toList());

        List<SicboQuickBetConfig> saved = repository.saveAll(configs);
        log.info("Đã cập nhật {} cấu hình quick bet Sicbo", saved.size());
        return saved.stream()
                .map(SicboQuickBetResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy cấu hình quick bet với ID: " + id);
        }
        repository.deleteById(id);
        log.info("Đã xoá quick bet Sicbo với ID: {}", id);
    }

    private String normalizeGroup(String input) {
        if (input == null || input.isBlank()) {
            return SicboQuickBetConfig.GROUP_PRIMARY;
        }
        return input.trim().toUpperCase();
    }
}


