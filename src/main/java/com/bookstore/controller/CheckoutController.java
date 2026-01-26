package com.bookstore.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.bookstore.domain.BillingAddress;
import com.bookstore.domain.CartItem;
import com.bookstore.domain.Order;
import com.bookstore.domain.Payment;
import com.bookstore.domain.ShippingAddress;
import com.bookstore.domain.ShoppingCart;
import com.bookstore.domain.User;
import com.bookstore.domain.UserBilling;
import com.bookstore.domain.UserPayment;
import com.bookstore.domain.UserShipping;
import com.bookstore.service.BillingAddressService;
import com.bookstore.service.CartItemService;
import com.bookstore.service.OrderService;
import com.bookstore.service.PaymentService;
import com.bookstore.service.ShippingAddressService;
import com.bookstore.service.ShoppingCartService;
import com.bookstore.service.UserPaymentService;
import com.bookstore.service.UserService;
import com.bookstore.service.UserShippingService;
import com.bookstore.utility.MailConstructor;
import com.bookstore.utility.USConstants;

import jakarta.mail.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class CheckoutController {

	private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

	private ShippingAddress shippingAddress = new ShippingAddress();
	private BillingAddress billingAddress = new BillingAddress();
	private Payment payment = new Payment();

	@Autowired
	private JavaMailSender mailSender;
	
	@Autowired
	private MailConstructor mailConstructor;
	
	@Autowired
	private UserService userService;

	@Autowired
	private CartItemService cartItemService;
	
	@Autowired
	private ShoppingCartService shoppingCartService;

	@Autowired
	private ShippingAddressService shippingAddressService;

	@Autowired
	private BillingAddressService billingAddressService;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private UserShippingService userShippingService;

	@Autowired
	private UserPaymentService userPaymentService;
	
	@Autowired
	private OrderService orderService;

	@RequestMapping("/checkout")
	public String checkout(@RequestParam("id") Long cartId,
			@RequestParam(value = "missingRequiredField", required = false) boolean missingRequiredField, Model model,
			Principal principal) {
		
		if (principal == null) {
			logger.warn("Unauthorized checkout attempt - no principal");
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			logger.warn("User not found for principal: {}", principal.getName());
			return "redirect:/login";
		}

		// SECURITY FIX: Enhanced IDOR protection with proper validation
		if (cartId == null || user.getShoppingCart() == null || !cartId.equals(user.getShoppingCart().getId())) {
			logger.warn("IDOR attempt detected - User {} tried to access cart {}", user.getId(), cartId);
			return "badRequestPage";
		}

		List<CartItem> cartItemList = cartItemService.findByShoppingCart(user.getShoppingCart());

		if (cartItemList.size() == 0) {
			model.addAttribute("emptyCart", true);
			return "forward:/shoppingCart/cart";
		}

		for (CartItem cartItem : cartItemList) {
			if (cartItem.getBook().getInStockNumber() < cartItem.getQty()) {
				model.addAttribute("notEnoughStock", true);
				return "forward:/shoppingCart/cart";
			}
		}

		List<UserShipping> userShippingList = user.getUserShippingList();
		List<UserPayment> userPaymentList = user.getUserPaymentList();

		model.addAttribute("userShippingList", userShippingList);
		model.addAttribute("userPaymentList", userPaymentList);

		if (userPaymentList.size() == 0) {
			model.addAttribute("emptyPaymentList", true);
		} else {
			model.addAttribute("emptyPaymentList", false);
		}

		if (userShippingList.size() == 0) {
			model.addAttribute("emptyShippingList", true);
		} else {
			model.addAttribute("emptyShippingList", false);
		}

		ShoppingCart shoppingCart = user.getShoppingCart();

		for (UserShipping userShipping : userShippingList) {
			if (userShipping.isUserShippingDefault()) {
				shippingAddressService.setByUserShipping(userShipping, shippingAddress);
			}
		}

		for (UserPayment userPayment : userPaymentList) {
			if (userPayment.isDefaultPayment()) {
				paymentService.setByUserPayment(userPayment, payment);
				billingAddressService.setByUserBilling(userPayment.getUserBilling(), billingAddress);
			}
		}

		model.addAttribute("shippingAddress", shippingAddress);
		model.addAttribute("payment", payment);
		model.addAttribute("billingAddress", billingAddress);
		model.addAttribute("cartItemList", cartItemList);
		model.addAttribute("shoppingCart", user.getShoppingCart());

		List<String> stateList = USConstants.listOfUSStatesCode;
		Collections.sort(stateList);
		model.addAttribute("stateList", stateList);

		model.addAttribute("classActiveShipping", true);

		if (missingRequiredField) {
			model.addAttribute("missingRequiredField", true);
		}

		return "checkout";
	}

	@RequestMapping(value = "/checkout", method = RequestMethod.POST)
	public String checkoutPost(@ModelAttribute("shippingAddress") ShippingAddress shippingAddress,
			@ModelAttribute("billingAddress") BillingAddress billingAddress, @ModelAttribute("payment") Payment payment,
			@ModelAttribute("billingSameAsShipping") String billingSameAsShipping,
			@ModelAttribute("shippingMethod") String shippingMethod, Principal principal, Model model) {
		
		if (principal == null) {
			logger.warn("Unauthorized checkout POST attempt");
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			logger.warn("User not found for principal: {}", principal.getName());
			return "redirect:/login";
		}
		
		ShoppingCart shoppingCart = user.getShoppingCart();

		List<CartItem> cartItemList = cartItemService.findByShoppingCart(shoppingCart);
		model.addAttribute("cartItemList", cartItemList);

		// SECURITY FIX: Input validation for billingSameAsShipping
		if (billingSameAsShipping != null && billingSameAsShipping.equals("true")) {
			billingAddress.setBillingAddressName(shippingAddress.getShippingAddressName());
			billingAddress.setBillingAddressStreet1(shippingAddress.getShippingAddressStreet1());
			billingAddress.setBillingAddressStreet2(shippingAddress.getShippingAddressStreet2());
			billingAddress.setBillingAddressCity(shippingAddress.getShippingAddressCity());
			billingAddress.setBillingAddressState(shippingAddress.getShippingAddressState());
			billingAddress.setBillingAddressCountry(shippingAddress.getShippingAddressCountry());
			billingAddress.setBillingAddressZipcode(shippingAddress.getShippingAddressZipcode());
		}

		// SECURITY FIX: Enhanced input validation with null checks
		if (shippingAddress == null || billingAddress == null || payment == null ||
				shippingAddress.getShippingAddressStreet1() == null || shippingAddress.getShippingAddressStreet1().trim().isEmpty() 
				|| shippingAddress.getShippingAddressCity() == null || shippingAddress.getShippingAddressCity().trim().isEmpty()
				|| shippingAddress.getShippingAddressState() == null || shippingAddress.getShippingAddressState().trim().isEmpty()
				|| shippingAddress.getShippingAddressName() == null || shippingAddress.getShippingAddressName().trim().isEmpty()
				|| shippingAddress.getShippingAddressZipcode() == null || shippingAddress.getShippingAddressZipcode().trim().isEmpty() 
				|| payment.getCardNumber() == null || payment.getCardNumber().trim().isEmpty()
				|| payment.getCvc() == 0 
				|| billingAddress.getBillingAddressStreet1() == null || billingAddress.getBillingAddressStreet1().trim().isEmpty()
				|| billingAddress.getBillingAddressCity() == null || billingAddress.getBillingAddressCity().trim().isEmpty() 
				|| billingAddress.getBillingAddressState() == null || billingAddress.getBillingAddressState().trim().isEmpty()
				|| billingAddress.getBillingAddressName() == null || billingAddress.getBillingAddressName().trim().isEmpty()
				|| billingAddress.getBillingAddressZipcode() == null || billingAddress.getBillingAddressZipcode().trim().isEmpty()) {
			
			logger.warn("Missing required fields in checkout for user: {}", user.getId());
			return "redirect:/checkout?id=" + shoppingCart.getId() + "&missingRequiredField=true";
		}
		
		// SECURITY FIX: Validate shipping method
		if (shippingMethod == null || (!shippingMethod.equals("groundShipping") && !shippingMethod.equals("premiumShipping"))) {
			logger.warn("Invalid shipping method: {} for user: {}", shippingMethod, user.getId());
			return "redirect:/checkout?id=" + shoppingCart.getId() + "&missingRequiredField=true";
		}
		
		Order order = orderService.createOrder(shoppingCart, shippingAddress, billingAddress, payment, shippingMethod, user);
		
		try {
			mailSender.send(mailConstructor.constructOrderConfirmationEmail(user, order, Locale.ENGLISH));
		} catch (Exception e) {
			logger.error("Failed to send order confirmation email for order: {}", order.getId(), e);
			// Continue processing even if email fails
		}
		
		shoppingCartService.clearShoppingCart(shoppingCart);
		
		LocalDate today = LocalDate.now();
		LocalDate estimatedDeliveryDate;
		
		if (shippingMethod.equals("groundShipping")) {
			estimatedDeliveryDate = today.plusDays(5);
		} else {
			estimatedDeliveryDate = today.plusDays(3);
		}
		
		model.addAttribute("estimatedDeliveryDate", estimatedDeliveryDate);
		
		logger.info("Order {} successfully created for user: {}", order.getId(), user.getId());
		
		return "orderSubmittedPage";
	}

	@RequestMapping("/setShippingAddress")
	public String setShippingAddress(@RequestParam("userShippingId") Long userShippingId, Principal principal,
			Model model) {
		
		if (principal == null) {
			logger.warn("Unauthorized setShippingAddress attempt");
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			logger.warn("User not found for principal: {}", principal.getName());
			return "redirect:/login";
		}
		
		if (userShippingId == null) {
			logger.warn("Null userShippingId provided by user: {}", user.getId());
			return "badRequestPage";
		}
		
		UserShipping userShipping = userShippingService.findById(userShippingId);

		// SECURITY FIX: Enhanced IDOR protection with proper validation
		if (userShipping == null || userShipping.getUser() == null || !userShipping.getUser().getId().equals(user.getId())) {
			logger.warn("IDOR attempt detected - User {} tried to access shipping address {}", user.getId(), userShippingId);
			return "badRequestPage";
		} else {
			shippingAddressService.setByUserShipping(userShipping, shippingAddress);

			List<CartItem> cartItemList = cartItemService.findByShoppingCart(user.getShoppingCart());

			model.addAttribute("shippingAddress", shippingAddress);
			model.addAttribute("payment", payment);
			model.addAttribute("billingAddress", billingAddress);
			model.addAttribute("cartItemList", cartItemList);
			model.addAttribute("shoppingCart", user.getShoppingCart());

			List<String> stateList = USConstants.listOfUSStatesCode;
			Collections.sort(stateList);
			model.addAttribute("stateList", stateList);

			List<UserShipping> userShippingList = user.getUserShippingList();
			List<UserPayment> userPaymentList = user.getUserPaymentList();

			model.addAttribute("userShippingList", userShippingList);
			model.addAttribute("userPaymentList", userPaymentList);

			model.addAttribute("shippingAddress", shippingAddress);

			model.addAttribute("classActiveShipping", true);

			if (userPaymentList.size() == 0) {
				model.addAttribute("emptyPaymentList", true);
			} else {
				model.addAttribute("emptyPaymentList", false);
			}

			model.addAttribute("emptyShippingList", false);

			return "checkout";
		}
	}

	@RequestMapping("/setPaymentMethod")
	public String setPaymentMethod(@RequestParam("userPaymentId") Long userPaymentId, Principal principal,
			Model model) {
		
		if (principal == null) {
			logger.warn("Unauthorized setPaymentMethod attempt");
			return "redirect:/login";
		}
		
		User user = userService.findByUsername(principal.getName());
		
		if (user == null) {
			logger.warn("User not found for principal: {}", principal.getName());
			return "redirect:/login";
		}
		
		if (userPaymentId == null) {
			logger.warn("Null userPaymentId provided by user: {}", user.getId());
			return "badRequestPage";
		}
		
		UserPayment userPayment = userPaymentService.findById(userPaymentId);
		
		if (userPayment == null) {
			logger.warn("UserPayment not found: {}", userPaymentId);
			return "badRequestPage";
		}
		
		UserBilling userBilling = userPayment.getUserBilling();

		// SECURITY FIX: Enhanced IDOR protection with proper validation
		if (userPayment.getUser() == null || !userPayment.getUser().getId().equals(user.getId())) {
			logger.warn("IDOR attempt detected - User {} tried to access payment method {}", user.getId(), userPaymentId);
			return "badRequestPage";
		} else {
			paymentService.setByUserPayment(userPayment, payment);

			List<CartItem> cartItemList = cartItemService.findByShoppingCart(user.getShoppingCart());

			billingAddressService.setByUserBilling(userBilling, billingAddress);

			model.addAttribute("shippingAddress", shippingAddress);
			model.addAttribute("payment", payment);
			model.addAttribute("billingAddress", billingAddress);
			model.addAttribute("cartItemList", cartItemList);
			model.addAttribute("shoppingCart", user.getShoppingCart());

			List<String> stateList = USConstants.listOfUSStatesCode;
			Collections.sort(stateList);
			model.addAttribute("stateList", stateList);

			List<UserShipping> userShippingList = user.getUserShippingList();
			List<UserPayment> userPaymentList = user.getUserPaymentList();

			model.addAttribute("userShippingList", userShippingList);
			model.addAttribute("userPaymentList", userPaymentList);

			model.addAttribute("shippingAddress", shippingAddress);

			model.addAttribute("classActivePayment", true);

			model.addAttribute("emptyPaymentList", false);

			if (userShippingList.size() == 0) {
				model.addAttribute("emptyShippingList", true);
			} else {
				model.addAttribute("emptyShippingList", false);
			}

			return "checkout";
		}
	}
}