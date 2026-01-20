package com.bookstore.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "book", indexes = {
    @Index(name = "idx_book_title", columnList = "title"),
    @Index(name = "idx_book_author", columnList = "author"),
    @Index(name = "idx_book_category", columnList = "category"),
    @Index(name = "idx_book_isbn", columnList = "isbn")
})
@Data
@EqualsAndHashCode(exclude = {"bookToCartItemList"})
public class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @NotBlank(message = "Author is required")
    @Size(min = 1, max = 100, message = "Author must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s.'-]+$", message = "Author name contains invalid characters")
    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @NotBlank(message = "Publisher is required")
    @Size(max = 100, message = "Publisher must not exceed 100 characters")
    @Column(name = "publisher", nullable = false, length = 100)
    private String publisher;

    @NotBlank(message = "Publication date is required")
    @Size(max = 50, message = "Publication date must not exceed 50 characters")
    @Column(name = "publication_date", nullable = false, length = 50)
    private String publicationDate;

    @NotBlank(message = "Language is required")
    @Size(min = 2, max = 50, message = "Language must be between 2 and 50 characters")
    @Column(name = "language", nullable = false, length = 50)
    private String language;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @NotNull(message = "Number of pages is required")
    @Min(value = 1, message = "Number of pages must be at least 1")
    @Max(value = 10000, message = "Number of pages must not exceed 10000")
    @Column(name = "number_of_pages", nullable = false)
    private Integer numberOfPages;

    @NotBlank(message = "Format is required")
    @Size(max = 50, message = "Format must not exceed 50 characters")
    @Column(name = "format", nullable = false, length = 50)
    private String format;

    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$", 
             message = "ISBN must be valid ISBN-10 or ISBN-13 format")
    @Column(name = "isbn", unique = true, nullable = false, length = 20)
    private String isbn;

    @NotNull(message = "Shipping weight is required")
    @DecimalMin(value = "0.01", message = "Shipping weight must be at least 0.01")
    @DecimalMax(value = "100.00", message = "Shipping weight must not exceed 100.00")
    @Digits(integer = 3, fraction = 2, message = "Shipping weight must have at most 3 integer digits and 2 decimal places")
    @Column(name = "shipping_weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal shippingWeight;

    @NotNull(message = "List price is required")
    @DecimalMin(value = "0.00", message = "List price must be at least 0.00")
    @DecimalMax(value = "999999.99", message = "List price must not exceed 999999.99")
    @Digits(integer = 8, fraction = 2, message = "List price must have at most 8 integer digits and 2 decimal places")
    @Column(name = "list_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal listPrice;

    @NotNull(message = "Our price is required")
    @DecimalMin(value = "0.00", message = "Our price must be at least 0.00")
    @DecimalMax(value = "999999.99", message = "Our price must not exceed 999999.99")
    @Digits(integer = 8, fraction = 2, message = "Our price must have at most 8 integer digits and 2 decimal places")
    @Column(name = "our_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal ourPrice;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must be at least 0")
    @Max(value = 1000000, message = "Stock quantity must not exceed 1000000")
    @Column(name = "in_stock_number", nullable = false)
    private Integer inStockNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private MultipartFile bookImage;

    @JsonIgnore
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<BookToCartItem> bookToCartItemList;

    @Transient
    public boolean isInStock() {
        return inStockNumber != null && inStockNumber > 0;
    }
}