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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shopping_cart")
@Data
@EqualsAndHashCode(exclude = {"cartItemList"})
public class ShoppingCart implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull(message = "Grand total is required")
    @DecimalMin(value = "0.00", message = "Grand total must be at least 0.00")
    @DecimalMax(value = "999999.99", message = "Grand total must not exceed 999999.99")
    @Digits(integer = 8, fraction = 2, message = "Grand total must have at most 8 integer digits and 2 decimal places")
    @Column(name = "grand_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "shoppingCart", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CartItem> cartItemList = new ArrayList<>();

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Transient
    public int getTotalItems() {
        return cartItemList != null ? cartItemList.stream()
            .mapToInt(CartItem::getQty)
            .sum() : 0;
    }

    @Transient
    public void updateGrandTotal() {
        this.grandTotal = cartItemList != null ? cartItemList.stream()
            .map(CartItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
    }
}