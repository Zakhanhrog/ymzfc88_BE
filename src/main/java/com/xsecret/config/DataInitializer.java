package com.xsecret.config;

import com.xsecret.entity.PaymentMethod;
import com.xsecret.entity.User;
import com.xsecret.repository.PaymentMethodRepository;
import com.xsecret.repository.UserRepository;
import com.xsecret.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemSettingsService systemSettingsService;

    @Value("${app.admin.default-username}")
    private String defaultAdminUsername;

    @Value("${app.admin.default-password}")
    private String defaultAdminPassword;

    @Value("${app.admin.default-email}")
    private String defaultAdminEmail;

    @Override
    public void run(String... args) throws Exception {
        initializeAdminUser();
        initializePaymentMethods();
        initializeSystemSettings();
    }

    private void initializeAdminUser() {
        if (!userRepository.existsByUsername(defaultAdminUsername)) {
            User admin = User.builder()
                    .username(defaultAdminUsername)
                    .email(defaultAdminEmail)
                    .password(passwordEncoder.encode(defaultAdminPassword))
                    .fullName("Administrator")
                    .role(User.Role.ADMIN)
                    .status(User.UserStatus.ACTIVE)
                    .balance(0.0)
                    .build();

            userRepository.save(admin);
            log.info("Default admin user created: {}", defaultAdminUsername);
        } else {
            log.info("Admin user already exists: {}", defaultAdminUsername);
        }
    }
    
    private void initializePaymentMethods() {
        if (paymentMethodRepository.count() == 0) {
            // MoMo Payment Method
            PaymentMethod momo = PaymentMethod.builder()
                    .type(PaymentMethod.PaymentType.MOMO)
                    .name("Ví MoMo")
                    .accountNumber("0987654321")
                    .accountName("ADMIN XSECRET")
                    .minAmount(BigDecimal.valueOf(10000))
                    .maxAmount(BigDecimal.valueOf(50000000))
                    .feePercent(BigDecimal.ZERO)
                    .feeFixed(BigDecimal.ZERO)
                    .processingTime("5-15 phút")
                    .isActive(true)
                    .displayOrder(1)
                    .description("Nạp tiền qua ví MoMo")
                    .build();
            
            // Bank Transfer Payment Method
            PaymentMethod bank = PaymentMethod.builder()
                    .type(PaymentMethod.PaymentType.BANK)
                    .name("Chuyển khoản ngân hàng")
                    .accountNumber("1234567890")
                    .accountName("ADMIN XSECRET")
                    .bankCode("VCB")
                    .minAmount(BigDecimal.valueOf(50000))
                    .maxAmount(BigDecimal.valueOf(100000000))
                    .feePercent(BigDecimal.ZERO)
                    .feeFixed(BigDecimal.ZERO)
                    .processingTime("10-30 phút")
                    .isActive(true)
                    .displayOrder(2)
                    .description("Chuyển khoản qua ngân hàng Vietcombank")
                    .build();
            
            // USDT Payment Method
            PaymentMethod usdt = PaymentMethod.builder()
                    .type(PaymentMethod.PaymentType.USDT)
                    .name("USDT TRC-20")
                    .accountNumber("TRX_WALLET_ADDRESS_HERE")
                    .accountName("ADMIN XSECRET")
                    .minAmount(BigDecimal.valueOf(100000))
                    .maxAmount(BigDecimal.valueOf(200000000))
                    .feePercent(BigDecimal.valueOf(1))
                    .feeFixed(BigDecimal.ZERO)
                    .processingTime("15-60 phút")
                    .isActive(true)
                    .displayOrder(3)
                    .description("Nạp tiền bằng USDT qua mạng TRC-20")
                    .build();
            
            paymentMethodRepository.save(momo);
            paymentMethodRepository.save(bank);
            paymentMethodRepository.save(usdt);
            
            log.info("Default payment methods created: MoMo, Bank Transfer, USDT");
        } else {
            log.info("Payment methods already exist, count: {}", paymentMethodRepository.count());
        }
    }

    private void initializeSystemSettings() {
        log.info("Initializing system settings...");
        systemSettingsService.initializeDefaultSettings();
        log.info("System settings initialization completed");
    }
}
