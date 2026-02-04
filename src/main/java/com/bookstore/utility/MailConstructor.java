package com.bookstore.utility;

import java.util.Locale;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.bookstore.domain.Order;
import com.bookstore.domain.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MailConstructor {
	
	private static final Logger logger = LoggerFactory.getLogger(MailConstructor.class);
	
	@Autowired
	private Environment env;
	
	@Autowired
	private TemplateEngine templateEngine;
	
	public SimpleMailMessage constructResetTokenEmail(
			String contextPath, 
			Locale locale, 
			String token, 
			User user, 
			String password
	) {
		String url = contextPath + "/newUser?token=" + token;
		String message = "\nPlease click on this link to verify your email and edit your personal information. Your password is: \n" + password;
		
		SimpleMailMessage email = new SimpleMailMessage();
		email.setTo(user.getEmail());
		email.setSubject("Bookstore - New User Registration");
		email.setText(url + message);
		email.setFrom(env.getProperty("support.email", "noreply@bookstore.com"));
		
		return email;
	}
	
	public MimeMessagePreparator constructOrderConfirmationEmail(
			User user, 
			Order order, 
			Locale locale
	) {
		Context context = new Context();
		context.setVariable("order", order);
		context.setVariable("user", user);
		context.setVariable("cartItemList", order.getCartItemList());
		String text = templateEngine.process("orderConfirmationEmailTemplate", context);
		
		return mimeMessage -> {
			try {
				MimeMessageHelper email = new MimeMessageHelper(mimeMessage, true, "UTF-8");
				email.setTo(user.getEmail());
				email.setSubject("Order Confirmation - " + order.getId());
				email.setText(text, true);
				email.setFrom(new InternetAddress(
					env.getProperty("support.email", "noreply@bookstore.com")
				));
			} catch (Exception e) {
				logger.error("Failed to construct order confirmation email", e);
				throw new RuntimeException("Email construction failed", e);
			}
		};
	}
}