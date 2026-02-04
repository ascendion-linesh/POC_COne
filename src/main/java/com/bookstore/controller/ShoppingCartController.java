package com.bookstore.controller;

import java.security.Principal;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookstore.domain.Book;
import com.bookstore.domain.CartItem;
import com.bookstore.domain.ShoppingCart;
import com.bookstore.domain.User;
import com.bookstore.service.BookService;
import com.bookstore.service.CartItemService;
import com.bookstore.service.ShoppingCartService;
import com.bookstore.service.UserService;

@Controller
@RequestMapping("/shoppingCart")
@Validated
public class ShoppingCartController {
	
	private static final Logger logger = LoggerFactory.getLogger(ShoppingCartController.class);
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private CartItemService cartItemService;
	
	@Autowired
	private BookService bookService;
	
	@Autowired
	private ShoppingCartService shoppingCartService;
	
	@RequestMapping("/cart")
	public String shoppingCart(Model model, Principal principal) {
		User user = userService.findByUsername(principal.getName());
		ShoppingCart shoppingCart = user.getShoppingCart();
		
		List<CartItem> cartItemList = cartItemService.findByShoppingCart(shoppingCart);
		
		shoppingCartService.updateShoppingCart(shoppingCart);
		
		model.addAttribute("cartItemList", cartItemList);
		model.addAttribute("shoppingCart", shoppingCart);
		
		return "shoppingCart";
	}

	@RequestMapping("/addItem")
	public String addItem(
			@ModelAttribute("book") Book book,
			@RequestParam("qty") @NotNull @Min(1) @Max(100) Integer qty,
			Model model, 
			Principal principal
	) {
		if(book == null || book.getId() == null || book.getId() <= 0) {
			logger.warn("Invalid book data");
			return "redirect:/bookshelf";
		}
		
		if(qty == null || qty < 1 || qty > 100) {
			logger.warn("Invalid quantity: {}", qty);
			model.addAttribute("invalidQuantity", true);
			return "forward:/bookDetail?id=" + book.getId();
		}
		
		User user = userService.findByUsername(principal.getName());
		Book fullBook = bookService.findOne(book.getId());
		
		if(fullBook == null) {
			logger.warn("Book not found: {}", book.getId());
			return "redirect:/bookshelf";
		}
		
		if (qty > fullBook.getInStockNumber()) {
			logger.warn("Insufficient stock for book {}: requested {}, available {}", 
				fullBook.getId(), qty, fullBook.getInStockNumber());
			model.addAttribute("notEnoughStock", true);
			return "forward:/bookDetail?id=" + fullBook.getId();
		}
		
		try {
			CartItem cartItem = cartItemService.addBookToCartItem(fullBook, user, qty);
			model.addAttribute("addBookSuccess", true);
			logger.info("Added {} units of book {} to cart for user {}", 
				qty, fullBook.getId(), user.getId());
		} catch (Exception e) {
			logger.error("Failed to add book to cart", e);
			model.addAttribute("error", "Failed to add book to cart");
		}
		
		return "forward:/bookDetail?id=" + fullBook.getId();
	}
	
	@RequestMapping("/updateCartItem")
	public String updateShoppingCart(
			@RequestParam("id") @NotNull Long cartItemId,
			@RequestParam("qty") @NotNull @Min(1) @Max(100) Integer qty
	) {
		if(cartItemId == null || cartItemId <= 0) {
			logger.warn("Invalid cart item ID: {}", cartItemId);
			return "forward:/shoppingCart/cart";
		}
		
		if(qty == null || qty < 1 || qty > 100) {
			logger.warn("Invalid quantity: {}", qty);
			return "forward:/shoppingCart/cart";
		}
		
		CartItem cartItem = cartItemService.findById(cartItemId);
		
		if(cartItem == null) {
			logger.warn("Cart item not found: {}", cartItemId);
			return "forward:/shoppingCart/cart";
		}
		
		if(qty > cartItem.getBook().getInStockNumber()) {
			logger.warn("Insufficient stock for cart item {}", cartItemId);
			return "forward:/shoppingCart/cart";
		}
		
		cartItem.setQty(qty);
		cartItemService.updateCartItem(cartItem);
		
		return "forward:/shoppingCart/cart";
	}
	
	@RequestMapping("/removeItem")
	public String removeItem(@RequestParam("id") @NotNull Long id) {
		if(id == null || id <= 0) {
			logger.warn("Invalid cart item ID: {}", id);
			return "forward:/shoppingCart/cart";
		}
		
		CartItem cartItem = cartItemService.findById(id);
		
		if(cartItem == null) {
			logger.warn("Cart item not found: {}", id);
			return "forward:/shoppingCart/cart";
		}
		
		cartItemService.removeCartItem(cartItem);
		logger.info("Removed cart item {}", id);
		
		return "forward:/shoppingCart/cart";
	}
}