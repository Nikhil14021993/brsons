package com.brsons.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.brsons.model.AddToCart;
import com.brsons.model.CartItemDetails;
import com.brsons.model.CartProductEntry;
import com.brsons.model.CartProductEntry1;
import com.brsons.model.Order;
import com.brsons.model.OrderItem;
import com.brsons.model.User;
import com.brsons.repository.AddToCartRepository;
import com.brsons.repository.CartProductEntryRepo;
import com.brsons.repository.OrderItemRepository;
import com.brsons.repository.OrderRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

@Controller
public class CheckoutController {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    OrderItemRepository orderItemRepository;

    @GetMapping("/checkout")
    public String checkoutPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("userPhone", user.getPhone());
        return "checkout";
    }
    
    @Autowired
    private CartProductEntryRepo cartRepo;

    

    @PostMapping("/checkout")
    public String processCheckout(@RequestParam String name,
                                  @RequestParam String addressLine1,
                                  @RequestParam String addressLine2,
                                  @RequestParam String city,
                                  @RequestParam String state,
                                  @RequestParam String zipCode,
                                  HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        // ✅ Fetch cart from DB using phone number
        List<CartProductEntry1> cartItems = cartRepo.findByUserPhone(user.getPhone());
        if (cartItems == null || cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        // ✅ Create Order
        Order order = new Order();
        order.setName(name);
        order.setUserPhone(user.getPhone());
        order.setAddressLine1(addressLine1);
        order.setAddressLine2(addressLine2);
        order.setCity(city);
        order.setState(state);
        order.setZipCode(zipCode);
        order.setStatus("Active");
        order.setOrderStatus("Not Confirmed");

        // ✅ Create Order Items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartProductEntry1 cartItem : cartItems) {
            OrderItem item = new OrderItem();
            item.setProductId(cartItem.getProductId());
            item.setQuantity(cartItem.getQuantity());
            item.setOrder(order);
            orderItems.add(item);
        }

        order.setOrderItems(orderItems);

        // ✅ Save order with items
        orderRepository.save(order);

        // ✅ Optionally clear cart from DB
       // cartRepo.deleteAll(cartItems);

        return "redirect:/";
    }

}

