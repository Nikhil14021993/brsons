


package com.brsons.controller;

import com.brsons.model.*;
import com.brsons.service.SupplierService;
import com.brsons.service.PurchaseOrderService;
import com.brsons.service.GRNService;
import com.brsons.service.CreditNoteService;
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
import java.util.Arrays;

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
    private GRNService grnService;
    
    @Autowired
    private SupplierRepository supplierRepository;
    
    @Autowired
    private CreditNoteService creditNoteService;
    
    // ==================== AJAX ENDPOINTS ====================
    
    @GetMapping("/api/purchase-orders/{poId}/products")
    @ResponseBody
    public List<Map<String, Object>> getProductsByPurchaseOrder(@PathVariable Long poId) {
        try {
            Optional<PurchaseOrder> po = purchaseOrderService.getPurchaseOrderById(poId);
            if (po.isPresent()) {
                return po.get().getOrderItems().stream()
                    .map(item -> {
                        Map<String, Object> productMap = new HashMap<>();
                        Product product = item.getProduct();
                        productMap.put("productId", product.getId());  // Changed from "id" to "productId"
                        productMap.put("productName", product.getProductName());
                        productMap.put("unitPrice", item.getUnitPrice());
                        productMap.put("orderedQuantity", item.getOrderedQuantity());
                        productMap.put("discountPercentage", item.getDiscountPercentage());
                        productMap.put("taxPercentage", item.getTaxPercentage());
                        productMap.put("totalAmount", item.getTotalAmount());
                        return productMap;
                    })
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
    
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
    
    @PostMapping("/purchase-orders/{id}/status")
    @ResponseBody
    public Map<String, Object> updatePurchaseOrderStatus(@PathVariable Long id, 
                                                        @RequestParam PurchaseOrder.POStatus newStatus, 
                                                        HttpSession session) {
        System.out.println("=== STATUS UPDATE REQUEST ===");
        System.out.println("PO ID: " + id);
        System.out.println("New Status: " + newStatus);
        System.out.println("Request Method: POST");
        System.out.println("===============================");
        
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                System.out.println("ERROR: Unauthorized user");
                return Map.of("success", false, "message", "Unauthorized");
            }
            
            System.out.println("User authorized: " + user.getName());
            
            // Use the new manual method that includes proper validation and stock management
            PurchaseOrder updatedPO = purchaseOrderService.updatePOStatusManually(id, newStatus);
            
            System.out.println("PO status updated successfully to: " + updatedPO.getStatus());
            
            return Map.of("success", true, "message", "Purchase Order status updated successfully to " + newStatus);
            
        } catch (Exception e) {
            System.err.println("ERROR in controller: " + e.getMessage());
            e.printStackTrace();
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }
    
    // Get PO receipt summary
    @GetMapping("/purchase-orders/{id}/receipt-summary")
    @ResponseBody
    public Map<String, Object> getPOReceiptSummary(@PathVariable Long id, HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                return Map.of("success", false, "message", "Unauthorized");
            }
            
            Map<String, Object> summary = purchaseOrderService.getPOReceiptSummary(id);
            if (summary != null) {
                return Map.of("success", true, "data", summary);
            } else {
                return Map.of("success", false, "message", "Purchase Order not found");
            }
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }
    

    

    

    
    @DeleteMapping("/purchase-orders/delete/{id}")
    @ResponseBody
    public Map<String, Object> deletePurchaseOrder(@PathVariable Long id, HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                return Map.of("success", false, "message", "Unauthorized");
            }
            
            purchaseOrderService.hardDeletePurchaseOrder(id);
            
            return Map.of("success", true, "message", "Purchase Order deleted successfully");
            
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
        
        List<GoodsReceivedNote> grns = grnService.getAllGRNs();
        GRNService.GRNStatistics stats = grnService.getGRNStatistics();
        
        model.addAttribute("grns", grns);
        model.addAttribute("grnStats", stats);
        model.addAttribute("user", user);
        
        // Ensure message attributes are always present in the model to prevent Thymeleaf errors
        model.addAttribute("successMessage", model.getAttribute("successMessage") != null ? model.getAttribute("successMessage") : "");
        model.addAttribute("errorMessage", model.getAttribute("errorMessage") != null ? model.getAttribute("errorMessage") : "");
        
        return "admin-grn";
    }
    
    @GetMapping("/grn/new")
    public String showAddGRNForm(Model model, HttpSession session) {
        // auth checks omitted here (keep your original)
        List<PurchaseOrder> purchaseOrders = purchaseOrderService.getPurchaseOrdersReadyForGRN();
        List<Supplier> suppliers = supplierService.getAllSuppliers();

        ObjectMapper objectMapper = new ObjectMapper();
        String purchaseOrdersJson = "[]";
        try {
            List<Map<String,Object>> poData = new ArrayList<>();
            for (PurchaseOrder po : purchaseOrders) {
                Map<String,Object> m = new HashMap<>();
                m.put("id", po.getId());
                m.put("poNumber", po.getPoNumber());
                m.put("supplierId", po.getSupplier() != null ? po.getSupplier().getId() : null);
                m.put("supplierName", po.getSupplier() != null ? po.getSupplier().getCompanyName() : null);

                List<Map<String,Object>> items = new ArrayList<>();
                if (po.getOrderItems() != null) {
                    for (PurchaseOrderItem it : po.getOrderItems()) {
                        Map<String,Object> im = new HashMap<>();
                        im.put("productId", it.getProduct().getId());
                        im.put("productName", it.getProduct().getProductName());
                        im.put("orderedQuantity", it.getOrderedQuantity());
                        im.put("unitPrice", it.getUnitPrice());
                        im.put("discountPercentage", it.getDiscountPercentage());
                        im.put("taxPercentage", it.getTaxPercentage());
                        items.add(im);
                    }
                }
                m.put("items", items);
                poData.add(m);
            }
            purchaseOrdersJson = objectMapper.writeValueAsString(poData);
        } catch (Exception ex) {
            purchaseOrdersJson = "[]";
        }

        model.addAttribute("grn", new GoodsReceivedNote());
        model.addAttribute("purchaseOrders", purchaseOrders);
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("purchaseOrdersJson", purchaseOrdersJson);
        model.addAttribute("user", session.getAttribute("user"));
        model.addAttribute("grnStatuses", GoodsReceivedNote.GRNStatus.values());
        return "admin-add-grn";
    }

    
    @PostMapping("/grn/new")
    public String createGRN(
            @ModelAttribute GoodsReceivedNote grn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receivedDate,
            RedirectAttributes redirectAttributes) {

        try {
            // 1) If the form sent receivedDate as date, set it (to LocalDateTime)
        	  grn.setReceivedDate(receivedDate);
        	  
            // 2) Resolve purchaseOrder (the form has purchaseOrder.id)
            Long poId = (grn.getPurchaseOrder() != null) ? grn.getPurchaseOrder().getId() : null;
            if (poId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Purchase Order is required");
                return "redirect:/admin/business/grn/new";
            }
            PurchaseOrder po = purchaseOrderService.getByIdWithItems(poId)
                    .orElseThrow(() -> new RuntimeException("Purchase Order not found"));
            grn.setPurchaseOrder(po);

            // 3) Set supplier from PO (safer than trusting what the form sent)
            grn.setSupplier(po.getSupplier());

            // 4) Resolve each GRN item: the binder created product objects with only ID set;
            //    we must fetch the real Product entity and set grn reference and calculate totals.
            if (grn.getGrnItems() != null) {
                for (GRNItem gi : grn.getGrnItems()) {
                    Long prodId = (gi.getProduct() != null) ? gi.getProduct().getId() : null;
                    if (prodId == null) continue;
                    Product p = productRepository.findById(prodId)
                            .orElseThrow(() -> new RuntimeException("Product not found: " + prodId));
                    gi.setProduct(p);
                    gi.setGrn(grn);
                    gi.calculateTotals();
                }
            }

            // 5) Calculate totals for GRN
            grn.calculateTotals();

            // 6) Save via service
            grnService.createGRN(grn);

            redirectAttributes.addFlashAttribute("successMessage", "GRN created successfully");
            return "redirect:/admin/business/grn";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating GRN: " + e.getMessage());
            return "redirect:/admin/business/grn/new";
        }
    }

    @GetMapping("/grn/edit/{id}")
    public String editGRN(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }

        GoodsReceivedNote grn = grnService.getById(id);
        PurchaseOrder po = grn.getPurchaseOrder();

        // 🔑 force load supplier before Thymeleaf
        if (po != null && po.getSupplier() != null) {
            po.getSupplier().getCompanyName();  // triggers lazy initialization
        }

        // Get all purchase orders for the dropdown (same as add GRN)
        List<PurchaseOrder> purchaseOrders = purchaseOrderService.getAllPurchaseOrders();
        
        // Convert purchase orders to JSON for JavaScript
        ObjectMapper objectMapper = new ObjectMapper();
        String purchaseOrdersJson = "[]";
        try {
            System.out.println("=== DEBUG: Processing " + purchaseOrders.size() + " purchase orders for edit GRN ===");
            List<Map<String, Object>> poData = new ArrayList<>();
            for (PurchaseOrder purchaseOrder : purchaseOrders) {
                System.out.println("Processing PO: " + purchaseOrder.getPoNumber() + " (ID: " + purchaseOrder.getId() + ")");
                Map<String, Object> poMap = new HashMap<>();
                poMap.put("id", purchaseOrder.getId());
                poMap.put("poNumber", purchaseOrder.getPoNumber());
                poMap.put("supplierId", purchaseOrder.getSupplier().getId());
                poMap.put("supplierName", purchaseOrder.getSupplier().getCompanyName());
                System.out.println("Supplier: " + purchaseOrder.getSupplier().getCompanyName() + " (ID: " + purchaseOrder.getSupplier().getId() + ")");

                List<Map<String, Object>> poItems = new ArrayList<>();
                if (purchaseOrder.getOrderItems() != null) {
                    System.out.println("PO has " + purchaseOrder.getOrderItems().size() + " items");
                    for (PurchaseOrderItem item : purchaseOrder.getOrderItems()) {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("productId", item.getProduct().getId());
                        itemMap.put("productName", item.getProduct().getProductName());
                        itemMap.put("orderedQuantity", item.getOrderedQuantity());
                        itemMap.put("unitPrice", item.getUnitPrice());
                        itemMap.put("discountPercentage", item.getDiscountPercentage());
                        itemMap.put("taxPercentage", item.getTaxPercentage());
                        poItems.add(itemMap);
                        System.out.println("  Item: " + item.getProduct().getProductName() + ", Qty: " + item.getOrderedQuantity() + ", Price: " + item.getUnitPrice() + ", Discount: " + item.getDiscountPercentage() + ", Tax: " + item.getTaxPercentage());
                    }
                } else {
                    System.out.println("PO has no items (getOrderItems() is null)");
                }
                poMap.put("items", poItems);
                poData.add(poMap);
            }
            purchaseOrdersJson = objectMapper.writeValueAsString(poData);
            System.out.println("Generated JSON for edit GRN: " + purchaseOrdersJson);
        } catch (Exception e) {
            System.err.println("Error converting purchase orders to JSON for edit GRN: " + e.getMessage());
            purchaseOrdersJson = "[]";
        }

        model.addAttribute("grn", grn);
        model.addAttribute("purchaseOrder", po);   // fixed PO
        model.addAttribute("poItems", po.getOrderItems());
        model.addAttribute("user", user);
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        model.addAttribute("purchaseOrders", purchaseOrders);
        model.addAttribute("purchaseOrdersJson", purchaseOrdersJson);

        return "admin-edit-grn";
    }

    @PostMapping("/grn/edit/{id}")
    public String updateGRN(@PathVariable Long id, 
                           @ModelAttribute GoodsReceivedNote grn, 
                           HttpSession session, 
                           RedirectAttributes redirectAttributes) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                return "redirect:/login";
            }
            
            GoodsReceivedNote updatedGRN = grnService.updateGRN(id, grn);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "GRN '" + updatedGRN.getGrnNumber() + "' updated successfully!");
            
            return "redirect:/admin/business/grn";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error updating GRN: " + e.getMessage());
            return "redirect:/admin/business/grn/edit/" + id;
        }
    }
    
    @PostMapping("/grn/status/{id}")
    @ResponseBody
    public Map<String, Object> updateGRNStatus(@PathVariable Long id, 
                                              @RequestParam String status, 
                                              HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                return Map.of("success", false, "message", "Unauthorized");
            }
            
            GoodsReceivedNote.GRNStatus newStatus = GoodsReceivedNote.GRNStatus.valueOf(status);
            grnService.updateGRNStatus(id, newStatus);
            
            return Map.of("success", true, "message", "GRN status updated successfully");
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/grn/delete/{id}")
    @ResponseBody
    public Map<String, Object> deleteGRN(@PathVariable Long id, HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                return Map.of("success", false, "message", "Unauthorized");
            }
            
            grnService.hardDeleteGRN(id);
            
            return Map.of("success", true, "message", "GRN deleted successfully");
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/grn/search")
    public String searchGRNs(@RequestParam(required = false) String query, 
                            Model model, 
                            HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        List<GoodsReceivedNote> grns = grnService.getAllGRNs(); // TODO: Implement search
        GRNService.GRNStatistics stats = grnService.getGRNStatistics();
        
        model.addAttribute("grns", grns);
        model.addAttribute("grnStats", stats);
        model.addAttribute("user", user);
        model.addAttribute("searchQuery", query);
        
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
        
        List<CreditNote> creditNotes = creditNoteService.getAllCreditNotes();
        CreditNoteService.CreditNoteStatistics stats = creditNoteService.getCreditNoteStatistics();
        
        model.addAttribute("creditNotes", creditNotes);
        model.addAttribute("creditNoteStats", stats);
        model.addAttribute("user", user);
        
        return "admin-credit-notes";
    }
    
    @GetMapping("/credit-notes/new")
    public String showAddCreditNoteForm(Model model, HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        List<PurchaseOrder> orders = purchaseOrderService.getAllPurchaseOrders();
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        List<Product> products = productRepository.findAll();
        
        model.addAttribute("creditNote", new CreditNote());
        model.addAttribute("orders", orders);
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("products", products);
        model.addAttribute("user", user);
        
        return "admin-add-credit-note";
    }
    
    @PostMapping("/credit-notes/new")
    public String addCreditNote(@ModelAttribute CreditNote creditNote,
                               @RequestParam(required = false) Long[] productIds,
                               @RequestParam(required = false) Integer[] quantities,
                               @RequestParam(required = false) BigDecimal[] unitPrices,
                               @RequestParam(required = false) BigDecimal[] discountPercentages,
                               @RequestParam(required = false) BigDecimal[] taxPercentages,
                               @RequestParam(required = false) String[] reasons,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                return "redirect:/login";
            }
            
            System.out.println("=== DEBUG: Creating Credit Note ===");
            System.out.println("Credit Note: " + creditNote);
            System.out.println("Purchase Order ID: " + (creditNote.getPurchaseOrder() != null ? creditNote.getPurchaseOrder().getId() : "null"));
            System.out.println("Supplier ID: " + (creditNote.getSupplier() != null ? creditNote.getSupplier().getId() : "null"));
            System.out.println("Credit Date: " + creditNote.getCreditDate());
            System.out.println("Credit Amount: " + creditNote.getCreditAmount());
            System.out.println("Reason: " + creditNote.getReason());
            System.out.println("Notes: " + creditNote.getNotes());
            System.out.println("Product IDs: " + (productIds != null ? Arrays.toString(productIds) : "null"));
            System.out.println("Quantities: " + (quantities != null ? Arrays.toString(quantities) : "null"));
            System.out.println("Unit Prices: " + (unitPrices != null ? Arrays.toString(unitPrices) : "null"));
            System.out.println("Discount Percentages: " + (discountPercentages != null ? Arrays.toString(discountPercentages) : "null"));
            System.out.println("Tax Percentages: " + (taxPercentages != null ? Arrays.toString(taxPercentages) : "null"));
            System.out.println("Reasons: " + (reasons != null ? Arrays.toString(reasons) : "null"));
            
            // Set basic credit note details
            creditNote.setCreatedBy(user.getName());
            creditNote.setStatus("Draft");
            creditNote.setCreatedAt(LocalDateTime.now());
            
            // Ensure creditAmount is set (even if 0)
            if (creditNote.getCreditAmount() == null) {
                creditNote.setCreditAmount(BigDecimal.ZERO);
            }
            
            // Create credit note items
            if (productIds != null && productIds.length > 0) {
                System.out.println("Processing " + productIds.length + " items");
                for (int i = 0; i < productIds.length; i++) {
                    if (productIds[i] != null) {
                        CreditNoteItem item = new CreditNoteItem();
                        
                        // Set product
                        Product product = productRepository.findById(productIds[i]).orElse(null);
                        if (product == null) {
                            System.out.println("Product not found for ID: " + productIds[i]);
                            continue;
                        }
                        item.setProduct(product);
                        
                        // Set other fields with null checks and defaults
                        item.setQuantity(quantities != null && i < quantities.length && quantities[i] != null ? quantities[i] : 0);
                        item.setUnitPrice(unitPrices != null && i < unitPrices.length && unitPrices[i] != null ? unitPrices[i] : BigDecimal.ZERO);
                        item.setDiscountPercentage(discountPercentages != null && i < discountPercentages.length && discountPercentages[i] != null ? discountPercentages[i] : BigDecimal.ZERO);
                        item.setTaxPercentage(taxPercentages != null && i < taxPercentages.length && taxPercentages[i] != null ? taxPercentages[i] : BigDecimal.ZERO);
                        item.setReason(reasons != null && i < reasons.length && reasons[i] != null ? reasons[i] : "");
                        
                        // Set the credit note reference
                        item.setCreditNote(creditNote);
                        
                        // Add item to credit note
                        creditNote.addCreditNoteItem(item);
                        
                        System.out.println("Added item: " + product.getProductName() + ", Qty: " + item.getQuantity() + ", Price: " + item.getUnitPrice());
                    }
                }
            } else {
                System.out.println("No product IDs provided");
            }
            
            System.out.println("Credit Note items count: " + creditNote.getCreditNoteItems().size());
            
            // Save the credit note
            CreditNote savedCreditNote = creditNoteService.createCreditNote(creditNote);
            
            System.out.println("Credit Note saved successfully with ID: " + savedCreditNote.getId());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Credit Note created successfully! Number: " + savedCreditNote.getCreditNoteNumber());
            return "redirect:/admin/business/credit-notes";
            
        } catch (Exception e) {
            System.err.println("=== ERROR: Creating Credit Note ===");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error creating Credit Note: " + e.getMessage());
            return "redirect:/admin/business/credit-notes/new";
        }
    }
    
    @GetMapping("/credit-notes/edit/{id}")
    public String showEditCreditNoteForm(@PathVariable Long id, Model model, HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        CreditNote creditNote = creditNoteService.getCreditNoteById(id)
            .orElseThrow(() -> new RuntimeException("Credit Note not found"));
        
        List<PurchaseOrder> orders = purchaseOrderService.getAllPurchaseOrders();
        List<Supplier> suppliers = supplierService.getAllSuppliers();
        List<Product> products = productRepository.findAll();
        
        model.addAttribute("creditNote", creditNote);
        model.addAttribute("orders", orders);
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("products", products);
        model.addAttribute("user", user);
        
        return "admin-edit-credit-note";
    }
    
    @GetMapping("/credit-notes/view/{id}")
    public String viewCreditNote(@PathVariable Long id, Model model, HttpSession session) {
        // Check if user is logged in and is admin
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equals(user.getType())) {
            return "redirect:/login";
        }
        
        CreditNote creditNote = creditNoteService.getCreditNoteById(id)
            .orElseThrow(() -> new RuntimeException("Credit Note not found"));
        
        model.addAttribute("creditNote", creditNote);
        model.addAttribute("user", user);
        
        return "admin-view-credit-note";
    }
    
    @PostMapping("/credit-notes/edit/{id}")
    public String updateCreditNote(@PathVariable Long id,
                                  @ModelAttribute CreditNote creditNoteDetails,
                                  @RequestParam(required = false) Long[] productIds,
                                  @RequestParam(required = false) Integer[] quantities,
                                  @RequestParam(required = false) BigDecimal[] unitPrices,
                                  @RequestParam(required = false) BigDecimal[] discountPercentages,
                                  @RequestParam(required = false) BigDecimal[] taxPercentages,
                                  @RequestParam(required = false) String[] reasons,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                return "redirect:/login";
            }
            
            System.out.println("=== UPDATING CREDIT NOTE ===");
            System.out.println("Credit Note ID: " + id);
            System.out.println("Product IDs: " + Arrays.toString(productIds));
            System.out.println("Quantities: " + Arrays.toString(quantities));
            System.out.println("Unit Prices: " + Arrays.toString(unitPrices));
            
            creditNoteDetails.setUpdatedBy(user.getName());
            creditNoteDetails.setUpdatedAt(LocalDateTime.now());
            
            // Create credit note items
            if (productIds != null && productIds.length > 0) {
                List<CreditNoteItem> items = new ArrayList<>();
                for (int i = 0; i < productIds.length; i++) {
                    if (productIds[i] != null) {
                        CreditNoteItem item = new CreditNoteItem();
                        Product product = productRepository.findById(productIds[i]).orElse(null);
                        if (product != null) {
                            item.setProduct(product);
                            item.setQuantity(quantities != null && i < quantities.length && quantities[i] != null ? quantities[i] : 0);
                            item.setUnitPrice(unitPrices != null && i < unitPrices.length && unitPrices[i] != null ? unitPrices[i] : BigDecimal.ZERO);
                            item.setDiscountPercentage(discountPercentages != null && i < discountPercentages.length && discountPercentages[i] != null ? discountPercentages[i] : BigDecimal.ZERO);
                            item.setTaxPercentage(taxPercentages != null && i < taxPercentages.length && taxPercentages[i] != null ? taxPercentages[i] : BigDecimal.ZERO);
                            item.setReason(reasons != null && i < reasons.length && reasons[i] != null ? reasons[i] : "");
                            
                            // Set the credit note reference
                            item.setCreditNote(creditNoteDetails);
                            
                            // Add item to credit note
                            creditNoteDetails.addCreditNoteItem(item);
                            
                            System.out.println("Added item: " + product.getProductName() + ", Qty: " + item.getQuantity() + ", Price: " + item.getUnitPrice());
                        }
                    }
                }
            }
            
            System.out.println("Credit Note items count: " + creditNoteDetails.getCreditNoteItems().size());
            
            creditNoteService.updateCreditNote(id, creditNoteDetails);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Credit Note updated successfully!");
            return "redirect:/admin/business/credit-notes";
            
        } catch (Exception e) {
            System.err.println("=== ERROR: Updating Credit Note ===");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error updating Credit Note: " + e.getMessage());
            return "redirect:/admin/business/credit-notes/edit/" + id;
        }
    }
    
    @PostMapping("/credit-notes/status/{id}")
    @ResponseBody
    public Map<String, Object> updateCreditNoteStatus(@PathVariable Long id, 
                                                     @RequestParam String status, 
                                                     HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                return Map.of("success", false, "message", "Unauthorized");
            }
            
            creditNoteService.updateCreditNoteStatus(id, status);
            
            return Map.of("success", true, "message", "Credit Note status updated successfully");
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/credit-notes/delete/{id}")
    @ResponseBody
    public Map<String, Object> deleteCreditNote(@PathVariable Long id, HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null || !"Admin".equals(user.getType())) {
                return Map.of("success", false, "message", "Unauthorized");
            }
            
            creditNoteService.deleteCreditNote(id);
            
            return Map.of("success", true, "message", "Credit Note deleted successfully");
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
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
        
        // Get GRN statistics
        GRNService.GRNStatistics grnStats = grnService.getGRNStatistics();
        
        // Get Credit Note statistics
        CreditNoteService.CreditNoteStatistics creditNoteStats = creditNoteService.getCreditNoteStatistics();
        
        model.addAttribute("user", user);
        model.addAttribute("supplierStats", supplierStats);
        model.addAttribute("poStats", poStats);
        model.addAttribute("grnStats", grnStats);
        model.addAttribute("creditNoteStats", creditNoteStats);
        
        return "admin-business-dashboard";
    }
    

}
