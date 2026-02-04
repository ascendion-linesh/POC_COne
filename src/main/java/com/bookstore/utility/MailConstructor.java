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

@Component
public class MailConstructor {
    @Autowired
    private Environment env;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    public SimpleMailMessage constructResetTokenEmail(
            String contextPath, Locale locale, String token, User user, String password
    ) {
        String url = contextPath + "/newUser?token="+token;
        String message;
        
        if (password != null && !password.isEmpty()) {
            message = "\nPlease click on this link to verify your email and edit your personal information. Your temporary password is: \n"+password;
        } else {
            message = "\nPlease click on this link to reset your password.";
        }
        
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(user.getEmail());
        email.setSubject("Bookstore - Account Verification");
        email.setText(url+message);
        email.setFrom(env.getProperty("support.email"));
        
        return email;
    }
    
    public MimeMessagePreparator constructOrderConfirmationEmail(User user, Order order, Locale locale) {
        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("user", user);
        context.setVariable("cartItemList", order.getCartItemList());
        String text = templateEngine.process("orderConfirmationEmailTemplate", context);
        
        return mimeMessage -> {
            MimeMessageHelper email = new MimeMessageHelper(mimeMessage);
            email.setTo(user.getEmail());
            email.setSubject("Order Confirmation - "+order.getId());
            email.setText(text, true);
            email.setFrom(new InternetAddress(env.getProperty("support.email")));
        };
    }
}