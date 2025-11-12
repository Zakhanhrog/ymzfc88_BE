package com.xsecret.runner;

import com.xsecret.service.ReferralBackfillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class ReferralCodeSeeder implements CommandLineRunner {

    private final ReferralBackfillService referralBackfillService;

    @Override
    public void run(String... args) throws Exception {
        int updated = referralBackfillService.backfillReferralCodes();
        if (updated > 0) {
            log.info("ReferralCodeSeeder created {} referral codes for existing users", updated);
        } else {
            log.info("ReferralCodeSeeder found no users without referral codes");
        }
    }
}

