package com.brsons.controller;

import com.brsons.model.AddToCart;
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
import java.util.List;
import java.util.Optional;

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
	public String viewProduct(@PathVariable Long id, Model model) {
	    Product product = productRepository.findById(id).orElse(null);
	    List<String> images = new ArrayList<>();
	    if (product.getImage1() != null) images.add(product.getImage1());
	    if (product.getImage2() != null) images.add(product.getImage2());
	    if (product.getImage3() != null) images.add(product.getImage3());
	    if (product.getImage4() != null) images.add(product.getImage4());
	    if (product.getImage5() != null) images.add(product.getImage5());
	    
	    model.addAttribute("product", product);
	    model.addAttribute("images", images);
	    return "product-details";
	}
	
	
	@PostMapping("/add-to-cart/{productId}")
	public String addToCart(@PathVariable Long productId,
	                        HttpSession session,
	                        RedirectAttributes redirectAttributes) throws Exception {
	    
	    // Get the logged-in user from session
	    User user = (User) session.getAttribute("user");

	    // If user is not logged in, redirect to login page
	    if (user == null) {
	        redirectAttributes.addFlashAttribute("error", "Please login to add to cart");
	        return "redirect:/login";
	    }

	    // Fetch or create the cart
	    AddToCart cart = addToCartRepository.findByUserId(user.getId()).orElse(new AddToCart());
	    cart.setUserId(user.getId());
	    cart.setUserName(user.getName());
	    cart.setUserEmail(user.getEmail());
	    cart.setUserPhone(user.getPhone());

	    // Prepare product list
	    List<Long> products = new ArrayList<>();
	    if (cart.getProductIds() != null) {
	        products = new ArrayList<>(Arrays.asList(cart.getProductIds()));
	    }

	    // Add product only if it's not already in the cart
	    if (!products.contains(productId)) {
	        products.add(productId);
	    }

	    // Update cart and save
	    cart.setProductIds(products.toArray(new Long[0]));
	    addToCartRepository.save(cart);

	    redirectAttributes.addFlashAttribute("success", "Product added to cart!");
	    return "redirect:/shop";
	}

}
