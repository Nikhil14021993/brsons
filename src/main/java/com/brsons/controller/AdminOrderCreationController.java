package com.brsons.controller;

import com.brsons.model.*;
import com.brsons.repository.*;
import com.brsons.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
// Password encoding handled by plain text storage
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/order-creation")
public class AdminOrderCreationController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private OrderService orderService;
    
    // Password stored as plain text in current system
    
    /**
     * Show the admin order creation page
     */
    @GetMapping
    public String showOrderCreationPage(HttpSession session, Model model) {
        Object user = session.getAttribute("user");
        if (user == null || !isAdmin(user)) {
            return "redirect:/login";
        }
        
        return "admin-order-creation";
    }
    
    /**
     * Search for existing users by phone or name
     */
    @PostMapping("/search-user")
    public String searchUser(@RequestParam String searchQuery, HttpSession session, Model model) {
        Object user = session.getAttribute("user");
        if (user == null || !isAdmin(user)) {
            return "redirect:/login";
        }
        
        List<User> searchResults = new ArrayList<>();
        
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            // Search by phone (exact match)
            Optional<User> userByPhone = userRepository.findByEmailOrPhone("", searchQuery.trim());
            if (userByPhone.isPresent()) {
                searchResults.add(userByPhone.get());
            }
            
            // Search by name (partial match)
            List<User> usersByName = userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCase(searchQuery.trim());
            searchResults.addAll(usersByName);
            
            // Remove duplicates
            searchResults = searchResults.stream()
                .distinct()
                .collect(Collectors.toList());
        }
        
        model.addAttribute("searchResults", searchResults);
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("showUserCreation", searchResults.isEmpty());
        
        return "admin-order-creation";
    }
    
    /**
     * Create a new user and proceed to order creation
     */
    @PostMapping("/create-user")
    public String createUserAndOrder(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String userType,
            @RequestParam String addressLine1,
            @RequestParam String addressLine2,
            @RequestParam String city,
            @RequestParam String state,
            @RequestParam String zipCode,
            @RequestParam(required = false) String buyerGstin,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        Object user = session.getAttribute("user");
        if (user == null || !isAdmin(user)) {
            return "redirect:/login";
        }
        
        try {
            // Check if user already exists
            Optional<User> existingUser = userRepository.findByEmailOrPhone(email, phone);
            if (existingUser.isPresent()) {
                redirectAttributes.addFlashAttribute("error", "User with this email or phone already exists");
                return "redirect:/admin/order-creation";
            }
            
            // Create new user
            User newUser = new User();
            newUser.setName(name);
            newUser.setEmail(email);
            newUser.setPhone(phone);
            newUser.setType(userType);
            newUser.setPassword("Bani@123"); // Default password (plain text)
            newUser.setStatus("ACTIVE");
            newUser.setRole("USER");
            
            User savedUser = userRepository.save(newUser);
            
            // Store user info in session for order creation
            session.setAttribute("orderForUser", savedUser);
            session.setAttribute("orderAddress", Map.of(
                "addressLine1", addressLine1,
                "addressLine2", addressLine2,
                "city", city,
                "state", state,
                "zipCode", zipCode,
                "buyerGstin", buyerGstin
            ));
            session.setAttribute("adminOrderMode", true); // Flag to indicate admin is creating order
            
            redirectAttributes.addFlashAttribute("success", "User created successfully with default password: Bani@123. You can now shop normally.");
            return "redirect:/shop";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating user: " + e.getMessage());
            return "redirect:/admin/order-creation";
        }
    }
    
    /**
     * Select existing user and proceed to order creation
     */
    @PostMapping("/select-user")
    public String selectUser(@RequestParam Long userId, HttpSession session, RedirectAttributes redirectAttributes) {
        Object user = session.getAttribute("user");
        if (user == null || !isAdmin(user)) {
            return "redirect:/login";
        }
        
        Optional<User> selectedUser = userRepository.findById(userId);
        if (selectedUser.isPresent()) {
            // Store user info in session for order creation
            session.setAttribute("orderForUser", selectedUser.get());
            session.setAttribute("adminOrderMode", true); // Flag to indicate admin is creating order
            redirectAttributes.addFlashAttribute("success", "Order will be created for: " + selectedUser.get().getName() + ". You can now shop normally.");
            return "redirect:/shop";
        } else {
            redirectAttributes.addFlashAttribute("error", "Selected user not found");
            return "redirect:/admin/order-creation";
        }
    }
    
    /**
     * Process the order creation
     */
    @PostMapping("/create-order")
    public String createOrder(
            @RequestParam Map<String, String> productQuantities,
            @RequestParam String billType,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        Object user = session.getAttribute("user");
        if (user == null || !isAdmin(user)) {
            return "redirect:/login";
        }
        
        User orderForUser = (User) session.getAttribute("orderForUser");
        Map<String, String> orderAddress = (Map<String, String>) session.getAttribute("orderAddress");
        
        if (orderForUser == null) {
            redirectAttributes.addFlashAttribute("error", "No user selected for order");
            return "redirect:/admin/order-creation";
        }
        
        try {
            // Create order
            Order order = new Order();
            order.setName(orderForUser.getName());
            order.setUserPhone(orderForUser.getPhone());
            order.setBillType(billType);
            order.setStatus("Active");
            order.setOrderStatus("Confirmed");
            order.setCreatedAt(LocalDateTime.now());
            // Note: Order entity doesn't have setUpdatedAt method
            
            // Set address if available
            if (orderAddress != null) {
                order.setAddressLine1(orderAddress.get("addressLine1"));
                order.setAddressLine2(orderAddress.get("addressLine2"));
                order.setCity(orderAddress.get("city"));
                order.setState(orderAddress.get("state"));
                order.setZipCode(orderAddress.get("zipCode"));
                order.setBuyerGstin(orderAddress.get("buyerGstin"));
            }
            
            // Calculate total and create order items
            BigDecimal total = BigDecimal.ZERO;
            List<OrderItem> orderItems = new ArrayList<>();
            
            for (Map.Entry<String, String> entry : productQuantities.entrySet()) {
                if (entry.getKey().startsWith("product_")) {
                    Long productId = Long.parseLong(entry.getKey().substring(8));
                    int quantity = Integer.parseInt(entry.getValue());
                    
                    if (quantity > 0) {
                        Product product = productRepository.findById(productId).orElse(null);
                        if (product != null) {
                            // Use appropriate price based on user type
                            double unitPrice = "B2B".equals(orderForUser.getType()) ? 
                                (product.getB2bPrice() != null ? product.getB2bPrice() : 0.0) :
                                (product.getRetailPrice() != null ? product.getRetailPrice() : 0.0);
                            
                            OrderItem orderItem = new OrderItem();
                            orderItem.setProductId(productId);
                            orderItem.setQuantity(quantity);
                            orderItem.setUnitPrice(BigDecimal.valueOf(unitPrice));
                            orderItem.setOrder(order);
                            
                            orderItems.add(orderItem);
                            total = total.add(BigDecimal.valueOf(unitPrice * quantity));
                            
                            // Update stock
                            if (product.getStockQuantity() != null) {
                                product.setStockQuantity(product.getStockQuantity() - quantity);
                                productRepository.save(product);
                            }
                        }
                    }
                }
            }
            
            if (orderItems.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No products selected");
                return "redirect:/admin/order-creation/select-products";
            }
            
            order.setTotal(total);
            order.setOrderItems(orderItems);
            
            // Save order
            Order savedOrder = orderRepository.save(order);
            
            // Save order items
            for (OrderItem item : orderItems) {
                item.setOrder(savedOrder);
                orderItemRepository.save(item);
            }
            
            // Clear session data
            session.removeAttribute("orderForUser");
            session.removeAttribute("orderAddress");
            
            redirectAttributes.addFlashAttribute("success", 
                "Order created successfully for " + orderForUser.getName() + 
                " with order ID: " + savedOrder.getId());
            
            return "redirect:/admin/orders";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating order: " + e.getMessage());
            return "redirect:/admin/order-creation/select-products";
        }
    }
    
    /**
     * Cancel admin order creation and clear session data
     */
    @PostMapping("/cancel")
    public String cancelOrderCreation(HttpSession session, RedirectAttributes redirectAttributes) {
        Object user = session.getAttribute("user");
        if (user == null || !isAdmin(user)) {
            return "redirect:/login";
        }
        
        // Clear admin order mode session data
        session.removeAttribute("adminOrderMode");
        session.removeAttribute("orderForUser");
        session.removeAttribute("orderAddress");
        
        redirectAttributes.addFlashAttribute("success", "Order creation cancelled successfully");
        return "redirect:/admin/order-creation";
    }
    
    /**
     * Check if the current user is an admin
     */
    private boolean isAdmin(Object user) {
        if (user instanceof User) {
            User u = (User) user;
            return "Admin".equals(u.getType()) || "ADMIN".equals(u.getRole());
        }
        return false;
    }
}
