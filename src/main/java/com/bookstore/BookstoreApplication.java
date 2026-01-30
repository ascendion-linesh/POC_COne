package com.bookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application Entry Point
 * 
 * Remediation Notes:
 * - Updated to Spring Boot 3.2.1
 * - Compatible with Java 17
 * - No security vulnerabilities in this file
 * 
 * @author Bookstore Team
 * @version 2.0 (Remediated)
 */
@SpringBootApplication
public class BookstoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookstoreApplication.class, args);
    }
}