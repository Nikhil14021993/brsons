package com.brsons.controller;

import com.brsons.model.Order;
import com.brsons.model.OrderItem;
import com.brsons.model.Product;
import com.brsons.model.Category;
import com.brsons.repository.OrderRepository;
import com.brsons.repository.ProductRepository;
import com.brsons.repository.CategoryRepository;
import com.brsons.service.EnhancedInvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
public class TestDataController {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private EnhancedInvoiceService enhancedInvoiceService;
    
    @GetMapping("/test/add-sample-orders")
    @ResponseBody
    public String addSampleOrders() {
        try {
            // Create a category first
            Category category = new Category();
            category.setCategoryName("Test Category");
            category.setStatus("Active");
            categoryRepository.save(category);
            
            // Create a product
            Product product = new Product();
            product.setProductName("Test T-Shirt");
            product.setRetailPrice(500.0);
            product.setB2bPrice(450.0);
            product.setDescription("A test t-shirt for testing purposes");
            product.setDiscount(0.0);
            product.setStockQuantity(100);
            product.setCategory(category);
            product.setStatus("Active");
            productRepository.save(product);
            
            // Sample Order 1 with product
            Order order1 = new Order();
            order1.setInvoiceNumber("PK-2025-000001");
            order1.setName("Rahul Kumar");
            order1.setUserPhone("+91 98765 43210");
            order1.setState("Maharashtra");
            order1.setBillType("Pakka");
            order1.setTotal(new BigDecimal("2500.00"));
            order1.setSubTotal(new BigDecimal("2380.95"));
            order1.setGstRate(new BigDecimal("5.00"));
            order1.setGstAmount(new BigDecimal("119.05"));
            order1.setOrderStatus("Pending");
            order1.setCreatedAt(LocalDateTime.now().minusDays(2));
            
            // Create order item
            OrderItem orderItem1 = new OrderItem();
            orderItem1.setProductId(product.getId());
            orderItem1.setQuantity(5);
            orderItem1.setOrder(order1);
            
            order1.getOrderItems().add(orderItem1);
            orderRepository.save(order1);
            
            return "Sample order with product added successfully! Order ID: " + order1.getId();
            
        } catch (Exception e) {
            return "Error adding sample orders: " + e.getMessage();
        }
    }
    
    @GetMapping("/test/test-checkout-invoice")
    @ResponseBody
    public String testCheckoutInvoice() {
        try {
            // Get the first order to test with
            Order order = orderRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No orders found"));
            
            // Test the checkout invoice generation
            byte[] pdfContent = enhancedInvoiceService.generateInvoiceAtCheckout(order);
            
            // Save the order with the generated invoice
            orderRepository.save(order);
            
            return "Checkout invoice generation test successful! Generated PDF size: " + pdfContent.length + " bytes. Invoice stored in order.";
            
        } catch (Exception e) {
            return "Error testing checkout invoice generation: " + e.getMessage();
        }
    }
    
}
