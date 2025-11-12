package com.xsecret.service;

import com.xsecret.entity.User;
import com.xsecret.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralBackfillService {

    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public int backfillReferralCodes() {
        List<User> users = userRepository.findAll();
        int updated = 0;
        for (User user : users) {
            if (user.getReferralCode() == null || user.getReferralCode().isBlank()) {
                user.setReferralCode(userService.generateUniqueReferralCode());
                userRepository.save(user);
                updated++;
            }
        }
        log.info("Generated referral codes for {} users", updated);
        return updated;
    }
}

