package com.brsons.controller;

import com.brsons.model.AddToCart;
import com.brsons.model.CartItemDetails;
import com.brsons.model.CartProductEntry;
import com.brsons.model.Category;
import com.brsons.model.Product;
import com.brsons.model.TaxBreakdown;
import com.brsons.model.User;
import com.brsons.repository.AddToCartRepository;
import com.brsons.repository.CategoryRepository;
import com.brsons.repository.ProductRepository;
import com.brsons.repository.UserRepository;
import com.brsons.service.TaxCalculationService;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ShopController {
	private final CategoryRepository categoryRepository ;
	 private final ProductRepository productRepository ;
	 private final AddToCartRepository addToCartRepository ;
	 private final UserRepository userRepository ;
	 private final TaxCalculationService taxCalculationService;
	 
	 public ShopController(CategoryRepository categoryRepository,ProductRepository productRepository, AddToCartRepository addToCartRepository, UserRepository userRepository, TaxCalculationService taxCalculationService) {
	        this.categoryRepository = categoryRepository;
	        this.productRepository = productRepository;
	        this.addToCartRepository = addToCartRepository;
	        this.userRepository = userRepository;
	        this.taxCalculationService = taxCalculationService;
	    }

	@GetMapping("/shop")
	public String viewCategories(Model model, HttpSession session) {
	    List<Category> categories = categoryRepository.findByStatus("Active");
	    model.addAttribute("categories", categories);
	    
	    // Check if admin is in order creation mode
	    Boolean adminOrderMode = (Boolean) session.getAttribute("adminOrderMode");
	    User orderForUser = (User) session.getAttribute("orderForUser");
	    
	    // If admin is not in order creation mode, redirect to order creation page
	    User user = (User) session.getAttribute("user");
	    if (user != null && "Admin".equals(user.getType()) && (adminOrderMode == null || !adminOrderMode)) {
	        System.out.println("Admin user attempting to access shop directly, redirecting to order creation");
	        return "redirect:/admin/order-creation";
	    }
	    
	    if (adminOrderMode != null && adminOrderMode && orderForUser != null) {
	        model.addAttribute("adminOrderMode", true);
	        model.addAttribute("orderForUser", orderForUser);
	        model.addAttribute("orderAddress", session.getAttribute("orderAddress"));
	    }
	    
	    // Set cart count in session for navbar display
	    if (user != null) {
	        AddToCart cart = addToCartRepository.findByUserId(user.getId()).orElse(null);
	        if (cart != null && !cart.getProductQuantities().isEmpty()) {
	            int totalItems = cart.getProductQuantities().stream()
	                    .mapToInt(CartProductEntry::getQuantity)
	                    .sum();
	            session.setAttribute("cartCount", totalItems);
	        } else {
	            session.setAttribute("cartCount", 0);
	        }
	    }
	    
	    return "shop-categories";
	}
	
	@GetMapping("/shop/category/{id}")
	public String viewProductsByCategory(@PathVariable Long id, Model model, HttpSession session) {
	    System.out.println("=== CATEGORY PRODUCTS ENDPOINT HIT ===");
	    System.out.println("Category ID: " + id);
	    System.out.println("Session ID: " + session.getId());
	    
	    // If admin is not in order creation mode, redirect to order creation page
	    User user = (User) session.getAttribute("user");
	    if (user != null && "Admin".equals(user.getType())) {
	        Boolean adminOrderMode = (Boolean) session.getAttribute("adminOrderMode");
	        if (adminOrderMode == null || !adminOrderMode) {
	            System.out.println("Admin user attempting to access shop category directly, redirecting to order creation");
	            return "redirect:/admin/order-creation";
	        }
	    }
	    
	    Category category = categoryRepository.findById(id).orElse(null);
	    if (category == null) {
	        System.out.println("Category not found for ID: " + id);
	        return "redirect:/shop";
	    }
	    
	    System.out.println("Found category: " + category.getCategoryName());
	    
	    // Try different status values to find products
	    List<Product> products = new ArrayList<>();
	    
	    // First try with "Active" (proper case)
	    products = productRepository.findByCategoryIdAndStatus(id, "Active");
	    System.out.println("Products with 'Active' status: " + products.size());
	    
	    // If no products found, try with "active" (lowercase)
	    if (products.isEmpty()) {
	        products = productRepository.findByCategoryIdAndStatus(id, "active");
	        System.out.println("Products with 'active' status: " + products.size());
	    }
	    
	    // If still no products, try without status filter
	    if (products.isEmpty()) {
	        System.out.println("No products found with status filter, trying without status...");
	        products = productRepository.findByCategoryId(id);
	        System.out.println("Products found without status filter: " + products.size());
	    }
	    
	    System.out.println("Total products found: " + products.size());
	    if (!products.isEmpty()) {
	        System.out.println("First product: " + products.get(0).getProductName());
	        System.out.println("First product status: " + products.get(0).getStatus());
	        System.out.println("First product category: " + (products.get(0).getCategory() != null ? products.get(0).getCategory().getCategoryName() : "null"));
	    }
	    
	    model.addAttribute("category", category);
	    model.addAttribute("products", products);
	    
	    // Check if admin is in order creation mode
	    Boolean adminOrderMode = (Boolean) session.getAttribute("adminOrderMode");
	    User orderForUser = (User) session.getAttribute("orderForUser");
	    
	    if (adminOrderMode != null && adminOrderMode && orderForUser != null) {
	        model.addAttribute("adminOrderMode", true);
	        model.addAttribute("orderForUser", orderForUser);
	        model.addAttribute("orderAddress", session.getAttribute("orderAddress"));
	    }
	    
	    // Set cart count in session for navbar display
	    if (user != null) {
	        AddToCart cart = addToCartRepository.findByUserId(user.getId()).orElse(null);
	        if (cart != null && !cart.getProductQuantities().isEmpty()) {
	            int totalItems = cart.getProductQuantities().stream()
	                    .mapToInt(CartProductEntry::getQuantity)
	                    .sum();
	            session.setAttribute("cartCount", totalItems);
	        } else {
	            session.setAttribute("cartCount", 0);
	        }
	    }
	    
	    return "shop-products";
	}
	@GetMapping("/product/{id}")
	public String viewProduct(@PathVariable Long id, Model model, HttpSession session) {
	    System.out.println("=== PRODUCT ENDPOINT HIT ===");
	    System.out.println("Product ID: " + id);
	    System.out.println("Session ID: " + session.getId());
	    
	    // If admin is not in order creation mode, redirect to order creation page
	    User user = (User) session.getAttribute("user");
	    if (user != null && "Admin".equals(user.getType())) {
	        Boolean adminOrderMode = (Boolean) session.getAttribute("adminOrderMode");
	        if (adminOrderMode == null || !adminOrderMode) {
	            System.out.println("Admin user attempting to access product directly, redirecting to order creation");
	            return "redirect:/admin/order-creation";
	        }
	    }
	    
	    Product product = productRepository.findById(id).orElse(null);
	    if (product == null) {
	        System.out.println("Product not found, redirecting to home");
	        return "redirect:/"; // Or a 404 page
	    }
	    
	    System.out.println("Product found: " + product.getProductName());

	    // Collect product images
	    List<String> images = new ArrayList<>();
	    if (product.getImage1() != null) images.add(product.getImage1());
	    if (product.getImage2() != null) images.add(product.getImage2());
	    if (product.getImage3() != null) images.add(product.getImage3());
	    if (product.getImage4() != null) images.add(product.getImage4());
	    if (product.getImage5() != null) images.add(product.getImage5());

	    model.addAttribute("product", product);
	    model.addAttribute("images", images);

	    // Check if admin is in order creation mode
	    Boolean adminOrderMode = (Boolean) session.getAttribute("adminOrderMode");
	    User orderForUser = (User) session.getAttribute("orderForUser");
	    
	    if (adminOrderMode != null && adminOrderMode && orderForUser != null) {
	        model.addAttribute("adminOrderMode", true);
	        model.addAttribute("orderForUser", orderForUser);
	        model.addAttribute("orderAddress", session.getAttribute("orderAddress"));
	    }

	    // Check quantity in cart
	    int quantityInCart = 0;
	    System.out.println("User from session: " + (user != null ? user.getId() : "null"));
	    
	    if (user != null) {
	        Optional<AddToCart> optionalCart = addToCartRepository.findByUserId(user.getId());
	        if (optionalCart.isPresent()) {
	            AddToCart cart = optionalCart.get();
	            List<CartProductEntry> productQuantities = cart.getProductQuantities();
	            if (productQuantities != null) {
	                for (CartProductEntry entry : productQuantities) {
	                    if (entry.getProductId().equals(product.getId())) {
	                        quantityInCart = entry.getQuantity();
	                        System.out.println("Found product in cart with quantity: " + quantityInCart);
	                        break;
	                    }
	                }
	            }
	        }
	    }

	    model.addAttribute("quantityInCart", quantityInCart);
	    System.out.println("Returning product-details template with quantityInCart: " + quantityInCart);

	    // Set cart count in session for navbar display
	    if (user != null) {
	        AddToCart cart = addToCartRepository.findByUserId(user.getId()).orElse(null);
	        if (cart != null && !cart.getProductQuantities().isEmpty()) {
	            int totalItems = cart.getProductQuantities().stream()
	                    .mapToInt(CartProductEntry::getQuantity)
	                    .sum();
	            session.setAttribute("cartCount", totalItems);
	        } else {
	            session.setAttribute("cartCount", 0);
	        }
	    }

	    return "product-details";
	}
	
	@PostMapping("/add-to-cart/{productId}")
	@ResponseBody
	public ResponseEntity<?> addToCart(@PathVariable Long productId,
	                                   HttpSession session) {
	    System.out.println("=== ADD TO CART ENDPOINT HIT ===");
	    System.out.println("Product ID: " + productId);
	    System.out.println("Session ID: " + session.getId());
	    
	    User user = (User) session.getAttribute("user");
	    System.out.println("User from session: " + (user != null ? user.getId() : "null"));

	    if (user == null) {
	        System.out.println("User not logged in, returning 401");
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please login");
	    }
	    
	    // If admin is not in order creation mode, prevent adding to cart
	    if ("Admin".equals(user.getType())) {
	        Boolean adminOrderMode = (Boolean) session.getAttribute("adminOrderMode");
	        if (adminOrderMode == null || !adminOrderMode) {
	            System.out.println("Admin user attempting to add to cart directly, returning error");
	            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin users can only add items to cart when creating orders for other users");
	        }
	    }

	    // Get existing cart from session or create new one
	    Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cart");
	    if (cart == null) {
	        cart = new HashMap<>();
	    }

	    // Get product details to check B2B minimum quantity
	    Product product = productRepository.findById(productId).orElse(null);
	    if (product == null) {
	        return ResponseEntity.badRequest().body("Product not found");
	    }
	    
	    // Check if admin is in order creation mode and determine the correct user type
	    Boolean adminOrderMode = (Boolean) session.getAttribute("adminOrderMode");
	    User orderForUser = (User) session.getAttribute("orderForUser");
	    String userTypeForQuantity = user.getType();
	    
	    if (adminOrderMode != null && adminOrderMode && orderForUser != null) {
	        userTypeForQuantity = orderForUser.getType();
	    }
	    
	    // Determine quantity to add based on user type and B2B minimum quantity
	    int quantityToAdd = 1;
	    if ("B2B".equals(userTypeForQuantity) && product.getB2bMinQuantity() != null && product.getB2bMinQuantity() > 1) {
	        quantityToAdd = product.getB2bMinQuantity();
	    }
	    
	    // Update quantity if product already exists, else set to quantityToAdd
	    cart.put(productId, cart.getOrDefault(productId, 0) + quantityToAdd);

	    // Store updated cart back into session
	    session.setAttribute("cart", cart);
	    
	    AddToCart cart1 = addToCartRepository.findByUserId(user.getId())
	            .orElse(new AddToCart());

	    cart1.setUserId(user.getId());
	    cart1.setUserName(user.getName());
	    cart1.setUserEmail(user.getEmail());
	    cart1.setUserPhone(user.getPhone());

	    // Check if product already exists in cart
	    List<CartProductEntry> productEntries = cart1.getProductQuantities();
	    Optional<CartProductEntry> existingEntryOpt = productEntries.stream()
	            .filter(e -> e.getProductId().equals(productId))
	            .findFirst();

	    int newQty;
	    if (existingEntryOpt.isPresent()) {
	        CartProductEntry entry = existingEntryOpt.get();
	        entry.setQuantity(entry.getQuantity() + quantityToAdd);
	        newQty = entry.getQuantity();
	        System.out.println("Updated existing product quantity to: " + newQty + " (added " + quantityToAdd + ")");
	    } else {
	        CartProductEntry newEntry = new CartProductEntry(productId, quantityToAdd, user.getPhone());
	        productEntries.add(newEntry);
	        newQty = quantityToAdd;
	        System.out.println("Added new product with quantity: " + newQty + " (B2B min qty: " + (product.getB2bMinQuantity() != null ? product.getB2bMinQuantity() : "null") + ")");
	    }

	    addToCartRepository.save(cart1);
	    System.out.println("Cart saved successfully, returning quantity: " + newQty);
	    
	    // Update cart count in session
	    int totalItems = cart1.getProductQuantities().stream()
	            .mapToInt(CartProductEntry::getQuantity)
	            .sum();
	    session.setAttribute("cartCount", totalItems);
	    
	    return ResponseEntity.ok(Map.of("quantity", newQty));
	}

	@PostMapping("/update-cart/{productId}")
	@ResponseBody
	public ResponseEntity<?> updateCart(@PathVariable Long productId,
	                                    @RequestParam int delta,
	                                    HttpSession session) {
	    
	    System.out.println("=== UPDATE CART ENDPOINT HIT ===");
	    System.out.println("Product ID: " + productId);
	    System.out.println("Delta: " + delta);
	    System.out.println("Session ID: " + session.getId());

	    User user = (User) session.getAttribute("user");
	    if (user == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required");
	    }
	    
	    System.out.println("User ID: " + user.getId() + ", User Type: " + user.getType());
	    
	    // If admin is not in order creation mode, prevent updating cart
	    if ("Admin".equals(user.getType())) {
	        Boolean adminOrderMode = (Boolean) session.getAttribute("adminOrderMode");
	        if (adminOrderMode == null || !adminOrderMode) {
	            System.out.println("Admin user attempting to update cart directly, returning error");
	            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin users can only update cart when creating orders for other users");
	        }
	    }

	    AddToCart cart = addToCartRepository.findByUserId(user.getId())
	            .orElseThrow(() -> new RuntimeException("Cart not found"));
	    
	    System.out.println("Cart found - ID: " + cart.getId() + ", User ID: " + cart.getUserId());
	    System.out.println("Total product entries in cart: " + cart.getProductQuantities().size());

	    List<CartProductEntry> productEntries = cart.getProductQuantities();
	    
	    // Debug: Print all cart entries
	    for (CartProductEntry entry : productEntries) {
	        System.out.println("Cart entry - Product ID: " + entry.getProductId() + ", Quantity: " + entry.getQuantity());
	    }
	    
	    Optional<CartProductEntry> entryOpt = productEntries.stream()
	            .filter(e -> e.getProductId().equals(productId))
	            .findFirst();
	    
	    System.out.println("Found cart entry: " + entryOpt.isPresent());

	    if (entryOpt.isPresent()) {
	        CartProductEntry entry = entryOpt.get();
	        System.out.println("Found cart entry - Product ID: " + entry.getProductId() + ", Current Quantity: " + entry.getQuantity());
	        
	        // Get product details to check B2B minimum quantity
	        Product product = productRepository.findById(productId).orElse(null);
	        if (product == null) {
	            return ResponseEntity.badRequest().body("Product not found");
	        }
	        
	        // Check if admin is in order creation mode and determine the correct user type
	        Boolean adminOrderMode = (Boolean) session.getAttribute("adminOrderMode");
	        User orderForUser = (User) session.getAttribute("orderForUser");
	        String userTypeForQuantity = user.getType();
	        
	        if (adminOrderMode != null && adminOrderMode && orderForUser != null) {
	            userTypeForQuantity = orderForUser.getType();
	        }
	        
	        // The frontend already calculates the correct delta based on user type and B2B minimum quantity
	        int actualDelta = delta;
	        
	        System.out.println("Delta received from frontend: " + delta);
	        System.out.println("Current quantity: " + entry.getQuantity());
	        System.out.println("B2B min quantity: " + (product.getB2bMinQuantity() != null ? product.getB2bMinQuantity() : "null"));
	        System.out.println("User type for quantity: " + userTypeForQuantity);
	        
	        int updatedQty = entry.getQuantity() + actualDelta;
	        System.out.println("Updated quantity: " + updatedQty);
	        
	                // Check B2B minimum quantity constraint - allow going to 0 for removal
        if ("B2B".equals(userTypeForQuantity) && product.getB2bMinQuantity() != null && product.getB2bMinQuantity() > 1) {
            if (updatedQty < 0) {
                System.out.println("Quantity below 0, rejecting update");
                return ResponseEntity.badRequest().body("Quantity cannot be negative");
            }
        }

	        if (updatedQty <= 0) {
	            // Remove the entry from the list
	            productEntries.remove(entry);
	            updatedQty = 0;
	            System.out.println("Product removed from cart, quantity: " + updatedQty);
	            System.out.println("Remaining entries in cart: " + productEntries.size());
	            
	            // Explicitly set the updated list to ensure JPA tracks the change
	            cart.setProductQuantities(productEntries);
	        } else {
	            entry.setQuantity(updatedQty);
	            System.out.println("Product quantity updated to: " + updatedQty);
	        }
	        
	        addToCartRepository.save(cart);
	        System.out.println("Cart saved successfully");
	        
	        // Update cart count in session
	        int totalItems = cart.getProductQuantities().stream()
	                .mapToInt(CartProductEntry::getQuantity)
	                .sum();
	        session.setAttribute("cartCount", totalItems);
	        System.out.println("Updated cart count in session: " + totalItems);
	        
	        return ResponseEntity.ok(Map.of("quantity", updatedQty));
	    } else {
	        return ResponseEntity.badRequest().body("Product not in cart");
	    }
	}
	
	// New method for updating product quantities in shop view (separate from cart updates)
	@PostMapping("/update-product-quantity/{productId}")
	@ResponseBody
	public ResponseEntity<?> updateProductQuantity(@PathVariable Long productId,
	                                             @RequestParam int delta,
	                                             HttpSession session) {
	    
	    User user = (User) session.getAttribute("user");
	    if (user == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required");
	    }
	    
	    // If admin is not in order creation mode, prevent updating
	    if ("Admin".equals(user.getType())) {
	        Boolean adminOrderMode = (Boolean) session.getAttribute("adminOrderMode");
	        if (adminOrderMode == null || !adminOrderMode) {
	            System.out.println("Admin user attempting to update product quantity directly, returning error");
	            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin users can only update quantities in order creation mode");
	        }
	    }
	    
	    System.out.println("=== UPDATE PRODUCT QUANTITY ===");
	    System.out.println("Product ID: " + productId + ", Delta: " + delta);
	    System.out.println("User: " + user.getName() + " (" + user.getType() + ")");
	    
	    // Get product details to check B2B minimum quantity
	    Product product = productRepository.findById(productId).orElse(null);
	    if (product == null) {
	        return ResponseEntity.badRequest().body("Product not found");
	    }
	    
	    // Check if admin is in order creation mode and determine the correct user type
	    Boolean adminOrderMode = (Boolean) session.getAttribute("adminOrderMode");
	    User orderForUser = (User) session.getAttribute("orderForUser");
	    String userTypeForQuantity = user.getType();
	    
	    if (adminOrderMode != null && adminOrderMode && orderForUser != null) {
	        userTypeForQuantity = orderForUser.getType();
	    }
	    
	    // The frontend already calculates the correct delta based on user type and B2B minimum quantity
	    int actualDelta = delta;
	    
	    System.out.println("Delta received from frontend: " + delta + " (B2B min qty: " + (product.getB2bMinQuantity() != null ? product.getB2bMinQuantity() : "null") + ")");
	    
	    // Get or create cart
	    AddToCart cart = addToCartRepository.findByUserId(user.getId()).orElse(null);
	    if (cart == null) {
	        // Create new cart if it doesn't exist
	        cart = new AddToCart();
	        cart.setUserId(user.getId());
	        cart.setProductQuantities(new ArrayList<>());
	    }
	    
	    List<CartProductEntry> productEntries = cart.getProductQuantities();
	    Optional<CartProductEntry> entryOpt = productEntries.stream()
	            .filter(e -> e.getProductId().equals(productId))
	            .findFirst();
	    
	    int updatedQty = 0;
	    
	    if (entryOpt.isPresent()) {
	        CartProductEntry entry = entryOpt.get();
	        int currentQty = entry.getQuantity();
	        updatedQty = currentQty + actualDelta;
	        
	        System.out.println("Updating existing product - Current Qty: " + currentQty + ", New Qty: " + updatedQty);
	        
	                // Check B2B minimum quantity constraint - allow going to 0 for removal
        if ("B2B".equals(userTypeForQuantity) && product.getB2bMinQuantity() != null && product.getB2bMinQuantity() > 1) {
            if (updatedQty < 0) {
                return ResponseEntity.badRequest().body("Quantity cannot be negative");
            }
        }
	        
	        if (updatedQty <= 0) {
	            // Remove the entry from the list
	            productEntries.remove(entry);
	            updatedQty = 0;
	            System.out.println("Product removed from cart, quantity: " + updatedQty);
	            System.out.println("Remaining entries in cart: " + productEntries.size());
	        } else {
	            entry.setQuantity(updatedQty);
	            System.out.println("Product quantity updated to: " + updatedQty);
	        }
	        
	        // Explicitly set the list back to ensure JPA tracks changes
	        cart.setProductQuantities(productEntries);
	    } else {
	        // Product not in cart, add it
	        if (actualDelta > 0) {
	            CartProductEntry newEntry = new CartProductEntry(productId, actualDelta, user.getPhone());
	            productEntries.add(newEntry);
	            updatedQty = actualDelta;
	            System.out.println("Added new product with quantity: " + updatedQty);
	        } else {
	            return ResponseEntity.badRequest().body("Cannot decrease quantity for product not in cart");
	        }
	    }
	    
	    addToCartRepository.save(cart);
	    System.out.println("Cart saved successfully");
	    
	    // Update cart count in session
	    int totalItems = cart.getProductQuantities().stream()
	            .mapToInt(CartProductEntry::getQuantity)
	            .sum();
	    session.setAttribute("cartCount", totalItems);
	    System.out.println("Updated cart count in session: " + totalItems);
	    
	    return ResponseEntity.ok(Map.of("quantity", updatedQty));
	}
	
	@GetMapping("/cart")
	public String viewCart(Model model, HttpSession session) {
	    User user = (User) session.getAttribute("user");

	    if (user == null) {
	        return "redirect:/login";
	    }
	    
	    // If admin is not in order creation mode, redirect to order creation page
	    if ("Admin".equals(user.getType())) {
	        Boolean adminOrderMode = (Boolean) session.getAttribute("adminOrderMode");
	        if (adminOrderMode == null || !adminOrderMode) {
	            System.out.println("Admin user attempting to access cart directly, redirecting to order creation");
	            return "redirect:/admin/order-creation";
	        }
	    }

	    // Check if admin is in order creation mode
	    Boolean adminOrderMode = (Boolean) session.getAttribute("adminOrderMode");
	    User orderForUser = (User) session.getAttribute("orderForUser");
	    
	    if (adminOrderMode != null && adminOrderMode && orderForUser != null) {
	        model.addAttribute("adminOrderMode", true);
	        model.addAttribute("orderForUser", orderForUser);
	        model.addAttribute("orderAddress", session.getAttribute("orderAddress"));
	    }

	    AddToCart cart = addToCartRepository.findByUserId(user.getId())
	            .orElse(null);

	    if (cart == null || cart.getProductQuantities().isEmpty()) {
	        model.addAttribute("message", "Your cart is empty.");
	        return "cart";
	    }

	    List<CartItemDetails> cartItems = new ArrayList<>();
	    double grandTotal = 0.0;

	    System.out.println("=== CART PAGE DEBUG ===");
	    System.out.println("User ID: " + user.getId());
	    System.out.println("Cart found: " + (cart != null));
	    if (cart != null) {
	        System.out.println("Cart ID: " + cart.getId());
	        System.out.println("Product quantities count: " + cart.getProductQuantities().size());
	    }

	    	        	    System.out.println("=== PROCESSING CART ENTRIES ===");
	    for (CartProductEntry entry : cart.getProductQuantities()) {
	        System.out.println("Processing entry - Product ID: " + entry.getProductId() + ", Quantity: " + entry.getQuantity());
	        
	        // Use a custom query to fetch product with category
	        Product product = productRepository.findByIdWithCategory(entry.getProductId()).orElse(null);

	            if (product != null) {
	                // Determine which user type to use for pricing
	                String userTypeForPricing;
	                if (adminOrderMode != null && adminOrderMode && orderForUser != null) {
	                    userTypeForPricing = orderForUser.getType();
	                } else {
	                    userTypeForPricing = user.getType();
	                }
	                
	                // Use appropriate price based on user type
	                double unitPrice = "B2B".equals(userTypeForPricing) ? product.getB2bPrice() : product.getRetailPrice();
	                System.out.println("Product found: " + product.getProductName() + ", B2B Price: " + product.getB2bPrice() + ", Retail Price: " + product.getRetailPrice() + ", Using Price: " + unitPrice + " (User Type for Pricing: " + userTypeForPricing + ", Admin Order Mode: " + adminOrderMode + ")");
	                System.out.println("Product category: " + (product.getCategory() != null ? product.getCategory().getCategoryName() : "NULL"));
	                
	                int quantity = entry.getQuantity();
	                double totalPrice = unitPrice * quantity;

	                grandTotal += totalPrice;

	                                CartItemDetails cartItem = new CartItemDetails(entry.getProductId(), product, quantity, totalPrice);
                cartItems.add(cartItem);
                System.out.println("Cart item created - ID: " + cartItem.getId() + " (Product ID), Total Price: " + cartItem.getTotalPrice() + ", Unit Price: " + unitPrice + ", User Type for Pricing: " + userTypeForPricing);
	            } else {
	                System.out.println("Product not found for ID: " + entry.getProductId());
	            }
	        }

	    System.out.println("Final cart items count: " + cartItems.size());
	    System.out.println("Final grand total: " + grandTotal);

	    // Calculate tax breakdown based on user state
	    String userState = null;
	    if (adminOrderMode != null && adminOrderMode && orderForUser != null) {
	        userState = orderForUser.getState();
	    } else {
	        userState = user.getState();
	    }
	    
	    TaxBreakdown taxBreakdown = null;
	    String taxType = "UNKNOWN";
	    
	    if (userState != null && !userState.trim().isEmpty()) {
	        taxType = taxCalculationService.determineTaxType(userState);
	        taxBreakdown = taxCalculationService.calculateTaxForCart(cartItems, userState);
	        System.out.println("Tax calculation - User State: " + userState + ", Tax Type: " + taxType + ", Tax Breakdown: " + taxBreakdown);
	    } else {
	        System.out.println("No user state available for tax calculation");
	    }

	    model.addAttribute("cartItems", cartItems);
	    model.addAttribute("grandTotal", grandTotal);
	    model.addAttribute("taxBreakdown", taxBreakdown);
	    model.addAttribute("taxType", taxType);
	    model.addAttribute("userState", userState);
	    model.addAttribute("businessState", taxCalculationService.getBusinessState());
	    
	    // Set cart count in session for navbar display
	    int totalItems = cart.getProductQuantities().stream()
	            .mapToInt(CartProductEntry::getQuantity)
	            .sum();
	    session.setAttribute("cartCount", totalItems);
	    
	    return "cart";
	}

	@GetMapping("/cart/count")
	@ResponseBody
	public ResponseEntity<?> getCartCount(HttpSession session) {
	    User user = (User) session.getAttribute("user");
	    
	    if (user == null) {
	        return ResponseEntity.ok(Map.of("count", 0));
	    }
	    
	    AddToCart cart = addToCartRepository.findByUserId(user.getId())
	            .orElse(null);
	    
	    if (cart == null || cart.getProductQuantities().isEmpty()) {
	        return ResponseEntity.ok(Map.of("count", 0));
	    }
	    
	    int totalItems = cart.getProductQuantities().stream()
	            .mapToInt(CartProductEntry::getQuantity)
	            .sum();
	    
	    return ResponseEntity.ok(Map.of("count", totalItems));
	}

	@GetMapping("/debug/tax-calculation")
	@ResponseBody
	public ResponseEntity<?> debugTaxCalculation(HttpSession session) {
	    User user = (User) session.getAttribute("user");
	    if (user == null) {
	        return ResponseEntity.ok(Map.of("error", "User not logged in"));
	    }
	    
	    Map<String, Object> response = new HashMap<>();
	    response.put("userName", user.getName());
	    response.put("userState", user.getState());
	    response.put("businessState", taxCalculationService.getBusinessState());
	    response.put("taxType", taxCalculationService.determineTaxType(user.getState()));
	    response.put("isIntraState", taxCalculationService.isIntraStateTransaction(user.getState()));
	    
	    return ResponseEntity.ok(response);
	}

	@GetMapping("/debug/session")
	@ResponseBody
	public ResponseEntity<?> debugSession(HttpSession session) {
	    System.out.println("=== SESSION DEBUG ENDPOINT ===");
	    System.out.println("Session ID: " + session.getId());
	    
	    User user = (User) session.getAttribute("user");
	    System.out.println("User from session: " + (user != null ? "User ID: " + user.getId() + ", Name: " + user.getName() : "null"));
	    
	    Map<String, Object> debugInfo = new HashMap<>();
	    debugInfo.put("sessionId", session.getId());
	    debugInfo.put("userLoggedIn", user != null);
	    
	    if (user != null) {
	        debugInfo.put("userId", user.getId());
	        debugInfo.put("userName", user.getName());
	        debugInfo.put("userEmail", user.getEmail());
	        debugInfo.put("userPhone", user.getPhone());
	    }
	    
	    return ResponseEntity.ok(debugInfo);
	}

	@GetMapping("/test/session")
	@ResponseBody
	public ResponseEntity<?> testSession(HttpSession session) {
	    System.out.println("=== TEST SESSION ENDPOINT ===");
	    System.out.println("Session ID: " + session.getId());
	    
	    // Try to get user from session
	    User user = (User) session.getAttribute("user");
	    System.out.println("User from session: " + (user != null ? "User ID: " + user.getId() + ", Name: " + user.getName() : "null"));
	    
	    // Try to set a test attribute
	    session.setAttribute("testAttribute", "testValue");
	    String testValue = (String) session.getAttribute("testAttribute");
	    System.out.println("Test attribute value: " + testValue);
	    
	    Map<String, Object> result = new HashMap<>();
	    result.put("sessionId", session.getId());
	    result.put("userLoggedIn", user != null);
	    result.put("testAttributeSet", "testValue".equals(testValue));
	    
	    if (user != null) {
	        result.put("userId", user.getId());
	        result.put("userName", user.getName());
	    }
	    
	    return ResponseEntity.ok(result);
	}

	@GetMapping("/cart/details")
	@ResponseBody
	public ResponseEntity<?> getCartDetails(HttpSession session) {
	    User user = (User) session.getAttribute("user");
	    if (user == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required");
	    }
	    
	    AddToCart cart = addToCartRepository.findByUserId(user.getId()).orElse(null);
	    if (cart == null || cart.getProductQuantities().isEmpty()) {
	        return ResponseEntity.ok(Map.of("items", new ArrayList<>()));
	    }
	    
	    List<Map<String, Object>> cartDetails = new ArrayList<>();
	    for (CartProductEntry entry : cart.getProductQuantities()) {
	        Map<String, Object> item = new HashMap<>();
	        item.put("productId", entry.getProductId());
	        item.put("quantity", entry.getQuantity());
	        cartDetails.add(item);
	    }
	    
	    return ResponseEntity.ok(Map.of("items", cartDetails));
	}

	

}
