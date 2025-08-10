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
	public String viewCategories(Model model) {
	    List<Category> categories = categoryRepository.findByStatus("Active");
	    model.addAttribute("categories", categories);
	    return "shop-categories";
	}
	
	@GetMapping("/shop/category/{id}")
	public String viewProductsByCategory(@PathVariable Long id, Model model) {
	    Category category = categoryRepository.findById(id).orElse(null);
	    if (category != null) {
	        List<Product> products = productRepository.findByCategoryIdAndStatus(id, "active");
	        model.addAttribute("category", category);
	        model.addAttribute("products", products);
	    }
	    return "shop-products";
	}
	@GetMapping("/product/{id}")
	public String viewProduct(@PathVariable Long id, Model model, HttpSession session) {
	    Product product = productRepository.findById(id).orElse(null);
	    if (product == null) {
	        return "redirect:/"; // Or a 404 page
	    }

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
	    if (user != null) {
	        Optional<AddToCart> optionalCart = addToCartRepository.findByUserId(user.getId());
	        if (optionalCart.isPresent()) {
	            AddToCart cart = optionalCart.get();
	            List<CartProductEntry> productQuantities = cart.getProductQuantities();
	            if (productQuantities != null) {
	                for (CartProductEntry entry : productQuantities) {
	                    if (entry.getProductId().equals(product.getId())) {
	                        quantityInCart = entry.getQuantity();
	                        break;
	                    }
	                }
	            }
	        }
	    }

	    model.addAttribute("quantityInCart", quantityInCart);

	    return "product-details";
	}
	
	@PostMapping("/add-to-cart/{productId}")
	@ResponseBody
	public ResponseEntity<?> addToCart(@PathVariable Long productId,
	                                   HttpSession session) {
	    User user = (User) session.getAttribute("user");

	    if (user == null) {
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
	    } else {
	        CartProductEntry newEntry = new CartProductEntry(productId, 1, user.getPhone());
	        productEntries.add(newEntry);
	        newQty = 1;
	    }

	    addToCartRepository.save(cart1);
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

	    for (CartProductEntry entry : cart.getProductQuantities()) {
	        Optional<Product> productOpt = productRepository.findById(entry.getProductId());

	        if (productOpt.isPresent()) {
	            Product product = productOpt.get();
	            int quantity = entry.getQuantity();
	            double totalPrice = product.getPrice() * quantity;

	            grandTotal += totalPrice;

	            cartItems.add(new CartItemDetails(product, quantity, totalPrice));
	        }
	    }

	    model.addAttribute("cartItems", cartItems);
	    model.addAttribute("grandTotal", grandTotal);
	    return "cart";
	}

	

}
