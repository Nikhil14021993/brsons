package com.brsons.controller;

import com.brsons.model.Category;
import com.brsons.model.Product;
import com.brsons.repository.CategoryRepository;
import com.brsons.repository.ProductRepository;
import com.brsons.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ShopController {
	private final CategoryRepository categoryRepository ;
	 private final ProductRepository productRepository ;
	 
	 public ShopController(CategoryRepository categoryRepository,ProductRepository productRepository) {
	        this.categoryRepository = categoryRepository;
	        this.productRepository = productRepository;
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
}
