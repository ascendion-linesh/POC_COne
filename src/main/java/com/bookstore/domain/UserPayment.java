package com.bookstore.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_payment")
@Data
@ToString(exclude = {"cardNumber", "cvc", "user"})
public class UserPayment implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "Payment type is required")
    @Size(max = 50, message = "Payment type must not exceed 50 characters")
    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @NotBlank(message = "Card name is required")
    @Size(max = 100, message = "Card name must not exceed 100 characters")
    @Column(name = "card_name", nullable = false, length = 100)
    private String cardName;

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Card number must be 13-19 digits")
    @JsonIgnore
    @ColumnTransformer(
        read = "pgp_sym_decrypt(card_number::bytea, current_setting('app.encryption_key'))",
        write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))"
    )
    @Column(name = "card_number", nullable = false, columnDefinition = "TEXT")
    private String cardNumber;

    @NotNull(message = "Expiry month is required")
    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @NotNull(message = "Expiry year is required")
    @Min(value = 2024, message = "Expiry year must be current or future year")
    @Max(value = 2050, message = "Expiry year must not exceed 2050")
    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    @NotBlank(message = "CVC is required")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVC must be 3 or 4 digits")
    @JsonIgnore
    @ColumnTransformer(
        read = "pgp_sym_decrypt(cvc::bytea, current_setting('app.encryption_key'))",
        write = "pgp_sym_encrypt(?, current_setting('app.encryption_key'))"
    )
    @Column(name = "cvc", nullable = false, columnDefinition = "TEXT")
    private String cvc;

    @NotBlank(message = "Holder name is required")
    @Size(min = 2, max = 100, message = "Holder name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s'-]+$", message = "Holder name contains invalid characters")
    @Column(name = "holder_name", nullable = false, length = 100)
    private String holderName;

    @Column(name = "default_payment", nullable = false)
    private boolean defaultPayment = false;

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

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "user_billing_id")
    private UserBilling userBilling;

    @Transient
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "************" + cardNumber.substring(cardNumber.length() - 4);
    }
}