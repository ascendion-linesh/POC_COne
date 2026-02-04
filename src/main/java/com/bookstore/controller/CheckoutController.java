package com.bookstore.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
	public String checkout(
			@RequestParam("id") Long cartId,
			@RequestParam(value = "missingRequiredField", required = false) boolean missingRequiredField, 
			Model model,
			Principal principal
	) {
		if(cartId == null || cartId <= 0) {
			logger.warn("Invalid cart ID: {}", cartId);
			return "badRequestPage";
		}
		
		User user = userService.findByUsername(principal.getName());

		if (!cartId.equals(user.getShoppingCart().getId())) {
			logger.warn("Unauthorized checkout attempt - cart {} by user {}", 
				cartId, user.getId());
			return "badRequestPage";
		}

		List<CartItem> cartItemList = cartItemService.findByShoppingCart(user.getShoppingCart());

		if (cartItemList.isEmpty()) {
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

		model.addAttribute("emptyPaymentList", userPaymentList.isEmpty());
		model.addAttribute("emptyShippingList", userShippingList.isEmpty());

		ShoppingCart shoppingCart = user.getShoppingCart();

		userShippingList.stream()
			.filter(UserShipping::isUserShippingDefault)
			.findFirst()
			.ifPresent(userShipping -> 
				shippingAddressService.setByUserShipping(userShipping, shippingAddress)
			);

		userPaymentList.stream()
			.filter(UserPayment::isDefaultPayment)
			.findFirst()
			.ifPresent(userPayment -> {
				paymentService.setByUserPayment(userPayment, payment);
				billingAddressService.setByUserBilling(userPayment.getUserBilling(), billingAddress);
			});

		model.addAttribute("shippingAddress", shippingAddress);
		model.addAttribute("payment", payment);
		model.addAttribute("billingAddress", billingAddress);
		model.addAttribute("cartItemList", cartItemList);
		model.addAttribute("shoppingCart", shoppingCart);

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
	public String checkoutPost(
			@Valid @ModelAttribute("shippingAddress") ShippingAddress shippingAddress,
			@Valid @ModelAttribute("billingAddress") BillingAddress billingAddress, 
			@Valid @ModelAttribute("payment") Payment payment,
			@RequestParam("billingSameAsShipping") String billingSameAsShipping,
			@RequestParam("shippingMethod") @NotBlank(message = "Shipping method is required") String shippingMethod, 
			BindingResult result,
			Principal principal, 
			Model model
	) {
		if(result.hasErrors()) {
			model.addAttribute("validationErrors", true);
			return "checkout";
		}
		
		User user = userService.findByUsername(principal.getName());
		ShoppingCart shoppingCart = user.getShoppingCart();

		List<CartItem> cartItemList = cartItemService.findByShoppingCart(shoppingCart);
		model.addAttribute("cartItemList", cartItemList);

		if ("true".equalsIgnoreCase(billingSameAsShipping)) {
			billingAddress.setBillingAddressName(shippingAddress.getShippingAddressName());
			billingAddress.setBillingAddressStreet1(shippingAddress.getShippingAddressStreet1());
			billingAddress.setBillingAddressStreet2(shippingAddress.getShippingAddressStreet2());
			billingAddress.setBillingAddressCity(shippingAddress.getShippingAddressCity());
			billingAddress.setBillingAddressState(shippingAddress.getShippingAddressState());
			billingAddress.setBillingAddressCountry(shippingAddress.getShippingAddressCountry());
			billingAddress.setBillingAddressZipcode(shippingAddress.getShippingAddressZipcode());
		}

		if (shippingAddress.getShippingAddressStreet1() == null || shippingAddress.getShippingAddressStreet1().trim().isEmpty() 
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
			
			logger.warn("Missing required checkout fields for user {}", user.getId());
			return "redirect:/checkout?id=" + shoppingCart.getId() + "&missingRequiredField=true";
		}
		
		if (!shippingMethod.equals("groundShipping") && !shippingMethod.equals("premiumShipping")) {
			logger.warn("Invalid shipping method: {}", shippingMethod);
			return "redirect:/checkout?id=" + shoppingCart.getId() + "&missingRequiredField=true";
		}
		
		try {
			Order order = orderService.createOrder(
				shoppingCart, 
				shippingAddress, 
				billingAddress, 
				payment, 
				shippingMethod, 
				user
			);
			
			mailSender.send(mailConstructor.constructOrderConfirmationEmail(user, order, Locale.ENGLISH));
			shoppingCartService.clearShoppingCart(shoppingCart);
			
			LocalDate today = LocalDate.now();
			LocalDate estimatedDeliveryDate;
			
			if ("groundShipping".equals(shippingMethod)) {
				estimatedDeliveryDate = today.plusDays(5);
			} else {
				estimatedDeliveryDate = today.plusDays(3);
			}
			
			model.addAttribute("estimatedDeliveryDate", estimatedDeliveryDate);
			logger.info("Order {} created successfully for user {}", order.getId(), user.getId());
			
		} catch (Exception e) {
			logger.error("Failed to create order for user {}", user.getId(), e);
			model.addAttribute("error", "Failed to process order. Please try again.");
			return "checkout";
		}
		
		return "orderSubmittedPage";
	}

	@RequestMapping("/setShippingAddress")
	public String setShippingAddress(
			@RequestParam("userShippingId") Long userShippingId, 
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

		if (!userShipping.getUser().getId().equals(user.getId())) {
			logger.warn("Unauthorized access to shipping address {} by user {}", 
				userShippingId, user.getId());
			return "badRequestPage";
		}
		
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
		model.addAttribute("emptyPaymentList", userPaymentList.isEmpty());
		model.addAttribute("emptyShippingList", false);

		return "checkout";
	}

	@RequestMapping("/setPaymentMethod")
	public String setPaymentMethod(
			@RequestParam("userPaymentId") Long userPaymentId, 
			Principal principal,
			Model model
	) {
		if(userPaymentId == null || userPaymentId <= 0) {
			logger.warn("Invalid payment ID: {}", userPaymentId);
			return "badRequestPage";
		}
		
		User user = userService.findByUsername(principal.getName());
		UserPayment userPayment = userPaymentService.findById(userPaymentId);
		
		if(userPayment == null) {
			logger.warn("Payment method not found: {}", userPaymentId);
			return "badRequestPage";
		}
		
		UserBilling userBilling = userPayment.getUserBilling();

		if (!userPayment.getUser().getId().equals(user.getId())) {
			logger.warn("Unauthorized access to payment method {} by user {}", 
				userPaymentId, user.getId());
			return "badRequestPage";
		}
		
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
		model.addAttribute("emptyShippingList", userShippingList.isEmpty());

		return "checkout";
	}
}