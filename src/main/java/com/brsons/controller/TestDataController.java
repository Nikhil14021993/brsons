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
import java.util.List;

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
    
    @GetMapping("/test/debug-products")
    @ResponseBody
    public String debugProducts() {
        try {
            List<Product> products = productRepository.findByStatus("Active");
            StringBuilder result = new StringBuilder();
            result.append("Found ").append(products.size()).append(" active products:\n");
            
            for (Product product : products) {
                result.append("ID: ").append(product.getId())
                      .append(", Name: ").append(product.getProductName())
                      .append(", Price: ").append(product.getRetailPrice())
                      .append(", Stock: ").append(product.getStockQuantity())
                      .append(", SKU: ").append(product.getSku())
                      .append("\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            return "Error debugging products: " + e.getMessage();
        }
    }
    
    @GetMapping("/test/add-sample-products")
    @ResponseBody
    public String addSampleProducts() {
        try {
            // Create a category first if it doesn't exist
            Category category = categoryRepository.findByCategoryName("Clothing")
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setCategoryName("Clothing");
                    newCategory.setStatus("Active");
                    return categoryRepository.save(newCategory);
                });
            
            // Create sample products
            String[] productNames = {
                "Cotton T-Shirt", "Denim Jeans", "Polo Shirt", "Hoodie", 
                "Cargo Pants", "Sweater", "Shorts", "Jacket"
            };
            
            Double[] retailPrices = {299.0, 1299.0, 599.0, 899.0, 1099.0, 799.0, 399.0, 1499.0};
            Double[] b2bPrices = {250.0, 1100.0, 500.0, 750.0, 900.0, 650.0, 320.0, 1200.0};
            Integer[] stockQuantities = {50, 25, 30, 20, 15, 18, 35, 10};
            String[] skus = {"TSH-001", "JNS-001", "POL-001", "HOD-001", "CAR-001", "SWT-001", "SHT-001", "JCK-001"};
            
            int productsCreated = 0;
            for (int i = 0; i < productNames.length; i++) {
                // Check if product already exists
                if (productRepository.findByProductNameContainingIgnoreCase(productNames[i]).isEmpty()) {
                    Product product = new Product();
                    product.setProductName(productNames[i]);
                    product.setDescription("Sample " + productNames[i] + " for testing");
                    product.setRetailPrice(retailPrices[i]);
                    product.setB2bPrice(b2bPrices[i]);
                    product.setPurchasePrice(retailPrices[i] * 0.5); // 50% of retail price
                    product.setB2bMinQuantity(5);
                    product.setDiscount(0.0);
                    product.setStockQuantity(stockQuantities[i]);
                    product.setReservedQuantity(0);
                    product.setStatus("Active");
                    product.setCategory(category);
                    product.setSku(skus[i]);
                    
                    // Set default tax configuration for sample products
                    product.setCgstPercentage(new java.math.BigDecimal("9.00")); // 9% CGST
                    product.setSgstPercentage(new java.math.BigDecimal("9.00")); // 9% SGST
                    product.setIgstPercentage(new java.math.BigDecimal("18.00")); // 18% IGST
                    
                    productRepository.save(product);
                    productsCreated++;
                }
            }
            
            return "Sample products created successfully! Created " + productsCreated + " new products. " + 
                   (productNames.length - productsCreated) + " products already existed.";
            
        } catch (Exception e) {
            return "Error adding sample products: " + e.getMessage();
        }
    }
    
}
