package com.brsons.controller;

import com.brsons.model.Product;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Arrays;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        List<Product> products = Arrays.asList(
        		new Product(1, "T-Shirt", 499.99, "Cotton T-Shirt in various colors", "/images/tshirt.jpg"),
                new Product(2, "Jeans", 899.00, "Slim-fit blue jeans", "/images/jeans.jpg"),
                new Product(3, "Jacket", 1599.00, "Winter warm jacket", "/images/jacket.jpg")
        );
        model.addAttribute("products", products);
        return "index";
    }
}