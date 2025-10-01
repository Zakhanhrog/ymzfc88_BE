package com.xsecret.config;

import com.xsecret.entity.User;
import com.xsecret.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-username}")
    private String defaultAdminUsername;

    @Value("${app.admin.default-password}")
    private String defaultAdminPassword;

    @Value("${app.admin.default-email}")
    private String defaultAdminEmail;

    @Override
    public void run(String... args) throws Exception {
        initializeAdminUser();
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
}
