package com.bookstore.domain;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Cart Item Entity
 * 
 * REMEDIATIONS:
 * 1. ✅ Changed javax.persistence.* to jakarta.persistence.*
 * 2. ✅ Changed javax.validation.* to jakarta.validation.*
 * 3. ✅ Added validation annotations
 * 
 * @author Bookstore Team
 * @version 2.0 (Remediated)
 */
@Entity
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private int qty;

    private BigDecimal subtotal;

    @OneToOne
    private Book book;

    @OneToMany(mappedBy = "cartItem")
    @JsonIgnore
    private List<BookToCartItem> bookToCartItemList;

    @ManyToOne
    @JoinColumn(name = "shopping_cart_id")
    private ShoppingCart shoppingCart;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    // Constructors
    public CartItem() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public List<BookToCartItem> getBookToCartItemList() { return bookToCartItemList; }
    public void setBookToCartItemList(List<BookToCartItem> bookToCartItemList) { this.bookToCartItemList = bookToCartItemList; }

    public ShoppingCart getShoppingCart() { return shoppingCart; }
    public void setShoppingCart(ShoppingCart shoppingCart) { this.shoppingCart = shoppingCart; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
}