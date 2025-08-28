package com.brsons.controller;

import com.brsons.service.InventoryService;
import com.brsons.repository.ProductRepository;
import com.brsons.model.Product;
import com.brsons.model.StockMovement;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/inventory")
public class InventoryController {
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private ProductRepository productRepository;
    
    // ==================== INVENTORY DASHBOARD ====================
    
    @GetMapping("/dashboard")
    public String inventoryDashboard(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Get inventory statistics
        BigDecimal totalStockValue = inventoryService.calculateTotalStockValue();
        List<Product> lowStockProducts = inventoryService.getLowStockProducts(10); // Below 10 units
        List<Product> outOfStockProducts = inventoryService.getOutOfStockProducts();
        
        // Get recent stock movements (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<StockMovement> recentMovements = inventoryService.getDashboardMovements(10); // Get last 10 movements
        
        model.addAttribute("totalStockValue", totalStockValue);
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("outOfStockProducts", outOfStockProducts);
        model.addAttribute("recentMovements", recentMovements);
        model.addAttribute("user", user);
        
        return "admin-inventory-dashboard";
    }
    
    // ==================== STOCK MOVEMENTS ====================
    
    @GetMapping("/movements")
    public String stockMovements(@RequestParam(required = false) Long productId,
                                @RequestParam(required = false) String movementType,
                                @RequestParam(required = false) String startDate,
                                @RequestParam(required = false) String endDate,
                                Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<StockMovement> movements;
        
        if (productId != null) {
            movements = inventoryService.getStockMovements(productId);
        } else {
            // Get all movements or filtered by date
            movements = inventoryService.getStockMovements(null); // Will need to implement this
        }
        
        // Get all products for filter dropdown
        List<Product> products = productRepository.findAll();
        
        model.addAttribute("movements", movements);
        model.addAttribute("products", products);
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("selectedMovementType", movementType);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("user", user);
        
        return "admin-stock-movements";
    }
    
    // ==================== LOW STOCK ALERTS ====================
    
    @GetMapping("/low-stock")
    public String lowStockAlerts(@RequestParam(defaultValue = "10") int threshold,
                                Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Product> lowStockProducts = inventoryService.getLowStockProducts(threshold);
        List<Product> outOfStockProducts = inventoryService.getOutOfStockProducts();
        List<Product> criticalStockProducts = inventoryService.getLowStockProducts(5); // Below 5 units
        List<Product> allLowStockProducts = new ArrayList<>();
        allLowStockProducts.addAll(outOfStockProducts);
        allLowStockProducts.addAll(lowStockProducts);
        
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("outOfStockProducts", outOfStockProducts);
        model.addAttribute("criticalStockProducts", criticalStockProducts);
        model.addAttribute("allLowStockProducts", allLowStockProducts);
        model.addAttribute("threshold", threshold);
        model.addAttribute("user", user);
        
        return "admin-low-stock-alerts";
    }
    
    // ==================== STOCK VALUATION ====================
    
    @GetMapping("/valuation")
    public String stockValuation(@RequestParam(required = false) String category,
                                @RequestParam(required = false) BigDecimal minValue,
                                @RequestParam(required = false) BigDecimal maxValue,
                                Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        BigDecimal totalStockValue = inventoryService.calculateTotalStockValue();
        List<Product> allProducts = productRepository.findAll();
        
        // Filter products if needed
        List<Product> filteredProducts = allProducts;
        if (category != null && !category.isEmpty()) {
            filteredProducts = allProducts.stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(Collectors.toList());
        }
        
        // Calculate additional metrics
        BigDecimal averageProductValue = totalStockValue.divide(BigDecimal.valueOf(allProducts.size()), 2, BigDecimal.ROUND_HALF_UP);
        int totalStockQuantity = allProducts.stream().mapToInt(p -> p.getStockQuantity() != null ? p.getStockQuantity() : 0).sum();
        
        // Get unique categories
        List<String> categories = allProducts.stream()
            .map(p -> p.getCategory() != null ? p.getCategory().getCategoryName() : "Uncategorized")
            .filter(c -> c != null && !c.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        
        // Generate chart data
        Map<String, BigDecimal> categoryChartData = generateCategoryChartData(allProducts);
        Map<String, BigDecimal> topProductsChartData = generateTopProductsChartData(allProducts);
        
        model.addAttribute("totalStockValue", totalStockValue);
        model.addAttribute("averageProductValue", averageProductValue);
        model.addAttribute("totalStockQuantity", totalStockQuantity);
        model.addAttribute("products", filteredProducts);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("minValue", minValue);
        model.addAttribute("maxValue", maxValue);
        model.addAttribute("categoryChartData", categoryChartData);
        model.addAttribute("topProductsChartData", topProductsChartData);
        model.addAttribute("user", user);
        
        return "admin-stock-valuation";
    }
    
    // ==================== STOCK ADJUSTMENT ====================
    
    @GetMapping("/adjust-stock")
    public String showAdjustStockForm(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Product> products = productRepository.findAll();
        
        // Convert products to JSON for JavaScript
        ObjectMapper objectMapper = new ObjectMapper();
        String productsJson = "[]";
        try {
            productsJson = objectMapper.writeValueAsString(products);
        } catch (Exception e) {
            // Handle exception
        }
        
        model.addAttribute("products", products);
        model.addAttribute("productsJson", productsJson);
        model.addAttribute("user", user);
        
        return "admin-adjust-stock";
    }
    
    @PostMapping("/adjust-stock")
    @ResponseBody
    public Map<String, Object> adjustStock(@RequestParam Long productId,
                                          @RequestParam int quantity,
                                          @RequestParam String reason,
                                          @RequestParam String adjustmentType,
                                          HttpSession session) {
        try {
            // Check if user is logged in and is admin
            Object user = session.getAttribute("user");
            if (user == null) {
                return Map.of("success", false, "message", "Unauthorized");
            }
            
            if ("increase".equals(adjustmentType)) {
                inventoryService.increaseStock(productId, quantity, reason, "MANUAL_ADJUSTMENT", null);
            } else if ("decrease".equals(adjustmentType)) {
                inventoryService.decreaseStock(productId, quantity, reason, "MANUAL_ADJUSTMENT", null);
            } else {
                return Map.of("success", false, "message", "Invalid adjustment type");
            }
            
            return Map.of("success", true, "message", "Stock adjusted successfully");
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }
    
    // ==================== INVENTORY REPORTS ====================
    
    @GetMapping("/reports")
    public String inventoryReports(@RequestParam(required = false) String reportType,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate,
                                 @RequestParam(required = false) String category,
                                 Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Get all products for category extraction
        List<Product> allProducts = productRepository.findAll();
        List<String> categories = allProducts.stream()
            .map(p -> p.getCategory() != null ? p.getCategory().getCategoryName() : "Uncategorized")
            .filter(c -> c != null && !c.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        
        model.addAttribute("reportType", reportType);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("categories", categories);
        model.addAttribute("user", user);
        
        return "admin-inventory-reports";
    }
    
    // Helper methods for chart data generation
    private Map<String, BigDecimal> generateCategoryChartData(List<Product> products) {
        Map<String, BigDecimal> categoryData = new HashMap<>();
        
        for (Product product : products) {
            String categoryName = product.getCategory() != null ? 
                product.getCategory().getCategoryName() : "Uncategorized";
            
            BigDecimal currentValue = categoryData.getOrDefault(categoryName, BigDecimal.ZERO);
            BigDecimal productValue = product.getPrice() != null ? 
                product.getPrice().multiply(BigDecimal.valueOf(product.getStockQuantity() != null ? product.getStockQuantity() : 0)) : 
                BigDecimal.ZERO;
            
            categoryData.put(categoryName, currentValue.add(productValue));
        }
        
        return categoryData;
    }
    
    private Map<String, BigDecimal> generateTopProductsChartData(List<Product> products) {
        return products.stream()
            .sorted((p1, p2) -> {
                BigDecimal val1 = p1.getPrice() != null ? 
                    p1.getPrice().multiply(BigDecimal.valueOf(p1.getStockQuantity() != null ? p1.getStockQuantity() : 0)) : 
                    BigDecimal.ZERO;
                BigDecimal val2 = p2.getPrice() != null ? 
                    p2.getPrice().multiply(BigDecimal.valueOf(p2.getStockQuantity() != null ? p2.getStockQuantity() : 0)) : 
                    BigDecimal.ZERO;
                return val2.compareTo(val1); // Descending order
            })
            .limit(10)
            .collect(Collectors.toMap(
                Product::getProductName,
                p -> p.getPrice() != null ? 
                    p.getPrice().multiply(BigDecimal.valueOf(p.getStockQuantity() != null ? p.getStockQuantity() : 0)) : 
                    BigDecimal.ZERO
            ));
    }
}
