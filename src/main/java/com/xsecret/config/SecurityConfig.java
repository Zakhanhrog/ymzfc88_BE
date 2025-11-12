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
import org.springframework.http.HttpMethod;
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
                    // Allow OPTIONS requests for CORS preflight
                    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                    // Public endpoints
                    .requestMatchers("/auth/**").permitAll()
                    .requestMatchers("/admin/login").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers("/uploads/**").permitAll()
                    .requestMatchers("/files/**").permitAll()
                    .requestMatchers("/betting-odds/**").permitAll() // Public betting odds for users
                    .requestMatchers("/xoc-dia/quick-bets/**").permitAll() // Public quick bets for Xóc Đĩa
                    .requestMatchers("/sicbo/quick-bets/**").permitAll() // Public quick bets for Sicbo
                    .requestMatchers("/xoc-dia/session/**").permitAll() // Public session state for Xóc Đĩa
                    .requestMatchers("/sicbo/session/**").permitAll() // Public session state for Sicbo
                    .requestMatchers("/sicbo/result-history/**").permitAll() // Public result history for Sicbo
                    .requestMatchers("/xoc-dia/result-history/**").permitAll() // Public result history for stats
                    .requestMatchers("/xoc-dia/bets/**").hasAnyRole("USER", "ADMIN") // Đặt cược Xóc Đĩa cần đăng nhập
                    .requestMatchers("/sicbo/bets/**").hasAnyRole("USER", "ADMIN") // Đặt cược Sicbo cần đăng nhập
                    .requestMatchers("/public/**").permitAll() // Public endpoints (lottery results, etc.)
                    .requestMatchers("/marquee-notifications/public/**").permitAll() // Public marquee notifications
                    .requestMatchers("/banners/public/**").permitAll() // Public banners
                    .requestMatchers(HttpMethod.GET, "/promotions/**").permitAll() // Public promotions (GET endpoints only)
                    .requestMatchers("/bets/**").hasAnyRole("USER", "ADMIN") // Betting endpoints for authenticated users
                    .requestMatchers("/test/**").permitAll() // Test endpoints
                    // Admin endpoints (must come after /admin/login and public endpoints)
                    // Note: /admin/promotions will override /promotions/** for admin routes
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/banners/admin/**").hasRole("ADMIN")
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
        // Use setAllowedOriginPatterns instead of setAllowedOrigins when using credentials
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",
            "https://tathiet168.com",
            "http://localhost:8080",
                "https://api.tathiet168.com",
            "https://admin.tathiet168.com",
            "http://localhost:5173",
            "http://localhost:5174"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
