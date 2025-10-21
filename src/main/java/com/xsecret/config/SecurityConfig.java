package com.xsecret.config;

import com.xsecret.security.JwtAuthenticationFilter;
import com.xsecret.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                    // Public endpoints
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/admin/login").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers("/uploads/**").permitAll()
                    .requestMatchers("/files/**").permitAll()
                    .requestMatchers("/betting-odds/**").permitAll() // Public betting odds for users
                    .requestMatchers("/public/**").permitAll() // Public endpoints (lottery results, etc.)
                    .requestMatchers("/banners/public/**").permitAll() // Public banner endpoints
                    .requestMatchers("/banners/**").hasRole("ADMIN") // Banner management endpoints for admin only
                    .requestMatchers("/bets/**").hasAnyRole("USER", "ADMIN") // Betting endpoints for authenticated users
                    .requestMatchers("/test/**").permitAll() // Test endpoints
                    // Admin endpoints
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/kyc/admin/**").hasRole("ADMIN")
                    // User endpoints
                    .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                    .requestMatchers("/kyc/**").hasAnyRole("USER", "ADMIN")
                    .requestMatchers("/wallet/**").hasAnyRole("USER", "ADMIN")
                    .requestMatchers("/transactions/**").hasAnyRole("USER", "ADMIN")
                    .requestMatchers("/user-payment-methods/**").hasAnyRole("USER", "ADMIN")
                    .requestMatchers("/points/**").hasAnyRole("USER", "ADMIN")
                    // All other requests need authentication
                    .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // For H2 Console
        http.headers(headers -> headers.frameOptions().deny());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:5173", 
        
            "http://localhost:8080",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:8080",
                "https://loto79.online",
                "https://api.loto79.online"

        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
