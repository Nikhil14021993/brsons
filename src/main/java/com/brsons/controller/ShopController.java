package com.brsons.controller;

import com.brsons.model.AddToCart;
import com.brsons.model.CartItemDetails;
import com.brsons.model.CartProductEntry;
import com.brsons.model.Category;
import com.brsons.model.Product;
import com.brsons.model.User;
import com.brsons.repository.AddToCartRepository;
import com.brsons.repository.CategoryRepository;
import com.brsons.repository.ProductRepository;
import com.brsons.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ShopController {
	private final CategoryRepository categoryRepository ;
	 private final ProductRepository productRepository ;
	 private final AddToCartRepository addToCartRepository ;
	 private final UserRepository userRepository ;
	 
	 public ShopController(CategoryRepository categoryRepository,ProductRepository productRepository, AddToCartRepository addToCartRepository, UserRepository userRepository) {
	        this.categoryRepository = categoryRepository;
	        this.productRepository = productRepository;
	        this.addToCartRepository = addToCartRepository;
	        this.userRepository = userRepository;
	    }

	@GetMapping("/shop")
	public String viewCategories(Model model, HttpSession session) {
	    List<Category> categories = categoryRepository.findByStatus("Active");
	    model.addAttribute("categories", categories);
	    
	    // Set cart count in session for navbar display
	    User user = (User) session.getAttribute("user");
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
	    
	    // Set cart count in session for navbar display
	    User user = (User) session.getAttribute("user");
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

	    // Check quantity in cart
	    int quantityInCart = 0;
	    User user = (User) session.getAttribute("user");
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

	    // Get existing cart from session or create new one
	    Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("cart");
	    if (cart == null) {
	        cart = new HashMap<>();
	    }

	    // Update quantity if product already exists, else set to 1
	    cart.put(productId, cart.getOrDefault(productId, 0) + 1);

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
	        entry.setQuantity(entry.getQuantity() + 1);
	        newQty = entry.getQuantity();
	        System.out.println("Updated existing product quantity to: " + newQty);
	    } else {
	        CartProductEntry newEntry = new CartProductEntry(productId, 1, user.getPhone());
	        productEntries.add(newEntry);
	        newQty = 1;
	        System.out.println("Added new product with quantity: " + newQty);
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

	    User user = (User) session.getAttribute("user");
	    if (user == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login required");
	    }

	    AddToCart cart = addToCartRepository.findByUserId(user.getId())
	            .orElseThrow(() -> new RuntimeException("Cart not found"));

	    List<CartProductEntry> productEntries = cart.getProductQuantities();
	    Optional<CartProductEntry> entryOpt = productEntries.stream()
	            .filter(e -> e.getProductId().equals(productId))
	            .findFirst();

	    if (entryOpt.isPresent()) {
	        CartProductEntry entry = entryOpt.get();
	        int updatedQty = entry.getQuantity() + delta;

	        if (updatedQty <= 0) {
	            productEntries.remove(entry);
	            updatedQty = 0;
	        } else {
	            entry.setQuantity(updatedQty);
	        }

	        addToCartRepository.save(cart);
	        
	        // Update cart count in session
	        int totalItems = cart.getProductQuantities().stream()
	                .mapToInt(CartProductEntry::getQuantity)
	                .sum();
	        session.setAttribute("cartCount", totalItems);
	        
	        return ResponseEntity.ok(Map.of("quantity", updatedQty));
	    }

	    return ResponseEntity.badRequest().body("Product not in cart");
	}
	
	@GetMapping("/cart")
	public String viewCart(Model model, HttpSession session) {
	    User user = (User) session.getAttribute("user");

	    if (user == null) {
	        return "redirect:/login";
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

	    for (CartProductEntry entry : cart.getProductQuantities()) {
	        System.out.println("Processing entry - Product ID: " + entry.getProductId() + ", Quantity: " + entry.getQuantity());
	        
	        // Use a custom query to fetch product with category
	        Product product = productRepository.findByIdWithCategory(entry.getProductId()).orElse(null);

	        if (product != null) {
	            System.out.println("Product found: " + product.getProductName() + ", Price: " + product.getRetailPrice());
	            System.out.println("Product category: " + (product.getCategory() != null ? product.getCategory().getCategoryName() : "NULL"));
	            
	            int quantity = entry.getQuantity();
	            double totalPrice = product.getRetailPrice() * quantity;

	            grandTotal += totalPrice;

	            CartItemDetails cartItem = new CartItemDetails(product.getId(), product, quantity, totalPrice);
	            cartItems.add(cartItem);
	            System.out.println("Cart item created - ID: " + cartItem.getId() + ", Total Price: " + cartItem.getTotalPrice());
	        } else {
	            System.out.println("Product not found for ID: " + entry.getProductId());
	        }
	    }

	    System.out.println("Final cart items count: " + cartItems.size());
	    System.out.println("Final grand total: " + grandTotal);

	    model.addAttribute("cartItems", cartItems);
	    model.addAttribute("grandTotal", grandTotal);
	    
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
