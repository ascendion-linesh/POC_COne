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
@Table(name = "user_shipping")
@Data
public class UserShipping implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "Recipient name is required")
    @Size(max = 100, message = "Recipient name must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Recipient name contains invalid characters")
    @Column(name = "user_shipping_name", nullable = false, length = 100)
    private String userShippingName;

    @NotBlank(message = "Street address is required")
    @Size(max = 200, message = "Street address must not exceed 200 characters")
    @Column(name = "user_shipping_street1", nullable = false, length = 200)
    private String userShippingStreet1;

    @Size(max = 200, message = "Street address line 2 must not exceed 200 characters")
    @Column(name = "user_shipping_street2", length = 200)
    private String userShippingStreet2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "City contains invalid characters")
    @Column(name = "user_shipping_city", nullable = false, length = 100)
    private String userShippingCity;

    @NotBlank(message = "State is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "State contains invalid characters")
    @Column(name = "user_shipping_state", nullable = false, length = 100)
    private String userShippingState;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 2, message = "Country must be 2-letter ISO code")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be valid 2-letter ISO code")
    @Column(name = "user_shipping_country", nullable = false, length = 2)
    private String userShippingCountry;

    @NotBlank(message = "Zipcode is required")
    @Pattern(regexp = "^[0-9]{5,10}$", message = "Zipcode must be 5-10 digits")
    @Column(name = "user_shipping_zipcode", nullable = false, length = 10)
    private String userShippingZipcode;

    @Column(name = "user_shipping_default", nullable = false)
    private boolean userShippingDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}