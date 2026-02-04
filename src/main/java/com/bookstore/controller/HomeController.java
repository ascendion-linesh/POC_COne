package com.bookstore.controller;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookstore.domain.Book;
import com.bookstore.domain.CartItem;
import com.bookstore.domain.Order;
import com.bookstore.domain.User;
import com.bookstore.domain.UserBilling;
import com.bookstore.domain.UserPayment;
import com.bookstore.domain.UserShipping;
import com.bookstore.domain.security.PasswordResetToken;
import com.bookstore.domain.security.Role;
import com.bookstore.domain.security.UserRole;
import com.bookstore.service.BookService;
import com.bookstore.service.CartItemService;
import com.bookstore.service.OrderService;
import com.bookstore.service.UserPaymentService;
import com.bookstore.service.UserService;
import com.bookstore.service.UserShippingService;
import com.bookstore.service.impl.UserSecurityService;
import com.bookstore.utility.MailConstructor;
import com.bookstore.utility.SecurityUtility;
import com.bookstore.utility.USConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@Validated
public class HomeController {
	
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
	
	@Autowired
	private JavaMailSender mailSender;
	
	@Autowired
	private MailConstructor mailConstructor;

	@Autowired
	private UserService userService;
	
	@Autowired
	private UserSecurityService userSecurityService;
	
	@Autowired
	private BookService bookService;
	
	@Autowired
	private UserPaymentService userPaymentService;
	
	@Autowired
	private UserShippingService userShippingService;
	
	@Autowired
	private CartItemService cartItemService;
	
	@Autowired
	private OrderService orderService;

	@RequestMapping("/")
	public String index() {
		return "index";
	}

	@RequestMapping("/login")
	public String login(Model model) {
		model.addAttribute("classActiveLogin", true);
		return "myAccount";
	}
	
	@RequestMapping("/hours")
	public String hours() {
		return "hours";
	}
	
	@RequestMapping("/faq")
	public String faq() {
		return "faq";
	}
	
	@RequestMapping("/bookshelf")
	public String bookshelf(Model model, Principal principal) {
		if(principal != null) {
			String username = principal.getName();
			User user = userService.findByUsername(username);
			model.addAttribute("user", user);
		}
		
		List<Book> bookList = bookService.findAll();
		model.addAttribute("bookList", bookList);
		model.addAttribute("activeAll", true);
		
		return "bookshelf";
	}
	
	@RequestMapping("/bookDetail")
	public String bookDetail(
			@RequestParam("id") Long id, 
			Model model, 
			Principal principal
	) {
		if(principal != null) {
			String username = principal.getName();
			User user = userService.findByUsername(username);
			model.addAttribute("user", user);
		}
		
		if(id == null || id <= 0) {
			logger.warn("Invalid book ID requested: {}", id);
			return "redirect:/bookshelf";
		}
		
		Book book = bookService.findOne(id);
		
		if(book == null) {
			logger.warn("Book not found with ID: {}", id);
			return "redirect:/bookshelf";
		}
		
		model.addAttribute("book", book);
		
		List<Integer> qtyList = Arrays.asList(1,2,3,4,5,6,7,8,9,10);
		
		model.addAttribute("qtyList", qtyList);
		model.addAttribute("qty", 1);
		
		return "bookDetail";
	}

	@RequestMapping("/forgetPassword")
	public String forgetPassword(
			HttpServletRequest request,
			@RequestParam("email") @Email(message = "Invalid email format") 
			@NotBlank(message = "Email is required") String email,
			Model model
	) {
		model.addAttribute("classActiveForgetPassword", true);
		
		email = email.trim().toLowerCase();
		
		User user = userService.findByEmail(email);
		
		if (user == null) {
			logger.warn("Password reset requested for non-existent email");
			model.addAttribute("emailNotExist", true);
			return "myAccount";
		}
		
		String password = SecurityUtility.randomPassword();
		String encryptedPassword = SecurityUtility.passwordEncoder().encode(password);
		user.setPassword(encryptedPassword);
		
		userService.save(user);
		
		String token = UUID.randomUUID().toString();
		userService.createPasswordResetTokenForUser(user, token);
		
		String appUrl = request.getScheme() + "://" + 
						request.getServerName() + ":" + 
						request.getServerPort() + 
						request.getContextPath();
		
		try {
			SimpleMailMessage newEmail = mailConstructor.constructResetTokenEmail(
				appUrl, request.getLocale(), token, user, password
			);
			mailSender.send(newEmail);
			model.addAttribute("forgetPasswordEmailSent", "true");
			logger.info("Password reset email sent successfully");
		} catch (Exception e) {
			logger.error("Failed to send password reset email", e);
			model.addAttribute("emailError", true);
		}
		
		return "myAccount";
	}
	
	@RequestMapping("/myProfile")
	public String myProfile(Model model, Principal principal) {
		User user = userService.findByUsername(principal.getName());
		model.addAttribute("user", user);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		UserShipping userShipping = new UserShipping();
		model.addAttribute("userShipping", userShipping);
		
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("listOfShippingAddresses", true);
		
		List<String> stateList = USConstants.listOfUSStatesCode;
		Collections.sort(stateList);
		model.addAttribute("stateList", stateList);
		model.addAttribute("classActiveEdit", true);
		
		return "myProfile";
	}
	
	@RequestMapping("/listOfCreditCards")
	public String listOfCreditCards(
			Model model, Principal principal, HttpServletRequest request
	) {
		User user = userService.findByUsername(principal.getName());
		model.addAttribute("user", user);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("classActiveBilling", true);
		model.addAttribute("listOfShippingAddresses", true);
		
		return "myProfile";
	}
	
	@RequestMapping("/listOfShippingAddresses")
	public String listOfShippingAddresses(
			Model model, Principal principal, HttpServletRequest request
	) {
		User user = userService.findByUsername(principal.getName());
		model.addAttribute("user", user);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("classActiveShipping", true);
		model.addAttribute("listOfShippingAddresses", true);
		
		return "myProfile";
	}
	
	@RequestMapping("/addNewCreditCard")
	public String addNewCreditCard(Model model, Principal principal) {
		User user = userService.findByUsername(principal.getName());
		model.addAttribute("user", user);
		
		model.addAttribute("addNewCreditCard", true);
		model.addAttribute("classActiveBilling", true);
		model.addAttribute("listOfShippingAddresses", true);
		
		UserBilling userBilling = new UserBilling();
		UserPayment userPayment = new UserPayment();
		
		model.addAttribute("userBilling", userBilling);
		model.addAttribute("userPayment", userPayment);
		
		List<String> stateList = USConstants.listOfUSStatesCode;
		Collections.sort(stateList);
		model.addAttribute("stateList", stateList);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	
	@RequestMapping("/addNewShippingAddress")
	public String addNewShippingAddress(Model model, Principal principal) {
		User user = userService.findByUsername(principal.getName());
		model.addAttribute("user", user);
		
		model.addAttribute("addNewShippingAddress", true);
		model.addAttribute("classActiveShipping", true);
		model.addAttribute("listOfCreditCards", true);
		
		UserShipping userShipping = new UserShipping();
		model.addAttribute("userShipping", userShipping);
		
		List<String> stateList = USConstants.listOfUSStatesCode;
		Collections.sort(stateList);
		model.addAttribute("stateList", stateList);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	
	@RequestMapping(value="/addNewCreditCard", method=RequestMethod.POST)
	public String addNewCreditCard(
			@Valid @ModelAttribute("userPayment") UserPayment userPayment,
			@Valid @ModelAttribute("userBilling") UserBilling userBilling,
			BindingResult result,
			Principal principal, 
			Model model
	) {
		if(result.hasErrors()) {
			model.addAttribute("validationErrors", true);
			return "myProfile";
		}
		
		User user = userService.findByUsername(principal.getName());
		userService.updateUserBilling(userBilling, userPayment, user);
		
		model.addAttribute("user", user);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("classActiveBilling", true);
		model.addAttribute("listOfShippingAddresses", true);
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	
	@RequestMapping(value="/addNewShippingAddress", method=RequestMethod.POST)
	public String addNewShippingAddressPost(
			@Valid @ModelAttribute("userShipping") UserShipping userShipping,
			BindingResult result,
			Principal principal, 
			Model model
	) {
		if(result.hasErrors()) {
			model.addAttribute("validationErrors", true);
			return "myProfile";
		}
		
		User user = userService.findByUsername(principal.getName());
		userService.updateUserShipping(userShipping, user);
		
		model.addAttribute("user", user);
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("listOfShippingAddresses", true);
		model.addAttribute("classActiveShipping", true);
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	
	@RequestMapping("/updateCreditCard")
	public String updateCreditCard(
			@RequestParam("id") Long creditCardId, 
			Principal principal, 
			Model model
	) {
		if(creditCardId == null || creditCardId <= 0) {
			logger.warn("Invalid credit card ID: {}", creditCardId);
			return "badRequestPage";
		}
		
		User user = userService.findByUsername(principal.getName());
		UserPayment userPayment = userPaymentService.findById(creditCardId);
		
		if(userPayment == null) {
			logger.warn("Credit card not found: {}", creditCardId);
			return "badRequestPage";
		}
		
		if(!user.getId().equals(userPayment.getUser().getId())) {
			logger.warn("Unauthorized access attempt to credit card {} by user {}", 
				creditCardId, user.getId());
			return "badRequestPage";
		}
		
		model.addAttribute("user", user);
		UserBilling userBilling = userPayment.getUserBilling();
		model.addAttribute("userPayment", userPayment);
		model.addAttribute("userBilling", userBilling);
		
		List<String> stateList = USConstants.listOfUSStatesCode;
		Collections.sort(stateList);
		model.addAttribute("stateList", stateList);
		
		model.addAttribute("addNewCreditCard", true);
		model.addAttribute("classActiveBilling", true);
		model.addAttribute("listOfShippingAddresses", true);
		
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	
	@RequestMapping("/updateUserShipping")
	public String updateUserShipping(
			@RequestParam("id") Long shippingAddressId, 
			Principal principal, 
			Model model
	) {
		if(shippingAddressId == null || shippingAddressId <= 0) {
			logger.warn("Invalid shipping address ID: {}", shippingAddressId);
			return "badRequestPage";
		}
		
		User user = userService.findByUsername(principal.getName());
		UserShipping userShipping = userShippingService.findById(shippingAddressId);
		
		if(userShipping == null) {
			logger.warn("Shipping address not found: {}", shippingAddressId);
			return "badRequestPage";
		}
		
		if(!user.getId().equals(userShipping.getUser().getId())) {
			logger.warn("Unauthorized access attempt to shipping address {} by user {}", 
				shippingAddressId, user.getId());
			return "badRequestPage";
		}
		
		model.addAttribute("user", user);
		model.addAttribute("userShipping", userShipping);
		
		List<String> stateList = USConstants.listOfUSStatesCode;
		Collections.sort(stateList);
		model.addAttribute("stateList", stateList);
		
		model.addAttribute("addNewShippingAddress", true);
		model.addAttribute("classActiveShipping", true);
		model.addAttribute("listOfCreditCards", true);
		
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	
	@RequestMapping(value="/setDefaultPayment", method=RequestMethod.POST)
	public String setDefaultPayment(
			@RequestParam("defaultUserPaymentId") Long defaultPaymentId, 
			Principal principal, 
			Model model
	) {
		if(defaultPaymentId == null || defaultPaymentId <= 0) {
			logger.warn("Invalid payment ID: {}", defaultPaymentId);
			return "badRequestPage";
		}
		
		User user = userService.findByUsername(principal.getName());
		userService.setUserDefaultPayment(defaultPaymentId, user);
		
		model.addAttribute("user", user);
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("classActiveBilling", true);
		model.addAttribute("listOfShippingAddresses", true);
		
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	
	@RequestMapping(value="/setDefaultShippingAddress", method=RequestMethod.POST)
	public String setDefaultShippingAddress(
			@RequestParam("defaultShippingAddressId") Long defaultShippingId, 
			Principal principal, 
			Model model
	) {
		if(defaultShippingId == null || defaultShippingId <= 0) {
			logger.warn("Invalid shipping ID: {}", defaultShippingId);
			return "badRequestPage";
		}
		
		User user = userService.findByUsername(principal.getName());
		userService.setUserDefaultShipping(defaultShippingId, user);
		
		model.addAttribute("user", user);
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("classActiveShipping", true);
		model.addAttribute("listOfShippingAddresses", true);
		
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	
	@RequestMapping("/removeCreditCard")
	public String removeCreditCard(
			@RequestParam("id") Long creditCardId, 
			Principal principal, 
			Model model
	) {
		if(creditCardId == null || creditCardId <= 0) {
			logger.warn("Invalid credit card ID: {}", creditCardId);
			return "badRequestPage";
		}
		
		User user = userService.findByUsername(principal.getName());
		UserPayment userPayment = userPaymentService.findById(creditCardId);
		
		if(userPayment == null) {
			logger.warn("Credit card not found: {}", creditCardId);
			return "badRequestPage";
		}
		
		if(!user.getId().equals(userPayment.getUser().getId())) {
			logger.warn("Unauthorized deletion attempt of credit card {} by user {}", 
				creditCardId, user.getId());
			return "badRequestPage";
		}
		
		model.addAttribute("user", user);
		userPaymentService.removeById(creditCardId);
		
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("classActiveBilling", true);
		model.addAttribute("listOfShippingAddresses", true);
		
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	
	@RequestMapping("/removeUserShipping")
	public String removeUserShipping(
			@RequestParam("id") Long userShippingId, 
			Principal principal, 
			Model model
	) {
		if(userShippingId == null || userShippingId <= 0) {
			logger.warn("Invalid shipping address ID: {}", userShippingId);
			return "badRequestPage";
		}
		
		User user = userService.findByUsername(principal.getName());
		UserShipping userShipping = userShippingService.findById(userShippingId);
		
		if(userShipping == null) {
			logger.warn("Shipping address not found: {}", userShippingId);
			return "badRequestPage";
		}
		
		if(!user.getId().equals(userShipping.getUser().getId())) {
			logger.warn("Unauthorized deletion attempt of shipping address {} by user {}", 
				userShippingId, user.getId());
			return "badRequestPage";
		}
		
		model.addAttribute("user", user);
		userShippingService.removeById(userShippingId);
		
		model.addAttribute("listOfShippingAddresses", true);
		model.addAttribute("classActiveShipping", true);
		
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		return "myProfile";
	}
	
	@RequestMapping(value="/newUser", method = RequestMethod.POST)
	public String newUserPost(
			HttpServletRequest request,
			@RequestParam("email") @Email(message = "Invalid email format") 
			@NotBlank(message = "Email is required") String userEmail,
			@RequestParam("username") @NotBlank(message = "Username is required") 
			@Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") String username,
			Model model
	) {
		model.addAttribute("classActiveNewAccount", true);
		
		userEmail = userEmail.trim().toLowerCase();
		username = username.trim();
		
		model.addAttribute("email", userEmail);
		model.addAttribute("username", username);
		
		if (userService.findByUsername(username) != null) {
			logger.warn("Username already exists: {}", username);
			model.addAttribute("usernameExists", true);
			return "myAccount";
		}
		
		if (userService.findByEmail(userEmail) != null) {
			logger.warn("Email already exists");
			model.addAttribute("emailExists", true);
			return "myAccount";
		}
		
		User user = new User();
		user.setUsername(username);
		user.setEmail(userEmail);
		
		String password = SecurityUtility.randomPassword();
		String encryptedPassword = SecurityUtility.passwordEncoder().encode(password);
		user.setPassword(encryptedPassword);
		
		Role role = new Role();
		role.setRoleId(1);
		role.setName("ROLE_USER");
		Set<UserRole> userRoles = new HashSet<>();
		userRoles.add(new UserRole(user, role));
		
		try {
			userService.createUser(user, userRoles);
			
			String token = UUID.randomUUID().toString();
			userService.createPasswordResetTokenForUser(user, token);
			
			String appUrl = request.getScheme() + "://" + 
							request.getServerName() + ":" + 
							request.getServerPort() + 
							request.getContextPath();
			
			SimpleMailMessage email = mailConstructor.constructResetTokenEmail(
				appUrl, request.getLocale(), token, user, password
			);
			mailSender.send(email);
			
			model.addAttribute("emailSent", "true");
			model.addAttribute("orderList", user.getOrderList());
			logger.info("New user created successfully: {}", username);
		} catch (Exception e) {
			logger.error("Failed to create new user", e);
			model.addAttribute("error", "Failed to create user. Please try again.");
			return "myAccount";
		}
		
		return "myAccount";
	}

	@RequestMapping("/newUser")
	public String newUser(
			Locale locale, 
			@RequestParam("token") String token, 
			Model model
	) {
		PasswordResetToken passToken = userService.getPasswordResetToken(token);

		if (passToken == null) {
			logger.warn("Invalid password reset token");
			String message = "Invalid Token.";
			model.addAttribute("message", message);
			return "redirect:/badRequest";
		}

		User user = passToken.getUser();
		String username = user.getUsername();

		UserDetails userDetails = userSecurityService.loadUserByUsername(username);

		Authentication authentication = new UsernamePasswordAuthenticationToken(
			userDetails, 
			userDetails.getPassword(),
			userDetails.getAuthorities()
		);
		
		SecurityContextHolder.getContext().setAuthentication(authentication);
		
		model.addAttribute("user", user);
		model.addAttribute("classActiveEdit", true);
		
		return "myProfile";
	}
	
	@RequestMapping(value="/updateUserInfo", method=RequestMethod.POST)
	public String updateUserInfo(
			@Valid @ModelAttribute("user") User user,
			@RequestParam(value = "newPassword", required = false) String newPassword,
			BindingResult result,
			Model model
	) {
		if(result.hasErrors()) {
			model.addAttribute("validationErrors", true);
			return "myProfile";
		}
		
		User currentUser = userService.findById(user.getId());
		
		if(currentUser == null) {
			logger.error("User not found: {}", user.getId());
			model.addAttribute("error", "User not found");
			return "myProfile";
		}
		
		User existingEmailUser = userService.findByEmail(user.getEmail());
		if (existingEmailUser != null && !existingEmailUser.getId().equals(currentUser.getId())) {
			model.addAttribute("emailExists", true);
			return "myProfile";
		}
		
		User existingUsernameUser = userService.findByUsername(user.getUsername());
		if (existingUsernameUser != null && !existingUsernameUser.getId().equals(currentUser.getId())) {
			model.addAttribute("usernameExists", true);
			return "myProfile";
		}
		
		if (newPassword != null && !newPassword.isEmpty() && newPassword.length() >= 8) {
			BCryptPasswordEncoder passwordEncoder = SecurityUtility.passwordEncoder();
			String dbPassword = currentUser.getPassword();
			
			if(passwordEncoder.matches(user.getPassword(), dbPassword)) {
				currentUser.setPassword(passwordEncoder.encode(newPassword));
			} else {
				model.addAttribute("incorrectPassword", true);
				return "myProfile";
			}
		}
		
		currentUser.setFirstName(user.getFirstName());
		currentUser.setLastName(user.getLastName());
		currentUser.setUsername(user.getUsername());
		currentUser.setEmail(user.getEmail());
		
		userService.save(currentUser);
		
		model.addAttribute("updateSuccess", true);
		model.addAttribute("user", currentUser);
		model.addAttribute("classActiveEdit", true);
		model.addAttribute("listOfShippingAddresses", true);
		model.addAttribute("listOfCreditCards", true);
		
		UserDetails userDetails = userSecurityService.loadUserByUsername(currentUser.getUsername());
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			userDetails, 
			userDetails.getPassword(),
			userDetails.getAuthorities()
		);
		
		SecurityContextHolder.getContext().setAuthentication(authentication);
		model.addAttribute("orderList", currentUser.getOrderList());
		
		return "myProfile";
	}
	
	@RequestMapping("/orderDetail")
	public String orderDetail(
			@RequestParam("id") Long orderId,
			Principal principal, 
			Model model
	) {
		if(orderId == null || orderId <= 0) {
			logger.warn("Invalid order ID: {}", orderId);
			return "badRequestPage";
		}
		
		User user = userService.findByUsername(principal.getName());
		Order order = orderService.findOne(orderId);
		
		if(order == null) {
			logger.warn("Order not found: {}", orderId);
			return "badRequestPage";
		}
		
		if(!order.getUser().getId().equals(user.getId())) {
			logger.warn("Unauthorized access attempt to order {} by user {}", 
				orderId, user.getId());
			return "badRequestPage";
		}
		
		List<CartItem> cartItemList = cartItemService.findByOrder(order);
		model.addAttribute("cartItemList", cartItemList);
		model.addAttribute("user", user);
		model.addAttribute("order", order);
		
		model.addAttribute("userPaymentList", user.getUserPaymentList());
		model.addAttribute("userShippingList", user.getUserShippingList());
		model.addAttribute("orderList", user.getOrderList());
		
		UserShipping userShipping = new UserShipping();
		model.addAttribute("userShipping", userShipping);
		
		List<String> stateList = USConstants.listOfUSStatesCode;
		Collections.sort(stateList);
		model.addAttribute("stateList", stateList);
		
		model.addAttribute("listOfShippingAddresses", true);
		model.addAttribute("classActiveOrders", true);
		model.addAttribute("listOfCreditCards", true);
		model.addAttribute("displayOrderDetail", true);
		
		return "myProfile";
	}
}