package com.brsons.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.brsons.model.User;
import com.brsons.service.OrderService;

import jakarta.servlet.http.HttpSession;

@Controller
public class OrderController {
	

@Autowired
OrderService orderService;
	
	@GetMapping("/orders")
	public String viewOrders(HttpSession session, Model model) {
	    User user = (User) session.getAttribute("user");
	    if (user == null) return "redirect:/login";
	    model.addAttribute("orders", orderService.getOrdersByUserPhone(user.getPhone()));
	    return "orders";
	}

}
