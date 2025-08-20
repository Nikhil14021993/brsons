package com.brsons.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import com.brsons.model.AddToCart;
import com.brsons.model.CartItemDetails;
import com.brsons.model.CartProductEntry;
import com.brsons.model.CartProductEntry1;
import com.brsons.model.Order;
import com.brsons.model.OrderItem;
import com.brsons.model.Product;
import com.brsons.model.User;
import com.brsons.repository.AddToCartRepository;
import com.brsons.repository.CartProductEntryRepo;
import com.brsons.repository.OrderItemRepository;
import com.brsons.repository.OrderRepository;
import com.brsons.repository.ProductRepository;
import com.brsons.service.CheckoutService;
// import com.brsons.service.EnhancedInvoiceService;
import com.brsons.service.OrderAccountingService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

@Controller
public class CheckoutController {

	@Autowired
    private CheckoutService checkoutService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    OrderItemRepository orderItemRepository;
    @Autowired
    OrderAccountingService orderAccountingService;
    @Autowired
    private ProductRepository productRepository;
    // Temporarily disabled EnhancedInvoiceService to fix database issues
    // @Autowired
    // EnhancedInvoiceService enhancedInvoiceService;

    @GetMapping("/checkout")
    public String checkoutPage(HttpSession session, Model model, HttpServletResponse response) throws IOException {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
   
        model.addAttribute("userPhone", user.getPhone());
        return "checkout";
    }
    
    @Autowired
    private CartProductEntryRepo cartRepo;

    // New AJAX endpoint for processing checkout
    @PostMapping("/checkout/ajax")
    public ResponseEntity<Map<String, Object>> processCheckoutAjax(@RequestBody Map<String, String> requestData, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.put("success", false);
            response.put("message", "User not logged in");
            response.put("redirect", "/login");
            return ResponseEntity.ok(response);
        }

        try {
            // Fetch cart from DB using phone number
            List<CartProductEntry1> cartItems = cartRepo.findByUserPhone(user.getPhone());
            if (cartItems == null || cartItems.isEmpty()) {
                response.put("success", false);
                response.put("message", "Cart is empty");
                response.put("redirect", "/cart");
                return ResponseEntity.ok(response);
            }

            // Check stock availability for all items
            List<Map<String, Object>> stockIssues = new ArrayList<>();
            for (CartProductEntry1 cartItem : cartItems) {
                Product product = productRepository.findById(cartItem.getProductId()).orElse(null);
                if (product != null) {
                    if (product.getStockQuantity() == null || product.getStockQuantity() < cartItem.getQuantity()) {
                        Map<String, Object> issue = new HashMap<>();
                        issue.put("productName", product.getProductName());
                        issue.put("requestedQuantity", cartItem.getQuantity());
                        issue.put("availableStock", product.getStockQuantity() != null ? product.getStockQuantity() : 0);
                        stockIssues.add(issue);
                    }
                }
            }

            if (!stockIssues.isEmpty()) {
                response.put("success", false);
                response.put("message", "Insufficient stock for some products");
                response.put("stockIssues", stockIssues);
                return ResponseEntity.ok(response);
            }

            // If we reach here, all products have sufficient stock
            // Process the actual checkout
            String name = requestData.get("name");
            String addressLine1 = requestData.get("addressLine1");
            String addressLine2 = requestData.get("addressLine2");
            String city = requestData.get("city");
            String state = requestData.get("state");
            String zipCode = requestData.get("zipCode");
            String billType = requestData.get("billType");
            String buyerGstin = requestData.get("buyerGstin");

            // Create Order
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

            // Create Order Items with price information and stock management
            List<OrderItem> orderItems = new ArrayList<>();
            for (CartProductEntry1 cartItem : cartItems) {
                Product product = productRepository.findById(cartItem.getProductId()).orElse(null);
                if (product != null) {
                    OrderItem item = new OrderItem();
                    item.setProductId(cartItem.getProductId());
                    item.setQuantity(cartItem.getQuantity());
                    item.setOrder(order);
                    
                    // Store the actual price at order time based on user type
                    BigDecimal unitPrice;
                    String priceType;
                    
                    if ("B2B".equalsIgnoreCase(user.getType()) && product.getB2bPrice() != null) {
                        unitPrice = BigDecimal.valueOf(product.getB2bPrice());
                        priceType = "b2b";
                    } else {
                        // Use retail price for retail users and admin users
                        unitPrice = BigDecimal.valueOf(product.getRetailPrice() != null ? product.getRetailPrice() : 0.0);
                        priceType = "retail";
                    }
                    
                    item.setUnitPrice(unitPrice);
                    item.setUserType(user.getType());
                    item.setPriceType(priceType);
                    item.calculateTotalPrice();
                    
                    orderItems.add(item);
                    
                    // Reduce stock quantity
                    int newStockQuantity = product.getStockQuantity() - cartItem.getQuantity();
                    product.setStockQuantity(newStockQuantity);
                    
                    // Update product status to "Out of Stock" if stock becomes 0
                    if (newStockQuantity <= 0) {
                        product.setStatus("Out of Stock");
                    }
                    
                    // Save updated product
                    productRepository.save(product);
                }
            }
           
            order.setOrderItems(orderItems);
            order.setBillType(billType != null ? billType : "Pakka");
            order.setBuyerGstin(buyerGstin);
            
            // Save order with items
            orderRepository.save(order);
            
            // Finalize GST + invoice + ledger with dynamic pricing based on user type
            orderAccountingService.finalizeTotalsAndInvoice(order, new BigDecimal("5.00"), order.getBillType(), user.getType());
            
            // Clear cart
            checkoutService.clearCart(user.getPhone());
            
            response.put("success", true);
            response.put("message", "Order placed successfully!");
            response.put("redirect", "/orders/" + order.getId() + "/invoice");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing checkout: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    // Keep the original method for backward compatibility
    @PostMapping("/checkout")
    public String processCheckout(@RequestParam String name,
                                  @RequestParam String addressLine1,
                                  @RequestParam String addressLine2,
                                  @RequestParam String city,
                                  @RequestParam String state,
                                  @RequestParam String zipCode,
                                  @RequestParam(required = false) String billType,
                                  @RequestParam(required = false) String buyerGstin,
                                  HttpSession session,  HttpServletResponse response) throws IOException {

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
        
        

        // ✅ Create Order Items with price information and stock management
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartProductEntry1 cartItem : cartItems) {
            // Get product to determine pricing and check stock
            Product product = productRepository.findById(cartItem.getProductId()).orElse(null);
            if (product != null) {
                // Check if product has sufficient stock
                if (product.getStockQuantity() == null || product.getStockQuantity() < cartItem.getQuantity()) {
                    // Insufficient stock - redirect back with error
                    return "redirect:/cart?error=Insufficient+stock+for+product+" + product.getProductName();
                }
                
                OrderItem item = new OrderItem();
                item.setProductId(cartItem.getProductId());
                item.setQuantity(cartItem.getQuantity());
                item.setOrder(order);
                
                // Store the actual price at order time based on user type
                BigDecimal unitPrice;
                String priceType;
                
                if ("B2B".equalsIgnoreCase(user.getType()) && product.getB2bPrice() != null) {
                    unitPrice = BigDecimal.valueOf(product.getB2bPrice());
                    priceType = "b2b";
                } else {
                    // Use retail price for retail users and admin users
                    unitPrice = BigDecimal.valueOf(product.getRetailPrice() != null ? product.getRetailPrice() : 0.0);
                    priceType = "retail";
                }
                
                item.setUnitPrice(unitPrice);
                item.setUserType(user.getType());
                item.setPriceType(priceType);
                item.calculateTotalPrice(); // Calculate total price
                
                orderItems.add(item);
                
                // Reduce stock quantity
                int newStockQuantity = product.getStockQuantity() - cartItem.getQuantity();
                product.setStockQuantity(newStockQuantity);
                
                // Update product status to "Out of Stock" if stock becomes 0
                if (newStockQuantity <= 0) {
                    product.setStatus("Out of Stock");
                }
                
                // Save updated product
                productRepository.save(product);
            }
        }
       
        order.setOrderItems(orderItems);
        order.setBillType(billType != null ? billType : "Pakka");
        order.setBuyerGstin(buyerGstin);
        // ✅ Save order with items
        orderRepository.save(order);
        
        // ✅ Finalize GST + invoice + ledger with dynamic pricing based on user type
        orderAccountingService.finalizeTotalsAndInvoice(order, new BigDecimal("5.00"), order.getBillType(), user.getType());
        
        // Temporarily disabled invoice generation to fix database issues
        // TODO: Re-enable after database schema is updated

        // ✅ Mark cart items checked_out = 'yes' (you already added this)
        //checkoutService.markCheckedOut(user.getPhone());

        // ✅ Optionally clear cart from DB
       // cartRepo.deleteAll(cartItems);
        
        // ✅ Clear cart and redirect
        checkoutService.clearCart(user.getPhone());
        return "redirect:/orders/" + order.getId() + "/invoice";

        //return "redirect:/?success=Order+placed+successfully!+Invoice+has+been+generated.";
    }

}

