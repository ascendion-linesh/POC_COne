package com.bookstore.utility;

import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecurityUtility {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Bean
    public static BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12, SECURE_RANDOM);
    }

    /**
     * Generates a secure random salt for password hashing
     * @return byte array containing random salt
     */
    public static byte[] generateSalt() {
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * Generates a random string for tokens
     * @return random string
     */
    public static String randomString() {
        byte[] buffer = new byte[32];
        SECURE_RANDOM.nextBytes(buffer);
        StringBuilder sb = new StringBuilder();
        for (byte b : buffer) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}