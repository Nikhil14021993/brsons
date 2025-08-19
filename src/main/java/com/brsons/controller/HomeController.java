package com.brsons.controller;

import com.brsons.model.Product;
import com.brsons.model.User;
import com.brsons.model.AddToCart;
import com.brsons.model.CartProductEntry;
import com.brsons.repository.UserRepository;
import com.brsons.repository.AddToCartRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;

@Controller
public class HomeController {
	private final UserRepository userRepository;
	private final AddToCartRepository addToCartRepository;

    public HomeController(UserRepository userRepository, AddToCartRepository addToCartRepository) {
        this.userRepository = userRepository;
        this.addToCartRepository = addToCartRepository;
    }
	  @GetMapping("/")
	public String home(HttpSession session, Model model) {
		// Debug logging
		System.out.println("=== HOME PAGE ACCESSED ===");
		System.out.println("Session ID: " + session.getId());
		
		User sessionUser = (User) session.getAttribute("user");
		System.out.println("User from session: " + (sessionUser != null ? "User ID: " + sessionUser.getId() + ", Name: " + sessionUser.getName() : "null"));
		
		// Add user to model for display purposes
		if (sessionUser != null) {
			model.addAttribute("currentUser", sessionUser);
			
			// Set cart count in session for navbar display
			AddToCart cart = addToCartRepository.findByUserId(sessionUser.getId()).orElse(null);
			if (cart != null && !cart.getProductQuantities().isEmpty()) {
				int totalItems = cart.getProductQuantities().stream()
						.mapToInt(CartProductEntry::getQuantity)
						.sum();
				session.setAttribute("cartCount", totalItems);
			} else {
				session.setAttribute("cartCount", 0);
			}
		}
		
		return "home";
	}
}