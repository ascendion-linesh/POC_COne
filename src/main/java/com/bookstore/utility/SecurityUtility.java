package com.bookstore.utility;

import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Security Utility Class
 * 
 * CRITICAL REMEDIATIONS:
 * 1. ✅ REMOVED hard-coded SALT (was "salt" - CRITICAL vulnerability)
 * 2. ✅ Implemented BCryptPasswordEncoder with automatic salt generation
 * 3. ✅ Added SecureRandom for cryptographically secure random generation
 * 4. ✅ Password encoding now uses industry-standard BCrypt
 * 
 * Security Improvements:
 * - BCrypt automatically generates unique salt for each password
 * - Salt is stored with the hash (no separate storage needed)
 * - Configurable work factor (strength = 12)
 * - Resistant to rainbow table attacks
 * 
 * @author Security Team
 * @version 2.0 (Remediated)
 */
@Component
public class SecurityUtility {

    private static final int BCRYPT_STRENGTH = 12;
    
    /**
     * Password Encoder using BCrypt
     * 
     * REMEDIATION: Replaces hard-coded salt with BCrypt's automatic salt generation
     * BCrypt generates a unique salt for each password automatically
     * 
     * @return BCryptPasswordEncoder with strength 12
     */
    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    /**
     * Secure Random Number Generator
     * 
     * @return SecureRandom instance
     */
    @Bean
    public static SecureRandom secureRandom() {
        return new SecureRandom();
    }

    /**
     * Generate Random Password
     * 
     * @param length desired password length
     * @return random password string
     */
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = secureRandom();
        StringBuilder password = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }

    /**
     * Generate Random Token
     * 
     * @return random token string
     */
    public static String generateRandomToken() {
        SecureRandom random = secureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        
        StringBuilder token = new StringBuilder();
        for (byte b : tokenBytes) {
            token.append(String.format("%02x", b));
        }
        
        return token.toString();
    }
}