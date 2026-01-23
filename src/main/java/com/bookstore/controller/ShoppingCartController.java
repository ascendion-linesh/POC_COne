package com.bookstore.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/shoppingCart")
public class ShoppingCartController {
	
	private static final Logger logger = LoggerFactory.getLogger(ShoppingCartController.class);
	private static final int MAX_QUANTITY = 100;
	private static final int MIN_QUANTITY = 1;
	
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
		if (principal == null) {
			logger.warn("Unauthorized cart access attempt");
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			logger.warn("User not found for principal: {}", principal.getName());
			return "redirect:/login";
		}
		
		ShoppingCart shoppingCart = user.getShoppingCart();
		
		if (shoppingCart == null) {
			logger.warn("Shopping cart not found for user: {}", user.getId());
			return "redirect:/";
		}
		
		List<CartItem> cartItemList = cartItemService.findByShoppingCart(shoppingCart);
		
		shoppingCartService.updateShoppingCart(shoppingCart);
		
		model.addAttribute("cartItemList", cartItemList);
		model.addAttribute("shoppingCart", shoppingCart);
		
		return "shoppingCart";
	}

	@RequestMapping("/addItem")
	public String addItem(
			@ModelAttribute("book") Book book,
			@ModelAttribute("qty") String qty,
			Model model, Principal principal
			) {
		if (principal == null) {
			logger.warn("Unauthorized addItem attempt");
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			logger.warn("User not found for principal: {}", principal.getName());
			return "redirect:/login";
		}
		
		// SECURITY FIX: Validate book and quantity
		if (book == null || book.getId() == null || book.getId() <= 0) {
			logger.warn("Invalid book provided for addItem");
			return "redirect:/bookshelf";
		}
		
		book = bookService.findOne(book.getId());
		
		if (book == null) {
			logger.warn("Book not found: {}", book.getId());
			return "redirect:/bookshelf";
		}
		
		// SECURITY FIX: Enhanced quantity validation
		int quantity;
		try {
			quantity = Integer.parseInt(qty);
		} catch (NumberFormatException e) {
			logger.warn("Invalid quantity format: {}", qty);
			model.addAttribute("invalidQuantity", true);
			return "forward:/bookDetail?id="+book.getId();
		}
		
		if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
			logger.warn("Quantity out of bounds: {}", quantity);
			model.addAttribute("invalidQuantity", true);
			return "forward:/bookDetail?id="+book.getId();
		}
		
		if (quantity > book.getInStockNumber()) {
			logger.info("Not enough stock for book {}: requested {}, available {}", 
					book.getId(), quantity, book.getInStockNumber());
			model.addAttribute("notEnoughStock", true);
			return "forward:/bookDetail?id="+book.getId();
		}
		
		CartItem cartItem = cartItemService.addBookToCartItem(book, user, quantity);
		model.addAttribute("addBookSuccess", true);
		
		logger.info("User {} added {} units of book {} to cart", user.getId(), quantity, book.getId());
		
		return "forward:/bookDetail?id="+book.getId();
	}
	
	@RequestMapping("/updateCartItem")
	public String updateShoppingCart(
			@ModelAttribute("id") Long cartItemId,
			@ModelAttribute("qty") int qty,
			Principal principal
			) {
		if (principal == null) {
			logger.warn("Unauthorized updateCartItem attempt");
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			logger.warn("User not found for principal: {}", principal.getName());
			return "redirect:/login";
		}
		
		// SECURITY FIX: Validate cart item ID
		if (cartItemId == null || cartItemId <= 0) {
			logger.warn("Invalid cart item ID: {}", cartItemId);
			return "forward:/shoppingCart/cart";
		}
		
		// SECURITY FIX: Validate quantity bounds
		if (qty < MIN_QUANTITY || qty > MAX_QUANTITY) {
			logger.warn("Quantity out of bounds: {}", qty);
			return "forward:/shoppingCart/cart";
		}
		
		CartItem cartItem = cartItemService.findById(cartItemId);
		
		if (cartItem == null) {
			logger.warn("Cart item not found: {}", cartItemId);
			return "forward:/shoppingCart/cart";
		}
		
		// SECURITY FIX: Verify cart item belongs to user
		if (cartItem.getShoppingCart() == null || 
				cartItem.getShoppingCart().getUser() == null ||
				!cartItem.getShoppingCart().getUser().getId().equals(user.getId())) {
			logger.warn("IDOR attempt detected - User {} tried to update cart item {}", 
					user.getId(), cartItemId);
			return "forward:/shoppingCart/cart";
		}
		
		// SECURITY FIX: Validate stock availability
		if (qty > cartItem.getBook().getInStockNumber()) {
			logger.warn("Not enough stock for cart item update: requested {}, available {}", 
					qty, cartItem.getBook().getInStockNumber());
			return "forward:/shoppingCart/cart";
		}
		
		cartItem.setQty(qty);
		cartItemService.updateCartItem(cartItem);
		
		logger.info("User {} updated cart item {} to quantity {}", user.getId(), cartItemId, qty);
		
		return "forward:/shoppingCart/cart";
	}
	
	@RequestMapping("/removeItem")
	public String removeItem(@RequestParam("id") Long id, Principal principal) {
		if (principal == null) {
			logger.warn("Unauthorized removeItem attempt");
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			logger.warn("User not found for principal: {}", principal.getName());
			return "redirect:/login";
		}
		
		// SECURITY FIX: Validate cart item ID
		if (id == null || id <= 0) {
			logger.warn("Invalid cart item ID for removal: {}", id);
			return "forward:/shoppingCart/cart";
		}
		
		CartItem cartItem = cartItemService.findById(id);
		
		if (cartItem == null) {
			logger.warn("Cart item not found for removal: {}", id);
			return "forward:/shoppingCart/cart";
		}
		
		// SECURITY FIX: Verify cart item belongs to user
		if (cartItem.getShoppingCart() == null || 
				cartItem.getShoppingCart().getUser() == null ||
				!cartItem.getShoppingCart().getUser().getId().equals(user.getId())) {
			logger.warn("IDOR attempt detected - User {} tried to remove cart item {}", 
					user.getId(), id);
			return "forward:/shoppingCart/cart";
		}
		
		cartItemService.removeCartItem(cartItem);
		
		logger.info("User {} removed cart item {}", user.getId(), id);
		
		return "forward:/shoppingCart/cart";
	}
}