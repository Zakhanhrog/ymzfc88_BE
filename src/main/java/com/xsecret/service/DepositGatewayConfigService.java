package com.xsecret.service;

import com.xsecret.dto.request.DepositGatewayConfigRequest;
import com.xsecret.dto.response.DepositGatewayConfigResponse;
import com.xsecret.entity.DepositGatewayConfig;
import com.xsecret.repository.DepositGatewayConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepositGatewayConfigService {

    private final DepositGatewayConfigRepository repository;

    @Transactional(readOnly = true)
    public Page<DepositGatewayConfigResponse> searchConfigs(String keyword, Boolean active, Pageable pageable) {
        Page<DepositGatewayConfig> configs = repository.search(
                keyword != null ? keyword.trim() : null,
                active,
                pageable
        );
        return configs.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public DepositGatewayConfigResponse getConfig(Long id) {
        DepositGatewayConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cấu hình cổng nạp"));
        return mapToResponse(config);
    }

    @Transactional
    public DepositGatewayConfigResponse createConfig(DepositGatewayConfigRequest request) {
        validateUniqueCodes(request, null);

        DepositGatewayConfig config = DepositGatewayConfig.builder()
                .bankCode(request.getBankCode().trim().toUpperCase())
                .bankName(request.getBankName().trim())
                .channelCode(request.getChannelCode().trim())
                .merchantId(request.getMerchantId().trim())
                .apiKey(request.getApiKey().trim())
                .notifyUrl(trimNullable(request.getNotifyUrl()))
                .returnUrl(trimNullable(request.getReturnUrl()))
                .active(request.getActive() == null || request.getActive())
                .priorityOrder(request.getPriorityOrder())
                .minAmount(request.getMinAmount())
                .maxAmount(request.getMaxAmount())
                .description(trimNullable(request.getDescription()))
                .build();

        DepositGatewayConfig saved = repository.save(config);
        log.info("Created deposit gateway config for bankCode={}, channelCode={}", saved.getBankCode(), saved.getChannelCode());
        return mapToResponse(saved);
    }

    @Transactional
    public DepositGatewayConfigResponse updateConfig(Long id, DepositGatewayConfigRequest request) {
        DepositGatewayConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cấu hình cổng nạp"));

        validateUniqueCodes(request, id);

        config.setBankCode(request.getBankCode().trim().toUpperCase());
        config.setBankName(request.getBankName().trim());
        config.setChannelCode(request.getChannelCode().trim());
        config.setMerchantId(request.getMerchantId().trim());
        config.setApiKey(request.getApiKey().trim());
        config.setNotifyUrl(trimNullable(request.getNotifyUrl()));
        config.setReturnUrl(trimNullable(request.getReturnUrl()));
        if (request.getActive() != null) {
            config.setActive(request.getActive());
        }
        config.setPriorityOrder(request.getPriorityOrder());
        config.setMinAmount(request.getMinAmount());
        config.setMaxAmount(request.getMaxAmount());
        config.setDescription(trimNullable(request.getDescription()));

        DepositGatewayConfig updated = repository.save(config);
        log.info("Updated deposit gateway config id={}", id);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteConfig(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy cấu hình cổng nạp");
        }
        repository.deleteById(id);
        log.info("Deleted deposit gateway config id={}", id);
    }

    @Transactional
    public DepositGatewayConfigResponse toggleStatus(Long id) {
        DepositGatewayConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cấu hình cổng nạp"));
        config.setActive(!Boolean.TRUE.equals(config.getActive()));
        DepositGatewayConfig updated = repository.save(config);
        log.info("Toggled deposit gateway config status id={}, active={}", id, updated.getActive());
        return mapToResponse(updated);
    }

    private void validateUniqueCodes(DepositGatewayConfigRequest request, Long currentId) {
        String bankCode = request.getBankCode().trim().toUpperCase();
        repository.findByBankCodeIgnoreCase(bankCode).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new IllegalArgumentException("Mã ngân hàng đã tồn tại");
            }
        });

        String channelCode = request.getChannelCode().trim();
        repository.findByChannelCodeIgnoreCase(channelCode)
                .ifPresent(existing -> {
                    if (currentId == null || !existing.getId().equals(currentId)) {
                        throw new IllegalArgumentException("Mã kênh đã tồn tại");
                    }
                });
    }

    private DepositGatewayConfigResponse mapToResponse(DepositGatewayConfig config) {
        return DepositGatewayConfigResponse.builder()
                .id(config.getId())
                .bankCode(config.getBankCode())
                .bankName(config.getBankName())
                .channelCode(config.getChannelCode())
                .merchantId(config.getMerchantId())
                .apiKey(config.getApiKey())
                .notifyUrl(config.getNotifyUrl())
                .returnUrl(config.getReturnUrl())
                .active(config.getActive())
                .priorityOrder(config.getPriorityOrder())
                .minAmount(config.getMinAmount())
                .maxAmount(config.getMaxAmount())
                .description(config.getDescription())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private String trimNullable(String value) {
        return value == null ? null : value.trim();
    }
}

