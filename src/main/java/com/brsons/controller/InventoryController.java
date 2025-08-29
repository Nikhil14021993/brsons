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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
        BigDecimal averageProductValue = BigDecimal.ZERO;
        if (!allProducts.isEmpty()) {
            // Only calculate average if we have products with stock
            List<Product> productsWithStock = allProducts.stream()
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 0)
                .collect(Collectors.toList());
            
            if (!productsWithStock.isEmpty()) {
                averageProductValue = totalStockValue.divide(BigDecimal.valueOf(productsWithStock.size()), 2, BigDecimal.ROUND_HALF_UP);
            }
        }
        
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
    
    // ==================== EXPORT ENDPOINTS ====================
    
    @GetMapping("/valuation/export/excel")
    public ResponseEntity<byte[]> exportValuationToExcel(@RequestParam(required = false) String category,
                                                        @RequestParam(required = false) BigDecimal minValue,
                                                        @RequestParam(required = false) BigDecimal maxValue,
                                                        HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            List<Product> allProducts = productRepository.findAll();
            
            // Apply filters
            List<Product> filteredProducts = allProducts;
            if (category != null && !category.isEmpty()) {
                filteredProducts = allProducts.stream()
                    .filter(p -> category.equals(p.getCategory() != null ? p.getCategory().getCategoryName() : "Uncategorized"))
                    .collect(Collectors.toList());
            }
            
            if (minValue != null) {
                filteredProducts = filteredProducts.stream()
                    .filter(p -> getProductValue(p).compareTo(minValue) >= 0)
                    .collect(Collectors.toList());
            }
            
            if (maxValue != null) {
                filteredProducts = filteredProducts.stream()
                    .filter(p -> getProductValue(p).compareTo(maxValue) <= 0)
                    .collect(Collectors.toList());
            }
            
            byte[] excelContent = generateExcelReport(filteredProducts);
            
            String filename = "stock_valuation_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelContent);
                
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    @GetMapping("/valuation/export/csv")
    public ResponseEntity<byte[]> exportValuationToCSV(@RequestParam(required = false) String category,
                                                      @RequestParam(required = false) BigDecimal minValue,
                                                      @RequestParam(required = false) BigDecimal maxValue,
                                                      HttpSession session) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            List<Product> allProducts = productRepository.findAll();
            
            // Apply filters
            List<Product> filteredProducts = allProducts;
            if (category != null && !category.isEmpty()) {
                filteredProducts = allProducts.stream()
                    .filter(p -> category.equals(p.getCategory() != null ? p.getCategory().getCategoryName() : "Uncategorized"))
                    .collect(Collectors.toList());
            }
            
            if (minValue != null) {
                filteredProducts = filteredProducts.stream()
                    .filter(p -> getProductValue(p).compareTo(minValue) >= 0)
                    .collect(Collectors.toList());
            }
            
            if (maxValue != null) {
                filteredProducts = filteredProducts.stream()
                    .filter(p -> getProductValue(p).compareTo(maxValue) <= 0)
                    .collect(Collectors.toList());
            }
            
            String csvContent = generateCSVReport(filteredProducts);
            
            String filename = "stock_valuation_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(csvContent.getBytes(StandardCharsets.UTF_8));
                
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private BigDecimal getProductValue(Product product) {
        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        BigDecimal price = getProductPrice(product);
        return price.multiply(BigDecimal.valueOf(stock));
    }
    
    private BigDecimal getProductPrice(Product product) {
        // First try the main price field
        if (product.getPrice() != null && product.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return product.getPrice();
        }
        
        // Fallback to purchase price
        if (product.getPurchasePrice() != null && product.getPurchasePrice() > 0) {
            return BigDecimal.valueOf(product.getPurchasePrice());
        }
        
        // Fallback to retail price
        if (product.getRetailPrice() != null && product.getRetailPrice() > 0) {
            return BigDecimal.valueOf(product.getRetailPrice());
        }
        
        // Fallback to b2b price
        if (product.getB2bPrice() != null && product.getB2bPrice() > 0) {
            return BigDecimal.valueOf(product.getB2bPrice());
        }
        
        // Default to 0 if no price is available
        return BigDecimal.ZERO;
    }
    
    private byte[] generateExcelReport(List<Product> products) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Stock Valuation Report");
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            
            // Create data style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.LEFT);
            
            // Create currency style
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setAlignment(HorizontalAlignment.RIGHT);
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("₹#,##0.00"));
            
            // Create date style
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setAlignment(HorizontalAlignment.CENTER);
            DataFormat dateFormat = workbook.createDataFormat();
            dateStyle.setDataFormat(dateFormat.getFormat("dd/mm/yyyy"));
            
            // Create headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Product Name", "Product ID", "Category", "Stock Quantity", "Unit Price", "Stock Value", "Last Updated"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 15 * 256); // Set column width
            }
            
            // Add data rows
            int rowNum = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(product.getProductName());
                row.createCell(1).setCellValue(product.getId() != null ? String.valueOf(product.getId()) : "");
                row.createCell(2).setCellValue(product.getCategory() != null ? product.getCategory().getCategoryName() : "Uncategorized");
                
                Cell stockCell = row.createCell(3);
                stockCell.setCellValue(product.getStockQuantity() != null ? product.getStockQuantity() : 0);
                stockCell.setCellStyle(dataStyle);
                
                Cell priceCell = row.createCell(4);
                BigDecimal price = getProductPrice(product);
                priceCell.setCellValue(price.doubleValue());
                priceCell.setCellStyle(currencyStyle);
                
                Cell valueCell = row.createCell(5);
                BigDecimal value = getProductValue(product);
                valueCell.setCellValue(value.doubleValue());
                valueCell.setCellStyle(currencyStyle);
                
                Cell dateCell = row.createCell(6);
                if (product.getLastUpdated() != null) {
                    dateCell.setCellValue(product.getLastUpdated().toLocalDate());
                    dateCell.setCellStyle(dateStyle);
                }
            }
            
            // Add summary row
            Row summaryRow = sheet.createRow(rowNum + 1);
            Cell summaryLabel = summaryRow.createCell(0);
            summaryLabel.setCellValue("TOTAL STOCK VALUE:");
            summaryLabel.setCellStyle(headerStyle);
            
            BigDecimal totalValue = products.stream()
                .map(this::getProductValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Cell totalValueCell = summaryRow.createCell(5);
            totalValueCell.setCellValue(totalValue.doubleValue());
            totalValueCell.setCellStyle(currencyStyle);
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
    
    private String generateCSVReport(List<Product> products) {
        StringBuilder csv = new StringBuilder();
        
        // Add headers
        csv.append("Product Name,Product ID,Category,Stock Quantity,Unit Price,Stock Value,Last Updated\n");
        
        // Add data rows
        for (Product product : products) {
            csv.append("\"").append(product.getProductName() != null ? product.getProductName().replace("\"", "\"\"") : "").append("\",");
            csv.append("\"").append(product.getId() != null ? String.valueOf(product.getId()).replace("\"", "\"\"") : "").append("\",");
            csv.append("\"").append(product.getCategory() != null ? product.getCategory().getCategoryName().replace("\"", "\"\"") : "Uncategorized").append("\",");
            csv.append(product.getStockQuantity() != null ? product.getStockQuantity() : 0).append(",");
            
            BigDecimal price = getProductPrice(product);
            csv.append(price.toString()).append(",");
            
            BigDecimal value = getProductValue(product);
            csv.append(value.toString()).append(",");
            
            if (product.getLastUpdated() != null) {
                csv.append(product.getLastUpdated().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
            csv.append("\n");
        }
        
        // Add summary
        BigDecimal totalValue = products.stream()
            .map(this::getProductValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        csv.append("\nTOTAL STOCK VALUE,").append(totalValue.toString()).append("\n");
        
        return csv.toString();
    }
    
    // ==================== UTILITY ENDPOINTS ====================
    
    @PostMapping("/update-product-prices")
    public String updateProductPrices(HttpSession session, RedirectAttributes redirectAttributes) {
        // Check if user is logged in and is admin
        Object user = session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            inventoryService.updateProductPricesForExistingProducts();
            redirectAttributes.addFlashAttribute("successMessage", "Product prices updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating product prices: " + e.getMessage());
        }
        
        return "redirect:/admin/inventory/valuation";
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
