package com.bookstore.controller;

import com.bookstore.domain.Book;
import com.bookstore.domain.CartItem;
import com.bookstore.domain.ShoppingCart;
import com.bookstore.domain.User;
import com.bookstore.service.BookService;
import com.bookstore.service.CartItemService;
import com.bookstore.service.ShoppingCartService;
import com.bookstore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

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

    @GetMapping("/cart")
    public String shoppingCart(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        ShoppingCart shoppingCart = user.getShoppingCart();

        List<CartItem> cartItemList = cartItemService.findByShoppingCart(shoppingCart);

        shoppingCartService.updateShoppingCart(shoppingCart);

        model.addAttribute("cartItemList", cartItemList);
        model.addAttribute("shoppingCart", shoppingCart);

        return "shoppingCart";
    }

    @PostMapping("/addItem")
    public String addItem(@ModelAttribute("book") Book book,
                         @ModelAttribute("qty") String qty,
                         Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        book = bookService.findOne(book.getId());

        if (Integer.parseInt(qty) > book.getInStockNumber()) {
            model.addAttribute("notEnoughStock", true);
            return "forward:/bookDetail?id=" + book.getId();
        }

        CartItem cartItem = cartItemService.addBookToCartItem(book, user, Integer.parseInt(qty));
        model.addAttribute("addBookSuccess", true);

        return "forward:/bookDetail?id=" + book.getId();
    }

    @GetMapping("/updateCartItem")
    public String updateShoppingCart(@RequestParam("id") Long cartItemId,
                                    @RequestParam("qty") int qty,
                                    Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        CartItem cartItem = cartItemService.findById(cartItemId);

        if (cartItem.getUser().getId() != user.getId()) {
            return "badRequestPage";
        }

        if (qty > cartItem.getBook().getInStockNumber()) {
            model.addAttribute("notEnoughStock", true);
            return "forward:/shoppingCart/cart";
        }

        cartItem.setQty(qty);
        cartItemService.updateCartItem(cartItem);

        return "forward:/shoppingCart/cart";
    }

    @GetMapping("/removeItem")
    public String removeItem(@RequestParam("id") Long id, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        CartItem cartItem = cartItemService.findById(id);

        if (cartItem.getUser().getId() != user.getId()) {
            return "badRequestPage";
        }

        cartItemService.removeCartItem(cartItem);

        return "forward:/shoppingCart/cart";
    }
}