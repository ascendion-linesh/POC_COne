package com.bookstore.controller;

import java.security.Principal;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

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
public class HomeController {
	
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
	private static final int MAX_USERNAME_LENGTH = 50;
	private static final int MAX_EMAIL_LENGTH = 100;
	
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
		model.addAttribute("activeAll",true);
		
		return "bookshelf";
	}
	
	@RequestMapping("/bookDetail")
	public String bookDetail(
			@RequestParam("id") Long id, Model model, Principal principal
			) {
		if(principal != null) {
			String username = principal.getName();
			User user = userService.findByUsername(username);
			model.addAttribute("user", user);
		}
		
		// SECURITY FIX: Validate book ID
		if (id == null || id <= 0) {
			logger.warn("Invalid book ID provided: {}", id);
			return "redirect:/bookshelf";
		}
		
		Book book = bookService.findOne(id);
		
		if (book == null) {
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
			@ModelAttribute("email") String email,
			Model model
			) {

		model.addAttribute("classActiveForgetPassword", true);
		
		// SECURITY FIX: Email validation
		if (email == null || email.trim().isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
			logger.warn("Invalid email format provided for password reset: {}", email);
			model.addAttribute("emailNotExist", true);
			return "myAccount";
		}
		
		User user = userService.findByEmail(email);
		
		if (user == null) {
			logger.info("Password reset requested for non-existent email: {}", email);
			model.addAttribute("emailNotExist", true);
			return "myAccount";
		}
		
		// SECURITY FIX: Enhanced password generation with SecureRandom
		String password = generateSecurePassword();
		
		String encryptedPassword = SecurityUtility.passwordEncoder().encode(password);
		user.setPassword(encryptedPassword);
		
		userService.save(user);
		
		String token = UUID.randomUUID().toString();
		userService.createPasswordResetTokenForUser(user, token);
		
		String appUrl = "http://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath();
		
		try {
			SimpleMailMessage newEmail = mailConstructor.constructResetTokenEmail(appUrl, request.getLocale(), token, user, password);
			mailSender.send(newEmail);
			model.addAttribute("forgetPasswordEmailSent", "true");
			logger.info("Password reset email sent to: {}", email);
		} catch (Exception e) {
			logger.error("Failed to send password reset email to: {}", email, e);
			model.addAttribute("emailError", true);
		}
		
		return "myAccount";
	}
	
	// SECURITY FIX: Improved password generation method
	private String generateSecurePassword() {
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
		SecureRandom random = new SecureRandom();
		StringBuilder password = new StringBuilder(16);
		for (int i = 0; i < 16; i++) {
			password.append(chars.charAt(random.nextInt(chars.length())));
		}
		return password.toString();
	}
	
	@RequestMapping("/myProfile")
	public String myProfile(Model model, Principal principal) {
		if (principal == null) {
			logger.warn("Unauthorized myProfile access attempt");
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			logger.warn("User not found for principal: {}", principal.getName());
			return "redirect:/login";
		}
		
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
		if (principal == null) {
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			return "redirect:/login";
		}
		
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
		if (principal == null) {
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			return "redirect:/login";
		}
		
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
	public String addNewCreditCard(
			Model model, Principal principal
			){
		if (principal == null) {
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			return "redirect:/login";
		}
		
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
	public String addNewShippingAddress(
			Model model, Principal principal
			){
		if (principal == null) {
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			return "redirect:/login";
		}
		
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
			@ModelAttribute("userPayment") UserPayment userPayment,
			@ModelAttribute("userBilling") UserBilling userBilling,
			Principal principal, Model model
			){
		if (principal == null) {
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			return "redirect:/login";
		}
		
		// SECURITY FIX: Validate payment and billing information
		if (userPayment == null || userBilling == null) {
			logger.warn("Null payment or billing information provided by user: {}", user.getId());
			return "redirect:/addNewCreditCard";
		}
		
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
			@ModelAttribute("userShipping") UserShipping userShipping,
			Principal principal, Model model
			){
		if (principal == null) {
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			return "redirect:/login";
		}
		
		// SECURITY FIX: Validate shipping information
		if (userShipping == null) {
			logger.warn("Null shipping information provided by user: {}", user.getId());
			return "redirect:/addNewShippingAddress";
		}
		
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
			@ModelAttribute("id") Long creditCardId, Principal principal, Model model
			) {
		if (principal == null) {
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			return "redirect:/login";
		}
		
		// SECURITY FIX: Validate credit card ID
		if (creditCardId == null || creditCardId <= 0) {
			logger.warn("Invalid credit card ID provided: {}", creditCardId);
			return "badRequestPage";
		}
		
		UserPayment userPayment = userPaymentService.findById(creditCardId);
		
		// SECURITY FIX: Enhanced IDOR protection
		if(userPayment == null || userPayment.getUser() == null || !user.getId().equals(userPayment.getUser().getId())) {
			logger.warn("IDOR attempt detected - User {} tried to access credit card {}", user.getId(), creditCardId);
			return "badRequestPage";
		} else {
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
	}
	
	@RequestMapping("/updateUserShipping")
	public String updateUserShipping(
			@ModelAttribute("id") Long shippingAddressId, Principal principal, Model model
			) {
		if (principal == null) {
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			return "redirect:/login";
		}
		
		// SECURITY FIX: Validate shipping address ID
		if (shippingAddressId == null || shippingAddressId <= 0) {
			logger.warn("Invalid shipping address ID provided: {}", shippingAddressId);
			return "badRequestPage";
		}
		
		UserShipping userShipping = userShippingService.findById(shippingAddressId);
		
		// SECURITY FIX: Enhanced IDOR protection
		if(userShipping == null || userShipping.getUser() == null || !user.getId().equals(userShipping.getUser().getId())) {
			logger.warn("IDOR attempt detected - User {} tried to access shipping address {}", user.getId(), shippingAddressId);
			return "badRequestPage";
		} else {
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
	}
	
	@RequestMapping(value="/setDefaultPayment", method=RequestMethod.POST)
	public String setDefaultPayment(
			@ModelAttribute("defaultUserPaymentId") Long defaultPaymentId, Principal principal, Model model
			) {
		if (principal == null) {
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			return "redirect:/login";
		}
		
		// SECURITY FIX: Validate default payment ID
		if (defaultPaymentId == null || defaultPaymentId <= 0) {
			logger.warn("Invalid default payment ID provided: {}", defaultPaymentId);
			return "redirect:/myProfile";
		}
		
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
			@ModelAttribute("defaultShippingAddressId") Long defaultShippingId, Principal principal, Model model
			) {
		if (principal == null) {
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			return "redirect:/login";
		}
		
		// SECURITY FIX: Validate default shipping ID
		if (defaultShippingId == null || defaultShippingId <= 0) {
			logger.warn("Invalid default shipping ID provided: {}", defaultShippingId);
			return "redirect:/myProfile";
		}
		
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
			@ModelAttribute("id") Long creditCardId, Principal principal, Model model
			){
		if (principal == null) {
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			return "redirect:/login";
		}
		
		// SECURITY FIX: Validate credit card ID
		if (creditCardId == null || creditCardId <= 0) {
			logger.warn("Invalid credit card ID for removal: {}", creditCardId);
			return "badRequestPage";
		}
		
		UserPayment userPayment = userPaymentService.findById(creditCardId);
		
		// SECURITY FIX: Enhanced IDOR protection
		if(userPayment == null || userPayment.getUser() == null || !user.getId().equals(userPayment.getUser().getId())) {
			logger.warn("IDOR attempt detected - User {} tried to remove credit card {}", user.getId(), creditCardId);
			return "badRequestPage";
		} else {
			model.addAttribute("user", user);
			userPaymentService.removeById(creditCardId);
			
			logger.info("User {} removed credit card {}", user.getId(), creditCardId);
			
			model.addAttribute("listOfCreditCards", true);
			model.addAttribute("classActiveBilling", true);
			model.addAttribute("listOfShippingAddresses", true);
			
			model.addAttribute("userPaymentList", user.getUserPaymentList());
			model.addAttribute("userShippingList", user.getUserShippingList());
			model.addAttribute("orderList", user.getOrderList());
			
			return "myProfile";
		}
	}
	
	@RequestMapping("/removeUserShipping")
	public String removeUserShipping(
			@ModelAttribute("id") Long userShippingId, Principal principal, Model model
			){
		if (principal == null) {
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			return "redirect:/login";
		}
		
		// SECURITY FIX: Validate shipping address ID
		if (userShippingId == null || userShippingId <= 0) {
			logger.warn("Invalid shipping address ID for removal: {}", userShippingId);
			return "badRequestPage";
		}
		
		UserShipping userShipping = userShippingService.findById(userShippingId);
		
		// SECURITY FIX: Enhanced IDOR protection
		if(userShipping == null || userShipping.getUser() == null || !user.getId().equals(userShipping.getUser().getId())) {
			logger.warn("IDOR attempt detected - User {} tried to remove shipping address {}", user.getId(), userShippingId);
			return "badRequestPage";
		} else {
			model.addAttribute("user", user);
			
			userShippingService.removeById(userShippingId);
			
			logger.info("User {} removed shipping address {}", user.getId(), userShippingId);
			
			model.addAttribute("listOfShippingAddresses", true);
			model.addAttribute("classActiveShipping", true);
			
			model.addAttribute("userPaymentList", user.getUserPaymentList());
			model.addAttribute("userShippingList", user.getUserShippingList());
			model.addAttribute("orderList", user.getOrderList());
			
			return "myProfile";
		}
	}
	
	@RequestMapping(value="/newUser", method = RequestMethod.POST)
	public String newUserPost(
			HttpServletRequest request,
			@ModelAttribute("email") String userEmail,
			@ModelAttribute("username") String username,
			Model model
			) throws Exception{
		model.addAttribute("classActiveNewAccount", true);
		model.addAttribute("email", userEmail);
		model.addAttribute("username", username);
		
		// SECURITY FIX: Enhanced input validation
		if (username == null || username.trim().isEmpty() || username.length() > MAX_USERNAME_LENGTH) {
			logger.warn("Invalid username provided: {}", username);
			model.addAttribute("usernameInvalid", true);
			return "myAccount";
		}
		
		if (userEmail == null || userEmail.trim().isEmpty() || userEmail.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(userEmail).matches()) {
			logger.warn("Invalid email provided: {}", userEmail);
			model.addAttribute("emailInvalid", true);
			return "myAccount";
		}
		
		if (userService.findByUsername(username) != null) {
			logger.info("Username already exists: {}", username);
			model.addAttribute("usernameExists", true);
			return "myAccount";
		}
		
		if (userService.findByEmail(userEmail) != null) {
			logger.info("Email already exists: {}", userEmail);
			model.addAttribute("emailExists", true);
			return "myAccount";
		}
		
		User user = new User();
		user.setUsername(username);
		user.setEmail(userEmail);
		
		// SECURITY FIX: Use improved password generation
		String password = generateSecurePassword();
		
		String encryptedPassword = SecurityUtility.passwordEncoder().encode(password);
		user.setPassword(encryptedPassword);
		
		Role role = new Role();
		role.setRoleId(1);
		role.setName("ROLE_USER");
		Set<UserRole> userRoles = new HashSet<>();
		userRoles.add(new UserRole(user, role));
		userService.createUser(user, userRoles);
		
		String token = UUID.randomUUID().toString();
		userService.createPasswordResetTokenForUser(user, token);
		
		String appUrl = "http://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath();
		
		try {
			SimpleMailMessage email = mailConstructor.constructResetTokenEmail(appUrl, request.getLocale(), token, user, password);
			mailSender.send(email);
			model.addAttribute("emailSent", "true");
			logger.info("New user created: {}", username);
		} catch (Exception e) {
			logger.error("Failed to send registration email to: {}", userEmail, e);
			model.addAttribute("emailError", true);
		}
		
		model.addAttribute("orderList", user.getOrderList());
		
		return "myAccount";
	}
	
	@RequestMapping("/newUser")
	public String newUser(Locale locale, @RequestParam("token") String token, Model model) {
		// SECURITY FIX: Validate token
		if (token == null || token.trim().isEmpty()) {
			logger.warn("Invalid token provided for new user activation");
			String message = "Invalid Token.";
			model.addAttribute("message", message);
			return "redirect:/badRequest";
		}
		
		PasswordResetToken passToken = userService.getPasswordResetToken(token);

		if (passToken == null) {
			logger.warn("Token not found: {}", token);
			String message = "Invalid Token.";
			model.addAttribute("message", message);
			return "redirect:/badRequest";
		}

		User user = passToken.getUser();
		
		if (user == null) {
			logger.warn("User not found for token: {}", token);
			String message = "Invalid Token.";
			model.addAttribute("message", message);
			return "redirect:/badRequest";
		}
		
		String username = user.getUsername();

		UserDetails userDetails = userSecurityService.loadUserByUsername(username);

		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(),
				userDetails.getAuthorities());
		
		SecurityContextHolder.getContext().setAuthentication(authentication);
		
		model.addAttribute("user", user);
		model.addAttribute("classActiveEdit", true);
		
		logger.info("User activated successfully: {}", username);
		
		return "myProfile";
	}
	
	@RequestMapping(value="/updateUserInfo", method=RequestMethod.POST)
	public String updateUserInfo(
			@ModelAttribute("user") User user,
			@ModelAttribute("newPassword") String newPassword,
			Model model
			) throws Exception {
		
		if (user == null || user.getId() == null) {
			logger.warn("Invalid user update attempt");
			throw new Exception("User not found");
		}
		
		User currentUser = userService.findById(user.getId());
		
		if(currentUser == null) {
			logger.warn("User not found for ID: {}", user.getId());
			throw new Exception("User not found");
		}
		
		// SECURITY FIX: Enhanced email validation
		if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
			if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
				logger.warn("Invalid email format provided: {}", user.getEmail());
				model.addAttribute("emailInvalid", true);
				return "myProfile";
			}
			
			User existingEmailUser = userService.findByEmail(user.getEmail());
			if (existingEmailUser != null && !existingEmailUser.getId().equals(currentUser.getId())) {
				logger.info("Email already exists: {}", user.getEmail());
				model.addAttribute("emailExists", true);
				return "myProfile";
			}
		}
		
		// SECURITY FIX: Enhanced username validation
		if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
			if (user.getUsername().length() > MAX_USERNAME_LENGTH) {
				logger.warn("Username too long: {}", user.getUsername());
				model.addAttribute("usernameInvalid", true);
				return "myProfile";
			}
			
			User existingUsernameUser = userService.findByUsername(user.getUsername());
			if (existingUsernameUser != null && !existingUsernameUser.getId().equals(currentUser.getId())) {
				logger.info("Username already exists: {}", user.getUsername());
				model.addAttribute("usernameExists", true);
				return "myProfile";
			}
		}
		
		// SECURITY FIX: Enhanced password update with validation
		if (newPassword != null && !newPassword.isEmpty() && !newPassword.equals("")){
			if (newPassword.length() < 8) {
				logger.warn("New password too short for user: {}", currentUser.getId());
				model.addAttribute("passwordTooShort", true);
				return "myProfile";
			}
			
			BCryptPasswordEncoder passwordEncoder = SecurityUtility.passwordEncoder();
			String dbPassword = currentUser.getPassword();
			if(passwordEncoder.matches(user.getPassword(), dbPassword)){
				currentUser.setPassword(passwordEncoder.encode(newPassword));
				logger.info("Password updated for user: {}", currentUser.getId());
			} else {
				logger.warn("Incorrect password provided for user: {}", currentUser.getId());
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

		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(),
				userDetails.getAuthorities());
		
		SecurityContextHolder.getContext().setAuthentication(authentication);
		model.addAttribute("orderList", user.getOrderList());
		
		logger.info("User info updated successfully: {}", currentUser.getId());
		
		return "myProfile";
	}
	
	@RequestMapping("/orderDetail")
	public String orderDetail(
			@RequestParam("id") Long orderId,
			Principal principal, Model model
			){
		if (principal == null) {
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			return "redirect:/login";
		}
		
		// SECURITY FIX: Validate order ID
		if (orderId == null || orderId <= 0) {
			logger.warn("Invalid order ID provided: {}", orderId);
			return "badRequestPage";
		}
		
		Order order = orderService.findOne(orderId);
		
		// SECURITY FIX: Enhanced IDOR protection
		if(order == null || order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
			logger.warn("IDOR attempt detected - User {} tried to access order {}", user.getId(), orderId);
			return "badRequestPage";
		} else {
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
}