package com.bookstore.utility;

import java.security.SecureRandom;

import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtility {
    
    @Bean
    public static BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12, new SecureRandom());
    }
    
    @Bean
    public static String randomPassword() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();
        
        while (password.length() < 20) {
            int index = secureRandom.nextInt(SALTCHARS.length());
            password.append(SALTCHARS.charAt(index));
        }
        
        return password.toString();
    }
}