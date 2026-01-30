package com.bookstore.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import org.springframework.web.multipart.MultipartFile;

/**
 * Book Entity
 * 
 * REMEDIATIONS:
 * 1. ✅ Changed javax.persistence.* to jakarta.persistence.*
 * 2. ✅ Changed javax.validation.* to jakarta.validation.*
 * 3. ✅ Added comprehensive validation annotations
 * 
 * @author Bookstore Team
 * @version 2.0 (Remediated)
 */
@Entity
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @NotBlank(message = "Author is required")
    @Size(min = 1, max = 255, message = "Author must be between 1 and 255 characters")
    private String author;

    @NotBlank(message = "Publisher is required")
    @Size(max = 255, message = "Publisher must not exceed 255 characters")
    private String publisher;

    @NotBlank(message = "Publication date is required")
    private String publicationDate;

    @NotBlank(message = "Language is required")
    @Size(max = 50, message = "Language must not exceed 50 characters")
    private String language;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @NotNull(message = "Number of pages is required")
    @Min(value = 1, message = "Number of pages must be at least 1")
    private Integer numberOfPages;

    @NotBlank(message = "Format is required")
    @Size(max = 50, message = "Format must not exceed 50 characters")
    private String format;

    @NotNull(message = "ISBN is required")
    @Min(value = 1, message = "ISBN must be a positive number")
    private Long isbn;

    @NotNull(message = "Shipping weight is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Shipping weight must be greater than 0")
    private Double shippingWeight;

    @NotNull(message = "List price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "List price must be greater than 0")
    private Double listPrice;

    @NotNull(message = "Our price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Our price must be greater than 0")
    private Double ourPrice;

    private boolean active = true;

    @Column(columnDefinition = "text")
    @Size(max = 10000, message = "Description must not exceed 10000 characters")
    private String description;

    @NotNull(message = "In stock number is required")
    @Min(value = 0, message = "In stock number cannot be negative")
    private Integer inStockNumber;

    @Transient
    private MultipartFile bookImage;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<BookToCartItem> bookToCartItemList;

    // Constructors
    public Book() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getPublicationDate() { return publicationDate; }
    public void setPublicationDate(String publicationDate) { this.publicationDate = publicationDate; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getNumberOfPages() { return numberOfPages; }
    public void setNumberOfPages(Integer numberOfPages) { this.numberOfPages = numberOfPages; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Long getIsbn() { return isbn; }
    public void setIsbn(Long isbn) { this.isbn = isbn; }

    public Double getShippingWeight() { return shippingWeight; }
    public void setShippingWeight(Double shippingWeight) { this.shippingWeight = shippingWeight; }

    public Double getListPrice() { return listPrice; }
    public void setListPrice(Double listPrice) { this.listPrice = listPrice; }

    public Double getOurPrice() { return ourPrice; }
    public void setOurPrice(Double ourPrice) { this.ourPrice = ourPrice; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getInStockNumber() { return inStockNumber; }
    public void setInStockNumber(Integer inStockNumber) { this.inStockNumber = inStockNumber; }

    public MultipartFile getBookImage() { return bookImage; }
    public void setBookImage(MultipartFile bookImage) { this.bookImage = bookImage; }

    public List<BookToCartItem> getBookToCartItemList() { return bookToCartItemList; }
    public void setBookToCartItemList(List<BookToCartItem> bookToCartItemList) { this.bookToCartItemList = bookToCartItemList; }
}