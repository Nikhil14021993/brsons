


package com.brsons.controller;

import com.brsons.model.*;
import com.brsons.service.SupplierService;
import com.brsons.service.PurchaseOrderService;
import com.brsons.repository.ProductRepository;
import com.brsons.repository.SupplierRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/admin/business")
public class BusinessManagementController {
    
    @Autowired
    private SupplierService supplierService;
    
    @Autowired
    private PurchaseOrderService purchaseOrderService;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private com.brsons.util.PurchaseOrderUtils poUtils;
    
    @Autowired
    private SupplierRepository supplierRepository;
    // ==================== SUPPLIER MANAGEMENT ====================
    
    @GetMapping("/suppliers")
    public String viewSuppliers(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        SupplierService.SupplierStatistics stats = supplierService.getSupplierStatistics();
        
        // Get unique cities for filter dropdown
        List<String> cities = supplierService.getAllSuppliers().stream()
                .map(Supplier::getCity)
                .filter(city -> city != null && !city.trim().isEmpty())
                .distinct()
                .sorted()
                .toList();
        
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("supplierStats", stats);
        model.addAttribute("cities", cities);
        model.addAttribute("user", user);
        
        return "admin-suppliers";
    }
    
    @GetMapping("/suppliers/new")
    public String showAddSupplierForm(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        model.addAttribute("supplier", new Supplier());
        model.addAttribute("user", user);
        model.addAttribute("supplierStatuses", Supplier.SupplierStatus.values());
        
        return "admin-add-supplier";
    }
    
    @PostMapping("/suppliers/new")
    public String addSupplier(@ModelAttribute Supplier supplier, 
                            HttpSession session, 
                            RedirectAttributes redirectAttributes) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                return "redirect:/login";
            }
            
            supplier.setCreatedBy(user.getName());
            Supplier savedSupplier = supplierService.createSupplier(supplier);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Supplier '" + savedSupplier.getCompanyName() + "' created successfully!");
            
            return "redirect:/admin/business/suppliers";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error creating supplier: " + e.getMessage());
            return "redirect:/admin/business/suppliers/new";
        }
    }
    
    @GetMapping("/suppliers/edit/{id}")
    public String showEditSupplierForm(@PathVariable Long id, Model model, HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        Optional<Supplier> supplier = supplierService.getSupplierById(id);
        if (supplier.isPresent()) {
            model.addAttribute("supplier", supplier.get());
            model.addAttribute("user", user);
            model.addAttribute("supplierStatuses", Supplier.SupplierStatus.values());
            return "admin-edit-supplier";
        }
        
        return "redirect:/admin/business/suppliers";
    }
    
    @PostMapping("/suppliers/edit/{id}")
    public String updateSupplier(@PathVariable Long id, 
                               @ModelAttribute Supplier supplier, 
                               HttpSession session, 
                               RedirectAttributes redirectAttributes) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                return "redirect:/login";
            }
            
            Supplier updatedSupplier = supplierService.updateSupplier(id, supplier);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Supplier '" + updatedSupplier.getCompanyName() + "' updated successfully!");
            
            return "redirect:/admin/business/suppliers";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error updating supplier: " + e.getMessage());
            return "redirect:/admin/business/suppliers/edit/" + id;
        }
    }
    
    @PostMapping("/suppliers/status/{id}")
    @ResponseBody
    public Map<String, Object> updateSupplierStatus(@PathVariable Long id, 
                                                   @RequestParam String status, 
                                                   HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                return Map.of("success", false, "message", "Unauthorized");
            }
            
            Supplier.SupplierStatus newStatus = Supplier.SupplierStatus.valueOf(status);
            supplierService.updateSupplierStatus(id, newStatus);
            
            return Map.of("success", true, "message", "Supplier status updated successfully");
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/suppliers/search")
    public String searchSuppliers(@RequestParam(required = false) String query, 
                                Model model, 
                                HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        List<Supplier> suppliers = supplierService.searchSuppliers(query);
        SupplierService.SupplierStatistics stats = supplierService.getSupplierStatistics();
        
        // Get unique cities for filter dropdown
        List<String> cities = supplierService.getAllSuppliers().stream()
                .map(Supplier::getCity)
                .filter(city -> city != null && !city.trim().isEmpty())
                .distinct()
                .sorted()
                .toList();
        
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("supplierStats", stats);
        model.addAttribute("cities", cities);
        model.addAttribute("user", user);
        model.addAttribute("searchQuery", query);
        
        return "admin-suppliers";
    }
    
    @GetMapping("/suppliers/filter")
    public String filterSuppliers(@RequestParam(required = false) String status,
                                @RequestParam(required = false) String city,
                                @RequestParam(required = false) String state,
                                @RequestParam(required = false) String country,
                                @RequestParam(required = false) Integer minRating,
                                Model model, 
                                HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        
        // Apply filters
        if (status != null && !status.isEmpty()) {
            try {
                Supplier.SupplierStatus statusEnum = Supplier.SupplierStatus.valueOf(status);
                suppliers = suppliers.stream()
                    .filter(s -> s.getStatus() == statusEnum)
                    .toList();
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore filter
            }
        }
        
        if (city != null && !city.isEmpty()) {
            suppliers = suppliers.stream()
                .filter(s -> city.equalsIgnoreCase(s.getCity()))
                .toList();
        }
        
        if (state != null && !state.isEmpty()) {
            suppliers = suppliers.stream()
                .filter(s -> state.equalsIgnoreCase(s.getState()))
                .toList();
        }
        
        if (country != null && !country.isEmpty()) {
            suppliers = suppliers.stream()
                .filter(s -> country.equalsIgnoreCase(s.getCountry()))
                .toList();
        }
        
        if (minRating != null && minRating > 0) {
            suppliers = suppliers.stream()
                .filter(s -> s.getRating() != null && s.getRating() >= minRating)
                .toList();
        }
        
        SupplierService.SupplierStatistics stats = supplierService.getSupplierStatistics();
        
        // Get unique cities for filter dropdown
        List<String> cities = supplierService.getAllSuppliers().stream()
                .map(Supplier::getCity)
                .filter(cityName -> cityName != null && !cityName.trim().isEmpty())
                .distinct()
                .sorted()
                .toList();
        
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("supplierStats", stats);
        model.addAttribute("cities", cities);
        model.addAttribute("user", user);
        model.addAttribute("filters", Map.of(
            "status", status,
            "city", city,
            "state", state,
            "country", country,
            "minRating", minRating
        ));
        
        return "admin-suppliers";
    }
    
    // ==================== PURCHASE ORDER MANAGEMENT ====================
    
    @GetMapping("/purchase-orders")
    public String viewPurchaseOrders(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        List<PurchaseOrder> purchaseOrders = purchaseOrderService.getAllPurchaseOrders();
        PurchaseOrderService.PurchaseOrderStatistics stats = purchaseOrderService.getPurchaseOrderStatistics();
        
        model.addAttribute("purchaseOrders", purchaseOrders);
        model.addAttribute("poStats", stats);
        model.addAttribute("user", user);
        
        // Add utility class to model for Thymeleaf access
        model.addAttribute("poUtils", poUtils);
        
        return "admin-purchase-orders";
    }
    
    @GetMapping("/purchase-orders/new")
    public String showAddPurchaseOrderForm(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        List<Supplier> suppliers = supplierService.getActiveSuppliers();
        List<Product> products = productRepository.findAll();
        
        // Convert products to JSON for JavaScript
        ObjectMapper objectMapper = new ObjectMapper();
        String productsJson = "[]";
        try {
            List<Map<String, Object>> productsData = new ArrayList<>();
            for (Product product : products) {
                Map<String, Object> productMap = new HashMap<>();
                productMap.put("id", product.getId());
                productMap.put("name", product.getProductName());
                productsData.add(productMap);
            }
            productsJson = objectMapper.writeValueAsString(productsData);
        } catch (Exception e) {
            // If JSON conversion fails, use empty array
            productsJson = "[]";
        }
        
        model.addAttribute("purchaseOrder", new PurchaseOrder());
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("products", products);
        model.addAttribute("productsJson", productsJson);
        model.addAttribute("user", user);
        model.addAttribute("poStatuses", PurchaseOrder.POStatus.values());
        
        return "admin-add-purchase-order";
    }
    
    @PostMapping("/purchase-orders/new")
    public String addPurchaseOrder(@RequestParam(required = false) String poNumber,
                                 @RequestParam(required = false) Long supplierId,
                                 @RequestParam(required = false) String expectedDeliveryDate,
                                 @RequestParam(required = false) String paymentTerms,
                                 @RequestParam(required = false) String deliveryAddress,
                                 @RequestParam(required = false) String notes,
                                 @RequestParam(required = false) String[] productIds,
                                 @RequestParam(required = false) Integer[] orderedQuantities,
                                 @RequestParam(required = false) BigDecimal[] unitPrices,
                                 @RequestParam(required = false) BigDecimal[] discountPercentages,
                                 @RequestParam(required = false) BigDecimal[] taxPercentages,
                                 @RequestParam(required = false) String[] itemNotes,
                                 HttpSession session, 
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                return "redirect:/login";
            }
            
            // Validate required fields
            if (supplierId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Supplier is required");
                return "redirect:/admin/business/purchase-orders/new";
            }
            
            if (productIds == null || productIds.length == 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "At least one product is required");
                return "redirect:/admin/business/purchase-orders/new";
            }
            
            // Create PurchaseOrder object
            PurchaseOrder purchaseOrder = new PurchaseOrder();
            purchaseOrder.setPoNumber(poNumber);
            
            // Set supplier
            Optional<Supplier> supplierOpt = supplierService.getSupplierById(supplierId);
            if (supplierOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Invalid supplier selected");
                return "redirect:/admin/business/purchase-orders/new";
            }
            Supplier supplier = supplierOpt.get();
            purchaseOrder.setSupplier(supplier);
            
            // Convert date string to LocalDateTime if provided
            if (expectedDeliveryDate != null && !expectedDeliveryDate.trim().isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(expectedDeliveryDate);
                    LocalDateTime dateTime = date.atStartOfDay();
                    purchaseOrder.setExpectedDeliveryDate(dateTime);
                } catch (Exception e) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Invalid delivery date format. Please use YYYY-MM-DD format.");
                    return "redirect:/admin/business/purchase-orders/new";
                }
            }
            
            purchaseOrder.setPaymentTerms(paymentTerms);
            purchaseOrder.setDeliveryAddress(deliveryAddress);
            purchaseOrder.setNotes(notes);
            purchaseOrder.setCreatedBy(user.getName());
            
            // Create order items
            List<PurchaseOrderItem> orderItems = new ArrayList<>();
            for (int i = 0; i < productIds.length; i++) {
                if (productIds[i] != null && !productIds[i].trim().isEmpty()) {
                    PurchaseOrderItem item = new PurchaseOrderItem();
                    
                    // Set product
                    Product product = productRepository.findById(Long.parseLong(productIds[i])).orElse(null);
                    if (product == null) continue;
                    item.setProduct(product);
                    
                    // Set other fields
                    item.setOrderedQuantity(orderedQuantities[i] != null ? orderedQuantities[i] : 0);
                    item.setUnitPrice(unitPrices[i] != null ? unitPrices[i] : BigDecimal.ZERO);
                    item.setDiscountPercentage(discountPercentages[i] != null ? discountPercentages[i] : BigDecimal.ZERO);
                    item.setTaxPercentage(taxPercentages[i] != null ? taxPercentages[i] : BigDecimal.ZERO);
                    item.setNotes(itemNotes[i] != null ? itemNotes[i] : "");
                    
                    // Calculate totals for this item
                    item.calculateTotals();
                    orderItems.add(item);
                }
            }
            
            // Don't set orderItems on purchaseOrder here - let the service handle it
            // purchaseOrder.setOrderItems(orderItems);
            
            // Save the purchase order with items
            PurchaseOrder savedPO = purchaseOrderService.createPurchaseOrderWithItems(purchaseOrder, orderItems);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Purchase Order '" + savedPO.getPoNumber() + "' created successfully!");
            
            return "redirect:/admin/business/purchase-orders";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error creating purchase order: " + e.getMessage());
            return "redirect:/admin/business/purchase-orders/new";
        }
    }
    
    @GetMapping("/purchase-orders/edit/{id}")
    public String showEditPurchaseOrderForm(@PathVariable Long id, Model model, HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        Optional<PurchaseOrder> purchaseOrder = purchaseOrderService.getPurchaseOrderById(id);
        if (purchaseOrder.isPresent()) {
            List<Supplier> suppliers = supplierService.getActiveSuppliers();
            List<Product> products = productRepository.findAll();
            
            model.addAttribute("purchaseOrder", purchaseOrder.get());
            model.addAttribute("suppliers", suppliers);
            model.addAttribute("products", products);
            model.addAttribute("user", user);
            model.addAttribute("poStatuses", PurchaseOrder.POStatus.values());
            return "admin-edit-purchase-order";
        }
        
        return "redirect:/admin/business/purchase-orders";
    }
    
    @PostMapping("/purchase-orders/edit/{id}")
    public String updatePurchaseOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String poNumber,
            @RequestParam Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedDeliveryDate,
            @RequestParam(required = false) String deliveryAddress,
            @RequestParam(required = false) String paymentTerms,
            @RequestParam(required = false, defaultValue = "0") BigDecimal shippingCost,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) Long[] itemIds,              // 👈 NEW
            @RequestParam(required = false) String[] productIds,
            @RequestParam(required = false) Integer[] orderedQuantities,
            @RequestParam(required = false) BigDecimal[] unitPrices,
            @RequestParam(required = false) BigDecimal[] discountPercentages,
            @RequestParam(required = false) BigDecimal[] taxPercentages,
            @RequestParam(required = false) String[] itemNotes,
            RedirectAttributes redirectAttributes) {
        
        try {
            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new RuntimeException("Supplier not found"));

            PurchaseOrder purchaseOrder = new PurchaseOrder();
            purchaseOrder.setId(id);
            purchaseOrder.setPoNumber(poNumber);
            purchaseOrder.setSupplier(supplier);
            if (expectedDeliveryDate != null) {
                purchaseOrder.setExpectedDeliveryDate(expectedDeliveryDate.atStartOfDay());
            }            purchaseOrder.setDeliveryAddress(deliveryAddress);
            purchaseOrder.setPaymentTerms(paymentTerms);
            purchaseOrder.setShippingCost(shippingCost);
            purchaseOrder.setNotes(notes);

            List<PurchaseOrderItem> orderItems = new ArrayList<>();

            if (productIds != null) {
                for (int i = 0; i < productIds.length; i++) {
                    if (productIds[i] != null && !productIds[i].trim().isEmpty()) {
                        Product product = productRepository.findById(Long.parseLong(productIds[i]))
                                .orElse(null);
                        if (product == null) continue;

                        PurchaseOrderItem item = new PurchaseOrderItem();

                        // 👇 If itemIds[] exists and has a value, set it (so JPA updates instead of insert)
                        if (itemIds != null && i < itemIds.length && itemIds[i] != null) {
                            item.setId(itemIds[i]);
                        }

                        item.setProduct(product);
                        item.setOrderedQuantity(orderedQuantities != null && i < orderedQuantities.length ? orderedQuantities[i] : 0);
                        item.setUnitPrice(unitPrices != null && i < unitPrices.length ? unitPrices[i] : BigDecimal.ZERO);
                        item.setDiscountPercentage(discountPercentages != null && i < discountPercentages.length ? discountPercentages[i] : BigDecimal.ZERO);
                        item.setTaxPercentage(taxPercentages != null && i < taxPercentages.length ? taxPercentages[i] : BigDecimal.ZERO);
                        item.setNotes(itemNotes != null && i < itemNotes.length ? itemNotes[i] : "");
                        item.calculateTotals();

                        orderItems.add(item);
                    }
                }
            }

            purchaseOrder.setOrderItems(orderItems);
            purchaseOrder.setUpdatedAt(LocalDateTime.now());
            purchaseOrder.calculateTotals();

            purchaseOrderService.updatePurchaseOrder(id, purchaseOrder);
            redirectAttributes.addFlashAttribute("successMessage", "Purchase order updated successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating purchase order: " + e.getMessage());
        }

        return "redirect:/admin/business/purchase-orders";
    }
   
    @PostMapping("/purchase-orders/status/{id}")
    @ResponseBody
    public Map<String, Object> updatePurchaseOrderStatus(@PathVariable Long id, 
                                                        @RequestParam String status, 
                                                        HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                return Map.of("success", false, "message", "Unauthorized");
            }
            
            PurchaseOrder.POStatus newStatus = PurchaseOrder.POStatus.valueOf(status);
            purchaseOrderService.updatePurchaseOrderStatus(id, newStatus);
            
            return Map.of("success", true, "message", "Purchase Order status updated successfully");
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/purchase-orders/search")
    public String searchPurchaseOrders(@RequestParam(required = false) String query, 
                                     Model model, 
                                     HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        List<PurchaseOrder> purchaseOrders = purchaseOrderService.searchPurchaseOrders(query);
        PurchaseOrderService.PurchaseOrderStatistics stats = purchaseOrderService.getPurchaseOrderStatistics();
        
        model.addAttribute("purchaseOrders", purchaseOrders);
        model.addAttribute("poStats", stats);
        model.addAttribute("user", user);
        model.addAttribute("searchQuery", query);
        
        // Add utility class to model for Thymeleaf access
        model.addAttribute("poUtils", poUtils);
        
        return "admin-purchase-orders";
    }
    
    // ==================== GRN MANAGEMENT ====================
    
    @GetMapping("/grn")
    public String viewGRN(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        // TODO: Implement GRN service and repository
        model.addAttribute("user", user);
        model.addAttribute("message", "GRN management coming soon!");
        
        return "admin-grn";
    }
    
    // ==================== SUPPLIER CREDIT NOTES ====================
    
    @GetMapping("/credit-notes")
    public String viewCreditNotes(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        // TODO: Implement credit note service and repository
        model.addAttribute("user", user);
        model.addAttribute("message", "Credit Note management coming soon!");
        
        return "admin-credit-notes";
    }
    
    // ==================== DASHBOARD ====================
    
    @GetMapping("/dashboard")
    public String businessDashboard(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        // Get supplier statistics
        SupplierService.SupplierStatistics supplierStats = supplierService.getSupplierStatistics();
        
        // Get purchase order statistics
        PurchaseOrderService.PurchaseOrderStatistics poStats = purchaseOrderService.getPurchaseOrderStatistics();
        
        // TODO: Get other business statistics
        // - GRN statistics
        // - Credit Note statistics
        // - Financial summaries
        
        model.addAttribute("user", user);
        model.addAttribute("supplierStats", supplierStats);
        model.addAttribute("poStats", poStats);
        
        return "admin-business-dashboard";
    }
    

}
