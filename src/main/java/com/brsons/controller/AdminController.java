package com.brsons.controller;

import com.brsons.model.Category;
import com.brsons.model.Product;
import com.brsons.model.ProductVariant;
import com.brsons.model.User;
import com.brsons.repository.CategoryRepository;
import com.brsons.repository.ProductRepository;
import com.brsons.repository.ProductVariantRepository;
import com.brsons.repository.UserRepository;
import com.brsons.service.DayBookService;
import com.brsons.service.OrderService;
import com.brsons.service.AdminOrderService;

import com.brsons.dto.OrderDisplayDto;
import com.brsons.repository.InvoiceRepository;

import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.ZoneId;
import java.util.Date;
import java.io.ByteArrayOutputStream;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {
	
	@Autowired
    private DayBookService dayBookService;
	
	@Autowired
    private AdminOrderService adminOrderService;
	
	@Autowired
    private UserRepository userRepository;
	
	@Autowired
    private ProductVariantRepository productVariantRepository;
	
	@Autowired
    private InvoiceRepository invoiceRepository;
	
	private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public AdminController(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }
	
	
    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null && "Admin".equalsIgnoreCase(user.getType());
    }

    @GetMapping("/admin")
    public String adminPage(HttpSession session) {
        if (isAdmin(session)) return "admin";
        return "redirect:/";
    }

    // User Management Endpoints
    @GetMapping("/admin/users")
    public String showAllUsers(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/";
        }
        
        try {
            List<User> users = userRepository.findAll();
            model.addAttribute("users", users);
            model.addAttribute("searchQuery", null);
            model.addAttribute("searchMessage", null);
        } catch (Exception e) {
            System.out.println("Error fetching users: " + e.getMessage());
            model.addAttribute("users", new ArrayList<>());
            model.addAttribute("searchQuery", null);
            model.addAttribute("searchMessage", "Error loading users");
        }
        
        return "admin-users";
    }
    
    @GetMapping("/admin/users/search")
    public String searchUsers(@RequestParam String query, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/";
        }
        
        try {
            List<User> users = userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContainingIgnoreCase(query.trim());
            model.addAttribute("users", users);
            model.addAttribute("searchQuery", query);
            
            if (users.isEmpty()) {
                model.addAttribute("searchMessage", "No users found matching '" + query + "'");
            } else {
                model.addAttribute("searchMessage", "Found " + users.size() + " user(s) matching '" + query + "'");
            }
        } catch (Exception e) {
            System.out.println("Error searching users: " + e.getMessage());
            model.addAttribute("users", new ArrayList<>());
            model.addAttribute("searchQuery", query);
            model.addAttribute("searchMessage", "Error searching users");
        }
        
        return "admin-users";
    }
    
    @GetMapping("/admin/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/";
        }
        
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return "redirect:/admin/users?error=User+not+found";
        }
        
        model.addAttribute("user", user);
        return "admin-edit-user";
    }
    
    @PostMapping("/admin/users/update")
    public String updateUser(@ModelAttribute User user, @RequestParam(required = false) String newPassword, 
                           HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/";
        }
        
        try {
            User existingUser = userRepository.findById(user.getId()).orElse(null);
            if (existingUser == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/users";
            }
            
            // Update user details
            existingUser.setName(user.getName());
            existingUser.setEmail(user.getEmail());
            existingUser.setPhone(user.getPhone());
            existingUser.setType(user.getType());
            existingUser.setStatus(user.getStatus());
            
            // Update password if provided
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                existingUser.setPassword(newPassword);
            }
            
            userRepository.save(existingUser);
            redirectAttributes.addFlashAttribute("success", "User updated successfully");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating user: " + e.getMessage());
        }
        
        return "redirect:/admin/users";
    }
    
    @PostMapping("/admin/users/delete/{id}")
    @ResponseBody
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return "unauthorized";
        }
        
        try {
            User currentUser = (User) session.getAttribute("user");
            if (currentUser != null && currentUser.getId().equals(id)) {
                return "error: Cannot delete yourself";
            }
            
            User userToDelete = userRepository.findById(id).orElse(null);
            if (userToDelete == null) {
                return "error: User not found";
            }
            
            // Check if this is the last admin
            if ("Admin".equalsIgnoreCase(userToDelete.getType())) {
                long adminCount = userRepository.countByTypeIgnoreCase("Admin");
                if (adminCount <= 1) {
                    return "error: Cannot delete the last admin";
                }
            }
            
            userRepository.delete(userToDelete);
            return "success";
            
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    
    @PostMapping("/admin/users/toggle-status/{id}")
    @ResponseBody
    public String toggleUserStatus(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return "unauthorized";
        }
        
        try {
            User currentUser = (User) session.getAttribute("user");
            if (currentUser != null && currentUser.getId().equals(id)) {
                return "error: Cannot deactivate yourself";
            }
            
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return "error: User not found";
            }
            
            String newStatus = "ACTIVE".equalsIgnoreCase(user.getStatus()) ? "INACTIVE" : "ACTIVE";
            user.setStatus(newStatus);
            userRepository.save(user);
            
            return "success:" + newStatus;
            
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    @GetMapping("/admin/delete-product")
    public String deleteProductPage(HttpSession session) {
        if (isAdmin(session)) return "admin-delete-product";
        return "redirect:/";
    }

    @GetMapping("/admin/update-product")
    public String updateProductPage(HttpSession session) {
        if (isAdmin(session)) return "admin-update-product";
        return "redirect:/";
    }

    @GetMapping("/admin/orders")
    public String ordersPage(HttpSession session, Model model) {
        if (isAdmin(session)) {
            List<OrderDisplayDto> allOrders = adminOrderService.getAllOrders();
            AdminOrderService.OrderStatistics stats = adminOrderService.getOrderStatistics();
            
            model.addAttribute("orders", allOrders);
            model.addAttribute("stats", stats);
            return "admin-orders";
        }
        return "redirect:/";
    }
    
    @PostMapping("/admin/orders/update-status")
    @ResponseBody
    public String updateOrderStatus(@RequestParam Long orderId, @RequestParam String newStatus, HttpSession session) {
        if (isAdmin(session)) {
            boolean success = adminOrderService.updateOrderStatus(orderId, newStatus);
            return success ? "success" : "error";
        }
        return "unauthorized";
    }
    
    /**
     * Invoice management endpoints
     */
    @GetMapping("/admin/invoices")
    public String invoiceManagementPage(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/";
        }
        
        // Get invoice statistics
        long totalInvoices = invoiceRepository.count();
        long activeInvoices = 0; // Temporarily set to 0 since findExpiredInvoices is not available
        long expiredInvoices = totalInvoices - activeInvoices;
        
        model.addAttribute("totalInvoices", totalInvoices);
        model.addAttribute("activeInvoices", activeInvoices);
        model.addAttribute("expiredInvoices", expiredInvoices);
        
        return "admin-invoices";
    }
    
    @PostMapping("/admin/invoices/cleanup")
    @ResponseBody
    public String cleanupExpiredInvoices(HttpSession session) {
        if (!isAdmin(session)) {
            return "unauthorized";
        }
        
        try {
            // For now, just return success since we removed the service
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }
    

    
    @GetMapping("/admin/orders/filter")
    public String filterOrders(@RequestParam(required = false) String status, HttpSession session, Model model) {
        if (isAdmin(session)) {
            List<OrderDisplayDto> orders;
            if (status != null && !status.isEmpty()) {
                orders = adminOrderService.getOrdersByStatus(status);
            } else {
                orders = adminOrderService.getAllOrders();
            }
            model.addAttribute("orders", orders);
            model.addAttribute("selectedStatus", status);
            return "admin-orders";
        }
        return "redirect:/";
    }
    
    @GetMapping("/admin/add-product")
    public String addProductForm(HttpSession session, Model model) {
        if (isAdmin(session)) {
            Product product = new Product();
            product.setStatus("Active");
            product.setDiscount(0.0);
            product.setStockQuantity(0);
            
            model.addAttribute("product", product);
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin-add-product";
        }
        return "redirect:/";
    }

    @PostMapping("/admin/add-product")
    public String saveProduct(
            @RequestParam String productName,
            @RequestParam(required = false) String description,
            @RequestParam Double retailPrice,
            @RequestParam(required = false) Double b2bPrice,
            @RequestParam(required = false) Double discount,
            @RequestParam(required = false) Integer stockQuantity,
            @RequestParam String status,
            @RequestParam String mainPhoto,
            @RequestParam String selectedCategory,
            @RequestParam(required = false) String newCategoryName,
            @RequestParam("imageFile1") MultipartFile imageFile1,
            @RequestParam(value = "imageFile2", required = false) MultipartFile imageFile2,
            @RequestParam(value = "imageFile3", required = false) MultipartFile imageFile3,
            @RequestParam(value = "imageFile4", required = false) MultipartFile imageFile4,
            @RequestParam(value = "imageFile5", required = false) MultipartFile imageFile5,
            // Variant fields
            @RequestParam(value = "sizes", required = false) String[] sizes,
            @RequestParam(value = "colors", required = false) String[] colors,
            @RequestParam(value = "variantStockQuantities", required = false) Integer[] variantStockQuantities,
            @RequestParam(value = "variantRetailPrices", required = false) Double[] variantRetailPrices,
            @RequestParam(value = "variantB2bPrices", required = false) Double[] variantB2bPrices,
            @RequestParam(value = "variantDiscounts", required = false) Double[] variantDiscounts,
            @RequestParam(value = "skus", required = false) String[] skus
    ) throws IOException {

        // 1. Handle category
        Category category;
        if ("Other".equals(selectedCategory)) {
            category = new Category();
            category.setCategoryName(newCategoryName);
            category.setStatus("Active");
            categoryRepository.save(category);
        } else {
            category = categoryRepository.findById(Long.parseLong(selectedCategory)).orElse(null);
        }

        // 2. Save files and store file paths
        String uploadDir = "src/main/resources/static/uploads/";
        Files.createDirectories(Paths.get(uploadDir));

        Product product = new Product();
        product.setProductName(productName);
        product.setDescription(description);
        product.setRetailPrice(retailPrice);
        product.setB2bPrice(b2bPrice != null ? b2bPrice : retailPrice * 0.8); // Default B2B price
        product.setDiscount(discount != null ? discount : 0.0);
        product.setStockQuantity(stockQuantity != null ? stockQuantity : 0);
        product.setStatus(status != null ? status : "Active");

        if (!imageFile1.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + imageFile1.getOriginalFilename();
            imageFile1.transferTo(Paths.get(uploadDir + fileName));
            product.setImage1("/uploads/" + fileName);
        }

        if (imageFile2 != null && !imageFile2.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + imageFile2.getOriginalFilename();
            imageFile2.transferTo(Paths.get(uploadDir + fileName));
            product.setImage2("/uploads/" + fileName);
        }

        if (imageFile3 != null && !imageFile3.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + imageFile3.getOriginalFilename();
            imageFile3.transferTo(Paths.get(uploadDir + fileName));
            product.setImage3("/uploads/" + fileName);
        }

        if (imageFile4 != null && !imageFile4.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + imageFile4.getOriginalFilename();
            imageFile4.transferTo(Paths.get(uploadDir + fileName));
            product.setImage4("/uploads/" + fileName);
        }

        if (imageFile5 != null && !imageFile5.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + imageFile5.getOriginalFilename();
            imageFile5.transferTo(Paths.get(uploadDir + fileName));
            product.setImage5("/uploads/" + fileName);
        }

        // 3. Set main photo
        String selectedMainPhotoPath = null;
        switch (mainPhoto) {
            case "image1":
                selectedMainPhotoPath = product.getImage1();
                break;
            case "image2":
                selectedMainPhotoPath = product.getImage2();
                break;
            case "image3":
                selectedMainPhotoPath = product.getImage3();
                break;
            case "image4":
                selectedMainPhotoPath = product.getImage4();
                break;
            case "image5":
                selectedMainPhotoPath = product.getImage5();
                break;
        }
        if (selectedMainPhotoPath == null) {
            if (product.getImage1() != null) selectedMainPhotoPath = product.getImage1();
            else if (product.getImage2() != null) selectedMainPhotoPath = product.getImage2();
            else if (product.getImage3() != null) selectedMainPhotoPath = product.getImage3();
            else if (product.getImage4() != null) selectedMainPhotoPath = product.getImage4();
            else if (product.getImage5() != null) selectedMainPhotoPath = product.getImage5();
        }

        product.setMainPhoto(selectedMainPhotoPath);
        
        // 4. Link category and save product
        product.setCategory(category);
        productRepository.save(product);

        // 5. Create variants if provided
        if (sizes != null && sizes.length > 0) {
            for (int i = 0; i < sizes.length; i++) {
                if (sizes[i] != null && !sizes[i].trim().isEmpty()) {
                    ProductVariant variant = new ProductVariant();
                    variant.setProduct(product);
                    variant.setSize(sizes[i]);
                    variant.setColor(colors != null && i < colors.length ? colors[i] : "Default");
                    variant.setStockQuantity(variantStockQuantities != null && i < variantStockQuantities.length ? variantStockQuantities[i] : stockQuantity);
                    variant.setRetailPrice(variantRetailPrices != null && i < variantRetailPrices.length ? variantRetailPrices[i] : retailPrice);
                    variant.setB2bPrice(variantB2bPrices != null && i < variantB2bPrices.length ? variantB2bPrices[i] : product.getB2bPrice());
                    variant.setVariantDiscount(variantDiscounts != null && i < variantDiscounts.length ? variantDiscounts[i] : discount);
                    variant.setSku(skus != null && i < skus.length && skus[i] != null ? skus[i] : generateSKU(product, sizes[i], colors != null && i < colors.length ? colors[i] : "Default"));
                    variant.setStatus("Active");
                    
                    productVariantRepository.save(variant);
                }
            }
        } else {
            // Create default variant if no variants provided
            ProductVariant defaultVariant = new ProductVariant();
            defaultVariant.setProduct(product);
            defaultVariant.setSize("Default");
            defaultVariant.setColor("Default");
            defaultVariant.setStockQuantity(stockQuantity != null ? stockQuantity : 0);
            defaultVariant.setRetailPrice(retailPrice);
            defaultVariant.setB2bPrice(product.getB2bPrice());
            defaultVariant.setVariantDiscount(discount != null ? discount : 0.0);
            defaultVariant.setSku(generateSKU(product, "Default", "Default"));
            defaultVariant.setStatus("Active");
            
            productVariantRepository.save(defaultVariant);
        }

        return "redirect:/admin?success=Product+Added+Successfully";
    }
    
    private String generateSKU(Product product, String size, String color) {
        String productCode = product.getProductName().substring(0, Math.min(3, product.getProductName().length())).toUpperCase();
        String sizeCode = size != null ? size.substring(0, Math.min(2, size.length())).toUpperCase() : "NA";
        String colorCode = color != null ? color.substring(0, Math.min(2, color.length())).toUpperCase() : "NA";
        String idCode = String.format("%04d", product.getId());
        
        return productCode + "-" + sizeCode + "-" + colorCode + "-" + idCode;
    }
    
    
    @PostMapping("/admin/add-category")
    public String saveCategory(
            @RequestParam("categoryName") String categoryName,
            @RequestParam("imageFile") MultipartFile imageFile
    ) throws IOException {

        Category category = new Category();
        category.setCategoryName(categoryName);
        category.setStatus("Active");

        // Handle image file upload
        if (!imageFile.isEmpty()) {
            String uploadDir = "src/main/resources/static/uploads/categories/";
            Files.createDirectories(Paths.get(uploadDir));

            String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            imageFile.transferTo(Paths.get(uploadDir + fileName));

            category.setImage("/uploads/categories/" + fileName);
        }

        categoryRepository.save(category);
        return "redirect:/admin?success=Category+Added+Successfully";
    }
    @GetMapping("/admin/add-category")
    public String addCategoryForm(HttpSession session, Model model) {
        if (isAdmin(session)) {
            model.addAttribute("category", new Category());
            return "admin-add-category"; // This should be your Thymeleaf HTML template
        }
        return "redirect:/";
    }
    
    @GetMapping("/admin/daybook")
    public String showDaybook(@RequestParam(value = "date", required = false) String date, Model model, HttpSession session) {
    	 if (isAdmin(session)) {
    	if (date != null) {
            LocalDate localDate = LocalDate.parse(date);
            Map<String, Object> daybookData = dayBookService.getDayBook(localDate);
            model.addAttribute("daybook", daybookData);
        }
    	return "daybook"; // daybook.html
    	 }
    	 return "redirect:/";
    }

    // Inventory Management Endpoints
    @GetMapping("/admin/inventory")
    public String showInventory(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equalsIgnoreCase(user.getType())) {
            return "redirect:/login";
        }

        // Get all products with categories for inventory view
        List<Product> products = productRepository.findAllWithCategory();
        
        // Get categories for filter dropdown
        List<Category> categories = categoryRepository.findAll();
        
        // Calculate inventory statistics
        long totalProducts = products.size();
        long lowStockProducts = products.stream()
            .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= 10)
            .count();
        long outOfStockProducts = products.stream()
            .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= 0)
            .count();
        long activeProducts = products.stream()
            .filter(p -> "Active".equalsIgnoreCase(p.getStatus()))
            .count();

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("outOfStockProducts", outOfStockProducts);
        model.addAttribute("activeProducts", activeProducts);
        
        return "admin-inventory";
    }

    @GetMapping("/admin/inventory/filter")
    public String filterInventory(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categoryStatus,
            @RequestParam(required = false) String productStatus,
            @RequestParam(required = false) String stockFilter,
            @RequestParam(required = false) String searchQuery,
            Model model, 
            HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equalsIgnoreCase(user.getType())) {
            return "redirect:/login";
        }

        List<Product> filteredProducts = productRepository.findAllWithCategory();
        
        // Apply filters
        if (categoryId != null) {
            filteredProducts = filteredProducts.stream()
                .filter(p -> p.getCategory() != null && p.getCategory().getId().equals(categoryId))
                .collect(Collectors.toList());
        }
        
        if (categoryStatus != null && !categoryStatus.isEmpty()) {
            filteredProducts = filteredProducts.stream()
                .filter(p -> p.getCategory() != null && categoryStatus.equalsIgnoreCase(p.getCategory().getStatus()))
                .collect(Collectors.toList());
        }
        
        if (productStatus != null && !productStatus.isEmpty()) {
            filteredProducts = filteredProducts.stream()
                .filter(p -> productStatus.equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());
        }
        
        if (stockFilter != null && !stockFilter.isEmpty()) {
            switch (stockFilter.toLowerCase()) {
                case "out_of_stock":
                    filteredProducts = filteredProducts.stream()
                        .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= 0)
                        .collect(Collectors.toList());
                    break;
                case "low_stock":
                    filteredProducts = filteredProducts.stream()
                        .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 0 && p.getStockQuantity() <= 10)
                        .collect(Collectors.toList());
                    break;
                case "in_stock":
                    filteredProducts = filteredProducts.stream()
                        .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 10)
                        .collect(Collectors.toList());
                    break;
            }
        }
        
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String query = searchQuery.toLowerCase().trim();
            filteredProducts = filteredProducts.stream()
                .filter(p -> p.getProductName().toLowerCase().contains(query) ||
                           (p.getDescription() != null && p.getDescription().toLowerCase().contains(query)))
                .collect(Collectors.toList());
        }

        // Get categories for filter dropdown
        List<Category> categories = categoryRepository.findAll();
        
        // Calculate filtered statistics
        long totalProducts = filteredProducts.size();
        long lowStockProducts = filteredProducts.stream()
            .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= 10)
            .count();
        long outOfStockProducts = filteredProducts.stream()
            .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= 0)
            .count();
        long activeProducts = filteredProducts.stream()
            .filter(p -> "Active".equalsIgnoreCase(p.getStatus()))
            .count();

        model.addAttribute("products", filteredProducts);
        model.addAttribute("categories", categories);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("outOfStockProducts", outOfStockProducts);
        model.addAttribute("activeProducts", activeProducts);
        
        // Add filter values for form persistence
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedCategoryStatus", categoryStatus);
        model.addAttribute("selectedProductStatus", productStatus);
        model.addAttribute("selectedStockFilter", stockFilter);
        model.addAttribute("searchQuery", searchQuery);
        
        return "admin-inventory";
    }

    @GetMapping("/admin/inventory/export")
    public ResponseEntity<byte[]> exportInventoryToExcel(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String categoryStatus,
            @RequestParam(required = false) String productStatus,
            @RequestParam(required = false) String stockFilter,
            @RequestParam(required = false) String searchQuery,
            HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        if (user == null || !"Admin".equalsIgnoreCase(user.getType())) {
            return ResponseEntity.status(401).build();
        }

        try {
            List<Product> products = productRepository.findAllWithCategory();
            
            // Apply the same filters as the view
            if (categoryId != null) {
                products = products.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().getId().equals(categoryId))
                    .collect(Collectors.toList());
            }
            
            if (categoryStatus != null && !categoryStatus.isEmpty()) {
                products = products.stream()
                    .filter(p -> p.getCategory() != null && categoryStatus.equalsIgnoreCase(p.getCategory().getStatus()))
                    .collect(Collectors.toList());
            }
            
            if (productStatus != null && !productStatus.isEmpty()) {
                products = products.stream()
                    .filter(p -> productStatus.equalsIgnoreCase(p.getStatus()))
                    .collect(Collectors.toList());
            }
            
            if (stockFilter != null && !stockFilter.isEmpty()) {
                switch (stockFilter.toLowerCase()) {
                    case "out_of_stock":
                        products = products.stream()
                            .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= 0)
                            .collect(Collectors.toList());
                        break;
                    case "low_stock":
                        products = products.stream()
                            .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 0 && p.getStockQuantity() <= 10)
                            .collect(Collectors.toList());
                        break;
                    case "in_stock":
                        products = products.stream()
                            .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() > 10)
                            .collect(Collectors.toList());
                        break;
                }
            }
            
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String query = searchQuery.toLowerCase().trim();
                products = products.stream()
                    .filter(p -> p.getProductName().toLowerCase().contains(query) ||
                               (p.getDescription() != null && p.getDescription().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
            }

            // Generate Excel file
            byte[] excelContent = generateInventoryExcel(products);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "inventory_report.xlsx");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excelContent);
                
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    private byte[] generateInventoryExcel(List<Product> products) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Inventory Report");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Product ID", "Product Name", "Description", "Category", "Category Status",
                "Retail Price", "B2B Price", "Discount (%)", "Stock Quantity", "Product Status",
                "Created Date", "Last Updated"
            };
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }
            
            // Create data rows
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.LEFT);
            
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);
            
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setAlignment(HorizontalAlignment.CENTER);
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            
            int rowNum = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(product.getId() != null ? product.getId() : 0);
                row.createCell(1).setCellValue(product.getProductName() != null ? product.getProductName() : "");
                row.createCell(2).setCellValue(product.getDescription() != null ? product.getDescription() : "");
                row.createCell(3).setCellValue(product.getCategory() != null ? product.getCategory().getCategoryName() : "");
                row.createCell(4).setCellValue(product.getCategory() != null ? product.getCategory().getStatus() : "");
                
                Cell retailPriceCell = row.createCell(5);
                retailPriceCell.setCellValue(product.getRetailPrice() != null ? product.getRetailPrice() : 0.0);
                retailPriceCell.setCellStyle(numberStyle);
                
                Cell b2bPriceCell = row.createCell(6);
                b2bPriceCell.setCellValue(product.getB2bPrice() != null ? product.getB2bPrice() : 0.0);
                b2bPriceCell.setCellStyle(numberStyle);
                
                Cell discountCell = row.createCell(7);
                discountCell.setCellValue(product.getDiscount() != null ? product.getDiscount() : 0.0);
                discountCell.setCellStyle(numberStyle);
                
                Cell stockCell = row.createCell(8);
                stockCell.setCellValue(product.getStockQuantity() != null ? product.getStockQuantity() : 0);
                stockCell.setCellStyle(numberStyle);
                
                row.createCell(9).setCellValue(product.getStatus() != null ? product.getStatus() : "");
                
                Cell createdCell = row.createCell(10);
                if (product.getCreatedAt() != null) {
                    createdCell.setCellValue(Date.from(product.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()));
                    createdCell.setCellStyle(dateStyle);
                }
                
                Cell updatedCell = row.createCell(11);
                if (product.getUpdatedAt() != null) {
                    updatedCell.setCellValue(Date.from(product.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant()));
                    updatedCell.setCellStyle(dateStyle);
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Convert to byte array
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                workbook.write(baos);
                return baos.toByteArray();
            }
        }
    }

}
