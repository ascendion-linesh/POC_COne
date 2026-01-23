package com.bookstore.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

import com.bookstore.domain.Book;
import com.bookstore.domain.User;
import com.bookstore.service.BookService;
import com.bookstore.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class SearchController {
	
	private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
	private static final int MAX_SEARCH_LENGTH = 100;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private BookService bookService;

	@RequestMapping("/searchByCategory")
	public String searchByCategory(
			@RequestParam("category") String category,
			Model model, Principal principal
			){
		if(principal!=null) {
			String username = principal.getName();
			User user = userService.findByUsername(username);
			model.addAttribute("user", user);
		}
		
		// SECURITY FIX: Input validation and sanitization for category
		if (category == null || category.trim().isEmpty()) {
			logger.warn("Empty category provided for search");
			model.addAttribute("emptyList", true);
			return "bookshelf";
		}
		
		if (category.length() > MAX_SEARCH_LENGTH) {
			logger.warn("Category search term too long: {}", category.length());
			model.addAttribute("emptyList", true);
			return "bookshelf";
		}
		
		// SECURITY FIX: Sanitize category to prevent XSS
		String sanitizedCategory = HtmlUtils.htmlEscape(category.trim());
		
		String classActiveCategory = "active"+sanitizedCategory;
		classActiveCategory = classActiveCategory.replaceAll("\\s+", "");
		classActiveCategory = classActiveCategory.replaceAll("&", "");
		model.addAttribute(classActiveCategory, true);
		
		List<Book> bookList = bookService.findByCategory(sanitizedCategory);
		
		if (bookList.isEmpty()) {
			logger.info("No books found for category: {}", sanitizedCategory);
			model.addAttribute("emptyList", true);
			return "bookshelf";
		}
		
		model.addAttribute("bookList", bookList);
		
		return "bookshelf";
	}
	
	@RequestMapping("/searchBook")
	public String searchBook(
			@ModelAttribute("keyword") String keyword,
			Principal principal, Model model
			) {
		if(principal!=null) {
			String username = principal.getName();
			User user = userService.findByUsername(username);
			model.addAttribute("user", user);
		}
		
		// SECURITY FIX: Input validation and sanitization for search keyword
		if (keyword == null || keyword.trim().isEmpty()) {
			logger.warn("Empty keyword provided for search");
			model.addAttribute("emptyList", true);
			return "bookshelf";
		}
		
		if (keyword.length() > MAX_SEARCH_LENGTH) {
			logger.warn("Search keyword too long: {}", keyword.length());
			model.addAttribute("emptyList", true);
			return "bookshelf";
		}
		
		// SECURITY FIX: Sanitize keyword to prevent SQL injection and XSS
		String sanitizedKeyword = HtmlUtils.htmlEscape(keyword.trim());
		
		// Additional validation: remove potentially dangerous characters
		sanitizedKeyword = sanitizedKeyword.replaceAll("[<>\"';\\\\]", "");
		
		if (sanitizedKeyword.isEmpty()) {
			logger.warn("Keyword became empty after sanitization");
			model.addAttribute("emptyList", true);
			return "bookshelf";
		}
		
		List<Book> bookList = bookService.blurrySearch(sanitizedKeyword);
		
		if (bookList.isEmpty()) {
			logger.info("No books found for keyword: {}", sanitizedKeyword);
			model.addAttribute("emptyList", true);
			return "bookshelf";
		}
		
		model.addAttribute("bookList", bookList);
		
		return "bookshelf";
	}
}