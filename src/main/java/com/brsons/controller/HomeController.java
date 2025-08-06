package com.brsons.controller;

import com.brsons.model.Product;
import com.brsons.model.User;
import com.brsons.repository.UserRepository;

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

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
	  @GetMapping("/")
	    public String home(@RequestParam(name = "userId", required = false) Long userId,
	                       HttpSession session,
	                       Model model) {

	        User sessionUser = (User) session.getAttribute("user");

	        if (userId != null && (sessionUser == null || !userId.equals(sessionUser.getId()))) {
	            // Fetch and store user in session if userId is passed
	            Optional<User> optionalUser = userRepository.findById(userId);
	            optionalUser.ifPresent(user -> session.setAttribute("user", user));
	        }
	   // List<Product> featured = productList.subList(0, Math.min(productList.size(), 3));
	   // model.addAttribute("featured", featured);
	    return "home";
	}
}