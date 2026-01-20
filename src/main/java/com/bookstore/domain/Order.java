package com.bookstore.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_order", indexes = {
    @Index(name = "idx_order_date", columnList = "order_date"),
    @Index(name = "idx_order_status", columnList = "order_status")
})
@Data
@EqualsAndHashCode(exclude = {"cartItemList", "user"})
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull(message = "Order date is required")
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @NotNull(message = "Shipping date is required")
    @Column(name = "shipping_date", nullable = false)
    private LocalDate shippingDate;

    @NotBlank(message = "Shipping method is required")
    @Size(max = 100, message = "Shipping method must not exceed 100 characters")
    @Column(name = "shipping_method", nullable = false, length = 100)
    private String shippingMethod;

    @NotBlank(message = "Order status is required")
    @Size(max = 50, message = "Order status must not exceed 50 characters")
    @Pattern(regexp = "^(PENDING|PROCESSING|SHIPPED|DELIVERED|CANCELLED|REFUNDED)$", 
             message = "Order status must be valid")
    @Column(name = "order_status", nullable = false, length = 50)
    private String orderStatus;

    @NotNull(message = "Order total is required")
    @DecimalMin(value = "0.00", message = "Order total must be at least 0.00")
    @DecimalMax(value = "999999.99", message = "Order total must not exceed 999999.99")
    @Digits(integer = 8, fraction = 2, message = "Order total must have at most 8 integer digits and 2 decimal places")
    @Column(name = "order_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal orderTotal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CartItem> cartItemList = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "shipping_address_id")
    private ShippingAddress shippingAddress;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "billing_address_id")
    private BillingAddress billingAddress;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Transient
    public int getTotalItems() {
        return cartItemList != null ? cartItemList.stream()
            .mapToInt(CartItem::getQty)
            .sum() : 0;
    }
}