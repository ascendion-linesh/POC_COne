package com.bookstore.controller;

import java.security.Principal;
import java.util.List;

import jakarta.validation.Valid;

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

@Controller
@RequestMapping("/shoppingCart")
public class ShoppingCartController {
    
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
            @ModelAttribute("book") @Valid Book book,
            @RequestParam("qty") String qty,
            Model model, Principal principal
    ) {
        User user = userService.findByUsername(principal.getName());
        book = bookService.findOne(book.getId());
        
        if (book == null) {
            model.addAttribute("bookNotFound", true);
            return "redirect:/bookshelf";
        }
        
        int quantity;
        try {
            quantity = Integer.parseInt(qty);
            if (quantity <= 0) {
                model.addAttribute("invalidQuantity", true);
                return "forward:/bookDetail?id="+book.getId();
            }
        } catch (NumberFormatException e) {
            model.addAttribute("invalidQuantity", true);
            return "forward:/bookDetail?id="+book.getId();
        }
        
        if (quantity > book.getInStockNumber()) {
            model.addAttribute("notEnoughStock", true);
            return "forward:/bookDetail?id="+book.getId();
        }
        
        CartItem cartItem = cartItemService.addBookToCartItem(book, user, quantity);
        model.addAttribute("addBookSuccess", true);
        
        return "forward:/bookDetail?id="+book.getId();
    }
    
    @RequestMapping("/updateCartItem")
    public String updateShoppingCart(
            @RequestParam("id") Long cartItemId,
            @RequestParam("qty") int qty
    ) {
        if (qty <= 0) {
            return "redirect:/shoppingCart/cart";
        }
        
        CartItem cartItem = cartItemService.findById(cartItemId);
        
        if (cartItem == null) {
            return "redirect:/shoppingCart/cart";
        }
        
        cartItem.setQty(qty);
        cartItemService.updateCartItem(cartItem);
        
        return "forward:/shoppingCart/cart";
    }
    
    @RequestMapping("/removeItem")
    public String removeItem(@RequestParam("id") Long id) {
        CartItem cartItem = cartItemService.findById(id);
        
        if (cartItem != null) {
            cartItemService.removeCartItem(cartItem);
        }
        
        return "forward:/shoppingCart/cart";
    }
}