package com.bookstore.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_billing")
@Data
public class UserBilling implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "Billing name is required")
    @Size(max = 100, message = "Billing name must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Billing name contains invalid characters")
    @Column(name = "user_billing_name", nullable = false, length = 100)
    private String userBillingName;

    @NotBlank(message = "Street address is required")
    @Size(max = 200, message = "Street address must not exceed 200 characters")
    @Column(name = "user_billing_street1", nullable = false, length = 200)
    private String userBillingStreet1;

    @Size(max = 200, message = "Street address line 2 must not exceed 200 characters")
    @Column(name = "user_billing_street2", length = 200)
    private String userBillingStreet2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "City contains invalid characters")
    @Column(name = "user_billing_city", nullable = false, length = 100)
    private String userBillingCity;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "State contains invalid characters")
    @Column(name = "user_billing_state", nullable = false, length = 100)
    private String userBillingState;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 2, message = "Country must be 2-letter ISO code")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be valid 2-letter ISO code")
    @Column(name = "user_billing_country", nullable = false, length = 2)
    private String userBillingCountry;

    @NotBlank(message = "Zipcode is required")
    @Pattern(regexp = "^[0-9]{5,10}$", message = "Zipcode must be 5-10 digits")
    @Column(name = "user_billing_zipcode", nullable = false, length = 10)
    private String userBillingZipcode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToOne(mappedBy = "userBilling", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private UserPayment userPayment;
}