package com.bookstore.utility;

import java.security.SecureRandom;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtility {
	
	@Value("${security.password.strength:12}")
	private int passwordStrength;
	
	@Bean
	public static BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12, new SecureRandom());
	}
	
	@Bean
	public static String randomPassword() {
		String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
		StringBuilder password = new StringBuilder();
		SecureRandom rnd = new SecureRandom();
		
		while (password.length() < 20) {
			int index = rnd.nextInt(SALTCHARS.length());
			password.append(SALTCHARS.charAt(index));
		}
		
		return password.toString();
	}
	
	public static String generateSecureToken(int length) {
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[length];
		random.nextBytes(bytes);
		
		StringBuilder token = new StringBuilder();
		for (byte b : bytes) {
			token.append(String.format("%02x", b));
		}
		
		return token.toString();
	}
}