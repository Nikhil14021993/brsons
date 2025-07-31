package com.brsons.controller;



import com.brsons.model.Product;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class ShopController {

    // Dummy product list (later you can replace with DB)
    private static final List<Product> productList = Arrays.asList(
        new Product(1, "T-Shirt", 499.99, "Cotton T-Shirt in various colors", "/images/tshirt.jpg"),
        new Product(2, "Jeans", 899.00, "Slim-fit blue jeans", "/images/jeans.jpg"),
        new Product(3, "Jacket", 1599.00, "Winter warm jacket", "/images/jacket.jpg")
    );

    @GetMapping("/shop")
    public String viewShop(Model model) {
        model.addAttribute("products", productList);
        return "shop";
    }

    @GetMapping("/product/{id}")
    public String viewProduct(@PathVariable int id, Model model) {
        Product product = productList.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
        model.addAttribute("product", product);
        return "product-details";
    }
}
