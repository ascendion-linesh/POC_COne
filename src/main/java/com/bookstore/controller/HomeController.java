package com.bookstore.controller;

import com.bookstore.domain.*;
import com.bookstore.service.*;
import com.bookstore.utility.SecurityUtility;
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
import org.springframework.web.bind.annotation.*;
import com.bookstore.service.impl.UserSecurityService;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.*;

@Controller
public class HomeController {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserService userService;

    @Autowired
    private UserSecurityService userSecurityService;

    @Autowired
    private UserPaymentService userPaymentService;

    @Autowired
    private UserShippingService userShippingService;

    @Autowired
    private CartItemService cartItemService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private BookService bookService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("classActiveLogin", true);
        return "myAccount";
    }

    @GetMapping("/bookshelf")
    public String bookshelf(Model model, Principal principal) {
        if (principal != null) {
            String username = principal.getName();
            User user = userService.findByUsername(username);
            model.addAttribute("user", user);
        }

        List<Book> bookList = bookService.findAll();
        model.addAttribute("bookList", bookList);
        model.addAttribute("activeAll", true);

        return "bookshelf";
    }

    @GetMapping("/bookDetail")
    public String bookDetail(@RequestParam("id") Long id, Model model, Principal principal) {
        if (principal != null) {
            String username = principal.getName();
            User user = userService.findByUsername(username);
            model.addAttribute("user", user);
        }

        Book book = bookService.findById(id).orElse(null);
        if (book == null) {
            return "redirect:/bookshelf";
        }

        model.addAttribute("book", book);

        List<Integer> qtyList = Arrays.asList(1,2,3,4,5,6,7,8,9,10);
        model.addAttribute("qtyList", qtyList);
        model.addAttribute("qty", 1);

        return "bookDetail";
    }

    @GetMapping("/forgetPassword")
    public String forgetPassword(Model model) {
        model.addAttribute("classActiveForgetPassword", true);
        return "myAccount";
    }

    @PostMapping("/forgetPassword")
    public String forgetPasswordPost(
            HttpServletRequest request,
            @ModelAttribute("email") String email,
            Model model) {

        model.addAttribute("classActiveForgetPassword", true);
        
        User user = userService.findByEmail(email);
        
        if (user == null) {
            model.addAttribute("emailNotExist", true);
            return "myAccount";
        }
        
        String password = SecurityUtility.randomString();
        String encryptedPassword = SecurityUtility.passwordEncoder().encode(password);
        user.setPassword(encryptedPassword);
        
        userService.save(user);
        
        String token = UUID.randomUUID().toString();
        userService.createPasswordResetTokenForUser(user, token);
        
        String appUrl = "http://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath();
        
        SimpleMailMessage newEmail = new SimpleMailMessage();
        newEmail.setTo(user.getEmail());
        newEmail.setSubject("Le's Bookstore - Password Reset");
        newEmail.setText("\nPlease click on this link to verify your email and edit your personal information. Your password is: \n"+password+"\n"+appUrl+"/newUser?token="+token);
        newEmail.setFrom("support@bookstore.com");
        
        mailSender.send(newEmail);
        
        model.addAttribute("forgetPasswordEmailSent", "true");
        
        return "myAccount";
    }

    @PostMapping("/newUser")
    public String newUserPost(
            HttpServletRequest request,
            @ModelAttribute("email") String userEmail,
            @ModelAttribute("username") String username,
            Model model) throws Exception {

        model.addAttribute("classActiveNewAccount", true);
        model.addAttribute("email", userEmail);
        model.addAttribute("username", username);

        if (userService.findByUsername(username) != null) {
            model.addAttribute("usernameExists", true);
            return "myAccount";
        }

        if (userService.findByEmail(userEmail) != null) {
            model.addAttribute("emailExists", true);
            return "myAccount";
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(userEmail);

        String password = SecurityUtility.randomString();
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

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(user.getEmail());
        email.setSubject("Le's Bookstore - New User");
        email.setText("\nPlease click on this link to verify your email and edit your personal information. Your password is: \n"+password+"\n"+appUrl+"/newUser?token="+token);
        email.setFrom("support@bookstore.com");

        mailSender.send(email);

        model.addAttribute("emailSent", "true");
        model.addAttribute("orderList", user.getOrderList());

        return "myAccount";
    }

    @GetMapping("/newUser")
    public String newUser(@RequestParam("token") String token, Model model) {
        PasswordResetToken passToken = userService.getPasswordResetToken(token);

        if (passToken == null) {
            String message = "Invalid Token.";
            model.addAttribute("message", message);
            return "redirect:/badRequest";
        }

        User user = passToken.getUser();
        String username = user.getUsername();

        UserDetails userDetails = userSecurityService.loadUserByUsername(username);

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(),
                userDetails.getAuthorities());
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        model.addAttribute("user", user);
        model.addAttribute("classActiveEdit", true);
        
        return "myProfile";
    }

    @GetMapping("/myProfile")
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

    @GetMapping("/faq")
    public String faq() {
        return "faq";
    }

    @GetMapping("/hours")
    public String hours() {
        return "hours";
    }
}