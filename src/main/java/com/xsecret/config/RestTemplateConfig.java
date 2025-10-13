package com.xsecret.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration cho RestTemplate với timeout settings
 */
@Configuration
public class RestTemplateConfig {
    
    /**
     * RestTemplate với timeout configuration
     * - Connect timeout: 5 giây
     * - Read timeout: 10 giây
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}

