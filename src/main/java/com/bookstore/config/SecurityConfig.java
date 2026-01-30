package com.bookstore.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.bookstore.service.impl.UserSecurityService;

import java.util.Arrays;

/**
 * Spring Security Configuration
 * 
 * CRITICAL REMEDIATIONS:
 * 1. ✅ CSRF PROTECTION ENABLED (was disabled - CRITICAL vulnerability fixed)
 * 2. ✅ Migrated from deprecated WebSecurityConfigurerAdapter to SecurityFilterChain
 * 3. ✅ Added comprehensive security headers
 * 4. ✅ Proper CORS configuration instead of disabling
 * 5. ✅ BCrypt password encoder with automatic salt generation
 * 6. ✅ Spring Security 6 compatible configuration
 * 
 * Security Features:
 * - CSRF protection with cookie-based token repository
 * - X-Frame-Options: DENY (Clickjacking protection)
 * - X-Content-Type-Options: nosniff (MIME-sniffing protection)
 * - X-XSS-Protection: 1; mode=block
 * - Strict-Transport-Security (HSTS)
 * - Content-Security-Policy
 * - Referrer-Policy: strict-origin-when-cross-origin
 * 
 * @author Security Team
 * @version 2.0 (Remediated - Spring Security 6)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private UserSecurityService userSecurityService;

    private static final String[] PUBLIC_MATCHERS = {
        "/css/**",
        "/js/**",
        "/image/**",
        "/",
        "/myAccount",
        "/newUser",
        "/forgetPassword",
        "/login",
        "/fonts/**",
        "/bookshelf",
        "/bookDetail/**",
        "/hours",
        "/faq",
        "/searchByCategory",
        "/searchBook"
    };

    /**
     * Password Encoder Bean
     * Uses BCrypt with automatic salt generation (strength 12)
     * REMEDIATION: Replaces hard-coded salt vulnerability
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Authentication Provider Configuration
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userSecurityService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication Manager Bean
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * CORS Configuration
     * REMEDIATION: Proper CORS configuration instead of disabling
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Main Security Filter Chain
     * 
     * CRITICAL REMEDIATIONS:
     * 1. ✅ CSRF PROTECTION ENABLED (was disabled - CRITICAL FIX)
     * 2. ✅ Migrated to SecurityFilterChain pattern (Spring Security 6)
     * 3. ✅ Added comprehensive security headers
     * 4. ✅ Proper CORS configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ✅ CRITICAL FIX: CSRF Protection ENABLED
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            
            // Proper CORS Configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Authorization Rules
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(PUBLIC_MATCHERS).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            
            // Form Login Configuration
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            
            // Logout Configuration
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            
            // Security Headers Configuration
            .headers(headers -> headers
                // X-Frame-Options: DENY (Clickjacking protection)
                .frameOptions(frame -> frame.deny())
                
                // X-Content-Type-Options: nosniff
                .contentTypeOptions(contentType -> {})
                
                // X-XSS-Protection: 1; mode=block
                .xssProtection(xss -> xss
                    .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                )
                
                // Strict-Transport-Security (HSTS)
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
                
                // Content-Security-Policy
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                                    "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                    "style-src 'self' 'unsafe-inline'; " +
                                    "img-src 'self' data: https:; " +
                                    "font-src 'self' data:; " +
                                    "connect-src 'self'")
                )
                
                // Referrer-Policy
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            );

        http.authenticationProvider(authenticationProvider());

        return http.build();
    }
}