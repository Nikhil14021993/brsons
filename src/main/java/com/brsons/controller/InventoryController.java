package com.brsons.controller;

import com.brsons.service.InventoryService;
import com.brsons.repository.ProductRepository;
import com.brsons.model.Product;
import com.brsons.model.StockMovement;
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
        
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("threshold", threshold);
        model.addAttribute("user", user);
        
        return "admin-low-stock-alerts";
    }
    
    // ==================== STOCK VALUATION ====================
    
    @GetMapping("/valuation")
    public String stockValuation(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        BigDecimal totalStockValue = inventoryService.calculateTotalStockValue();
        List<Product> allProducts = productRepository.findAll();
        
        // Calculate individual product values
        for (Product product : allProducts) {
            BigDecimal productValue = inventoryService.calculateProductStockValue(product.getId());
            // You might want to add a transient field to Product for this
        }
        
        model.addAttribute("totalStockValue", totalStockValue);
        model.addAttribute("products", allProducts);
        model.addAttribute("user", user);
        
        return "admin-stock-valuation";
    }
    
    // ==================== STOCK ADJUSTMENT ====================
    
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
                                 Model model, HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("reportType", reportType);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("user", user);
        
        return "admin-inventory-reports";
    }
}
