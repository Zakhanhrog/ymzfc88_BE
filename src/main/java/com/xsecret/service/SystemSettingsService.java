package com.xsecret.service;

import com.xsecret.dto.request.SystemSettingsRequest;
import com.xsecret.dto.response.SystemSettingsResponse;
import com.xsecret.entity.SystemSettings;
import com.xsecret.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingsService {

    private final SystemSettingsRepository systemSettingsRepository;

    /**
     * Lấy giá trị setting theo key
     */
    @Transactional(readOnly = true)
    public String getSettingValue(String key) {
        log.debug("Getting setting value for key: {}", key);
        String value = systemSettingsRepository.findBySettingKey(key)
                .map(SystemSettings::getSettingValue)
                .orElse(null);
        log.debug("Setting value for key {}: {}", key, value);
        return value;
    }

    /**
     * Lấy giá trị setting theo key với default value
     */
    @Transactional(readOnly = true)
    public String getSettingValue(String key, String defaultValue) {
        log.debug("Getting setting value for key: {} with default: {}", key, defaultValue);
        String value = systemSettingsRepository.findBySettingKey(key)
                .map(SystemSettings::getSettingValue)
                .orElse(defaultValue);
        log.debug("Setting value for key {}: {}", key, value);
        return value;
    }

    /**
     * Lấy tất cả settings
     */
    @Transactional(readOnly = true)
    public List<SystemSettingsResponse> getAllSettings() {
        return systemSettingsRepository.findAll().stream()
                .map(SystemSettingsResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy settings theo category
     */
    @Transactional(readOnly = true)
    public List<SystemSettingsResponse> getSettingsByCategory(String category) {
        return systemSettingsRepository.findByCategory(category).stream()
                .map(SystemSettingsResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy setting theo key
     */
    @Transactional(readOnly = true)
    public SystemSettingsResponse getSettingByKey(String key) {
        SystemSettings settings = systemSettingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + key));
        return SystemSettingsResponse.fromEntity(settings);
    }

    /**
     * Tạo hoặc cập nhật setting
     */
    @Transactional
    public SystemSettingsResponse createOrUpdateSetting(SystemSettingsRequest request) {
        log.info("Creating or updating setting: {} with value: {}", request.getSettingKey(), request.getSettingValue());

        SystemSettings settings = systemSettingsRepository.findBySettingKey(request.getSettingKey())
                .orElse(SystemSettings.builder()
                        .settingKey(request.getSettingKey())
                        .build());

        settings.setSettingValue(request.getSettingValue());
        settings.setDescription(request.getDescription());
        settings.setCategory(request.getCategory());

        SystemSettings savedSettings = systemSettingsRepository.save(settings);
        systemSettingsRepository.flush(); // Force flush to database
        log.info("Successfully saved setting: {} with value: {}", savedSettings.getSettingKey(), savedSettings.getSettingValue());
        return SystemSettingsResponse.fromEntity(savedSettings);
    }

    /**
     * Xóa setting
     */
    public void deleteSetting(String key) {
        log.info("Deleting setting: {}", key);
        SystemSettings settings = systemSettingsRepository.findBySettingKey(key)
                .orElseThrow(() -> new RuntimeException("Setting not found: " + key));
        systemSettingsRepository.delete(settings);
    }

    /**
     * Lấy lý do khóa rút tiền mặc định
     */
    @Transactional(readOnly = true)
    public String getDefaultWithdrawalLockReason() {
        log.info("Getting default withdrawal lock reason from database...");
        String reason = getSettingValue(
                SystemSettings.DEFAULT_WITHDRAWAL_LOCK_REASON,
                "Tài khoản của bạn đã bị khóa rút tiền do vi phạm chính sách của hệ thống. Vui lòng liên hệ admin để được hỗ trợ."
        );
        log.info("Default withdrawal lock reason: {}", reason);
        return reason;
    }

    /**
     * Lấy tất cả contact links
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, String> getContactLinks() {
        java.util.Map<String, String> contactLinks = new java.util.HashMap<>();
        contactLinks.put("livechat", getSettingValue(SystemSettings.CONTACT_LIVECHAT_LINK, "#"));
        contactLinks.put("facebook", getSettingValue(SystemSettings.CONTACT_FACEBOOK_LINK, "#"));
        contactLinks.put("messenger", getSettingValue(SystemSettings.CONTACT_MESSENGER_LINK, "#"));
        contactLinks.put("telegram", getSettingValue(SystemSettings.CONTACT_TELEGRAM_LINK, "#"));
        contactLinks.put("hotline", getSettingValue(SystemSettings.CONTACT_HOTLINE_LINK, "#"));
        return contactLinks;
    }

    /**
     * Lấy tỷ lệ hoa hồng mặc định cho đại lý
     */
    @Transactional(readOnly = true)
    public double getAgentCommissionPercentage() {
        String value = getSettingValue(SystemSettings.AGENT_COMMISSION_PERCENTAGE, "5");
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            log.warn("Invalid agent commission percentage value: {}. Falling back to default 5%", value);
            return 5.0;
        }
    }

    /**
     * Cập nhật contact link
     */
    @Transactional
    public SystemSettingsResponse updateContactLink(String linkType, String linkUrl) {
        String settingKey;
        String description;
        
        switch (linkType.toLowerCase()) {
            case "livechat":
                settingKey = SystemSettings.CONTACT_LIVECHAT_LINK;
                description = "Link cho thẻ Livechat 24/24";
                break;
            case "facebook":
                settingKey = SystemSettings.CONTACT_FACEBOOK_LINK;
                description = "Link cho thẻ Kênh Facebook";
                break;
            case "messenger":
                settingKey = SystemSettings.CONTACT_MESSENGER_LINK;
                description = "Link cho thẻ Messenger Facebook";
                break;
            case "telegram":
                settingKey = SystemSettings.CONTACT_TELEGRAM_LINK;
                description = "Link cho thẻ Telegram";
                break;
            case "hotline":
                settingKey = SystemSettings.CONTACT_HOTLINE_LINK;
                description = "Link cho thẻ Hotline";
                break;
            default:
                throw new RuntimeException("Invalid contact link type: " + linkType);
        }
        
        return createOrUpdateSetting(SystemSettingsRequest.builder()
                .settingKey(settingKey)
                .settingValue(linkUrl)
                .description(description)
                .category("CONTACT")
                .build());
    }

    /**
     * Khởi tạo các settings mặc định
     */
    @Transactional
    public void initializeDefaultSettings() {
        log.info("Initializing default system settings");

        // Default withdrawal lock reason
        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.DEFAULT_WITHDRAWAL_LOCK_REASON)) {
            createOrUpdateSetting(SystemSettingsRequest.builder()
                    .settingKey(SystemSettings.DEFAULT_WITHDRAWAL_LOCK_REASON)
                    .settingValue("Tài khoản của bạn đã bị khóa rút tiền do vi phạm chính sách của hệ thống. Vui lòng liên hệ admin để được hỗ trợ.")
                    .description("Lý do mặc định khi khóa rút tiền của người dùng")
                    .category("WITHDRAWAL")
                    .build());
        }

        // System maintenance message
        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.SYSTEM_MAINTENANCE_MESSAGE)) {
            createOrUpdateSetting(SystemSettingsRequest.builder()
                    .settingKey(SystemSettings.SYSTEM_MAINTENANCE_MESSAGE)
                    .settingValue("Hệ thống đang bảo trì. Vui lòng quay lại sau.")
                    .description("Thông báo khi hệ thống bảo trì")
                    .category("SYSTEM")
                    .build());
        }

        // Min/Max amounts
        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.MIN_WITHDRAWAL_AMOUNT)) {
            createOrUpdateSetting(SystemSettingsRequest.builder()
                    .settingKey(SystemSettings.MIN_WITHDRAWAL_AMOUNT)
                    .settingValue("50000")
                    .description("Số tiền rút tối thiểu")
                    .category("WITHDRAWAL")
                    .build());
        }

        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.MAX_WITHDRAWAL_AMOUNT)) {
            createOrUpdateSetting(SystemSettingsRequest.builder()
                    .settingKey(SystemSettings.MAX_WITHDRAWAL_AMOUNT)
                    .settingValue("50000000")
                    .description("Số tiền rút tối đa")
                    .category("WITHDRAWAL")
                    .build());
        }

        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.MIN_DEPOSIT_AMOUNT)) {
            createOrUpdateSetting(SystemSettingsRequest.builder()
                    .settingKey(SystemSettings.MIN_DEPOSIT_AMOUNT)
                    .settingValue("50000")
                    .description("Số tiền nạp tối thiểu")
                    .category("DEPOSIT")
                    .build());
        }

        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.MAX_DEPOSIT_AMOUNT)) {
            createOrUpdateSetting(SystemSettingsRequest.builder()
                    .settingKey(SystemSettings.MAX_DEPOSIT_AMOUNT)
                    .settingValue("100000000")
                    .description("Số tiền nạp tối đa")
                    .category("DEPOSIT")
                    .build());
        }

        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.AGENT_COMMISSION_PERCENTAGE)) {
            createOrUpdateSetting(SystemSettingsRequest.builder()
                    .settingKey(SystemSettings.AGENT_COMMISSION_PERCENTAGE)
                    .settingValue("5")
                    .description("Tỷ lệ hoa hồng mặc định cho đại lý (đơn vị %)")
                    .category("COMMISSION")
                    .build());
        }

        // Contact page links
        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.CONTACT_LIVECHAT_LINK)) {
            createOrUpdateSetting(SystemSettingsRequest.builder()
                    .settingKey(SystemSettings.CONTACT_LIVECHAT_LINK)
                    .settingValue("#")
                    .description("Link cho thẻ Livechat 24/24")
                    .category("CONTACT")
                    .build());
        }

        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.CONTACT_FACEBOOK_LINK)) {
            createOrUpdateSetting(SystemSettingsRequest.builder()
                    .settingKey(SystemSettings.CONTACT_FACEBOOK_LINK)
                    .settingValue("#")
                    .description("Link cho thẻ Kênh Facebook")
                    .category("CONTACT")
                    .build());
        }

        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.CONTACT_MESSENGER_LINK)) {
            createOrUpdateSetting(SystemSettingsRequest.builder()
                    .settingKey(SystemSettings.CONTACT_MESSENGER_LINK)
                    .settingValue("#")
                    .description("Link cho thẻ Messenger Facebook")
                    .category("CONTACT")
                    .build());
        }

        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.CONTACT_TELEGRAM_LINK)) {
            createOrUpdateSetting(SystemSettingsRequest.builder()
                    .settingKey(SystemSettings.CONTACT_TELEGRAM_LINK)
                    .settingValue("#")
                    .description("Link cho thẻ Telegram")
                    .category("CONTACT")
                    .build());
        }

        if (!systemSettingsRepository.existsBySettingKey(SystemSettings.CONTACT_HOTLINE_LINK)) {
            createOrUpdateSetting(SystemSettingsRequest.builder()
                    .settingKey(SystemSettings.CONTACT_HOTLINE_LINK)
                    .settingValue("#")
                    .description("Link cho thẻ Hotline")
                    .category("CONTACT")
                    .build());
        }
    }
}

