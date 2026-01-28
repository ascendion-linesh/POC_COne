package com.bookstore.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.bookstore.service.impl.UserSecurityService;
import com.bookstore.utility.SecurityUtility;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled=true)
public class SecurityConfig {
	
	@Autowired
	private Environment env;

	@Autowired
	private UserSecurityService userSecurityService;

	private BCryptPasswordEncoder passwordEncoder() {
		return SecurityUtility.passwordEncoder();
	}

	private static final String[] PUBLIC_MATCHERS = {
			"/css/**",
			"/js/**",
			"/image/**",
			"/",
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

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(PUBLIC_MATCHERS).permitAll()
				.anyRequest().authenticated()
			)
			.csrf(csrf -> csrf.disable())
			.cors(cors -> cors.disable())
			.formLogin(form -> form
				.failureUrl("/login?error")
				.loginPage("/login").permitAll()
			)
			.logout(logout -> logout
				.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
				.logoutSuccessUrl("/?logout")
				.deleteCookies("remember-me")
				.permitAll()
			)
			.rememberMe(rememberMe -> rememberMe.key("uniqueAndSecret"));

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = 
			http.getSharedObject(AuthenticationManagerBuilder.class);
		authenticationManagerBuilder
			.userDetailsService(userSecurityService)
			.passwordEncoder(passwordEncoder());
		return authenticationManagerBuilder.build();
	}
}