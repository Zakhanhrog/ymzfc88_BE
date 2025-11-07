package com.xsecret.service;

import com.xsecret.dto.request.XocDiaQuickBetRequest;
import com.xsecret.dto.response.XocDiaQuickBetResponse;
import com.xsecret.entity.XocDiaQuickBetConfig;
import com.xsecret.repository.XocDiaQuickBetConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class XocDiaQuickBetService {

    private final XocDiaQuickBetConfigRepository repository;

    @Transactional(readOnly = true)
    public List<XocDiaQuickBetResponse> getAllConfigs() {
        return repository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(XocDiaQuickBetResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<XocDiaQuickBetResponse> getActiveConfigs() {
        return repository.findAllByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(XocDiaQuickBetResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public XocDiaQuickBetResponse getById(Long id) {
        XocDiaQuickBetConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình quick bet với ID: " + id));
        return XocDiaQuickBetResponse.fromEntity(config);
    }

    @Transactional
    public XocDiaQuickBetResponse create(XocDiaQuickBetRequest request) {
        if (repository.existsByCode(request.getCode())) {
            throw new RuntimeException("Quick bet với mã " + request.getCode() + " đã tồn tại");
        }

        String normalizedGroup = normalizeGroup(request.getLayoutGroup());

        XocDiaQuickBetConfig config = XocDiaQuickBetConfig.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .payoutMultiplier(request.getPayoutMultiplier())
                .pattern(normalizePattern(request.getPattern()))
                .layoutGroup(normalizedGroup)
                .displayOrder(request.getDisplayOrder())
                .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
                .build();

        XocDiaQuickBetConfig saved = repository.save(config);
        log.info("Đã tạo quick bet mới: {} ({})", saved.getName(), saved.getCode());
        return XocDiaQuickBetResponse.fromEntity(saved);
    }

    @Transactional
    public XocDiaQuickBetResponse update(Long id, XocDiaQuickBetRequest request) {
        XocDiaQuickBetConfig config = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cấu hình quick bet với ID: " + id));

        if (!config.getCode().equals(request.getCode())
                && repository.existsByCode(request.getCode())) {
            throw new RuntimeException("Quick bet với mã " + request.getCode() + " đã tồn tại");
        }

        String normalizedGroup = normalizeGroup(request.getLayoutGroup());

        config.setCode(request.getCode());
        config.setName(request.getName());
        config.setDescription(request.getDescription());
        config.setPayoutMultiplier(request.getPayoutMultiplier());
        config.setPattern(normalizePattern(request.getPattern()));
        config.setLayoutGroup(normalizedGroup);
        config.setDisplayOrder(request.getDisplayOrder());
        if (request.getIsActive() != null) {
            config.setIsActive(request.getIsActive());
        }

        XocDiaQuickBetConfig updated = repository.save(config);
        log.info("Đã cập nhật quick bet: {} ({})", updated.getName(), updated.getCode());
        return XocDiaQuickBetResponse.fromEntity(updated);
    }

    @Transactional
    public List<XocDiaQuickBetResponse> batchUpdate(List<XocDiaQuickBetRequest> requests) {
        List<XocDiaQuickBetConfig> configs = requests.stream()
                .map(request -> {
                    if (request.getId() == null) {
                        throw new RuntimeException("Thiếu ID quick bet khi cập nhật hàng loạt");
                    }
                    XocDiaQuickBetConfig config = repository.findById(request.getId())
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy quick bet với ID: " + request.getId()));

                    if (!config.getCode().equals(request.getCode())
                            && repository.existsByCode(request.getCode())) {
                        throw new RuntimeException("Quick bet với mã " + request.getCode() + " đã tồn tại");
                    }

                    String normalizedGroup = normalizeGroup(request.getLayoutGroup());

                    config.setCode(request.getCode());
                    config.setName(request.getName());
                    config.setDescription(request.getDescription());
                    config.setPayoutMultiplier(request.getPayoutMultiplier());
                    config.setPattern(normalizePattern(request.getPattern()));
                    config.setLayoutGroup(normalizedGroup);
                    config.setDisplayOrder(request.getDisplayOrder());
                    if (request.getIsActive() != null) {
                        config.setIsActive(request.getIsActive());
                    }

                    return config;
                })
                .collect(Collectors.toList());

        List<XocDiaQuickBetConfig> saved = repository.saveAll(configs);
        log.info("Đã cập nhật {} cấu hình quick bet", saved.size());
        return saved.stream()
                .map(XocDiaQuickBetResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy cấu hình quick bet với ID: " + id);
        }
        repository.deleteById(id);
        log.info("Đã xoá quick bet với ID: {}", id);
    }

    private String normalizeGroup(String input) {
        if (input == null || input.isBlank()) {
            return XocDiaQuickBetConfig.GROUP_TOP;
        }
        return input.trim().toUpperCase();
    }

    private String normalizePattern(String pattern) {
        if (pattern == null) {
            return null;
        }
        String trimmed = pattern.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.replaceAll("\\s+", "");
    }
}


