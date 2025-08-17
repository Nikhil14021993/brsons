package com.brsons.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.brsons.model.User;
import com.brsons.service.OrderService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;

@Controller
public class OrderController {
	
	 @PersistenceContext
	    private EntityManager entityManager;
@Autowired
OrderService orderService;
	
	@GetMapping("/orders")
	public String viewOrders(HttpSession session, Model model) {
	    User user = (User) session.getAttribute("user");
	    if (user == null) return "redirect:/login";
	    model.addAttribute("orders", orderService.getOrdersByUserPhone(user.getPhone()));
	    return "orders";
	}
	
	@GetMapping("/orders/{orderId}")
	public String getOrderDetails(@PathVariable Long orderId, Model model) {
	    // Fetch order item details (photo, quantity, price, subtotal)
		List<Object[]> items = entityManager.createQuery(
		        "SELECT p.id, p.mainPhoto, i.quantity, p.price, (i.quantity * p.price) " +
		        "FROM OrderItem i JOIN Product p ON i.productId = p.id " +
		        "WHERE i.order.id = :orderId", Object[].class)
		        .setParameter("orderId", orderId)
		        .getResultList();

	    model.addAttribute("items", items);

	    // Calculate subtotal
	    Double subtotal = items.stream()
	            .mapToDouble(r -> ((Number) r[4]).doubleValue())
	            .sum();

	    model.addAttribute("subtotal", subtotal);
	    model.addAttribute("orderId", orderId);

	    return "order-details"; // Thymeleaf page
	}
}
