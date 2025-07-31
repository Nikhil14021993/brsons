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
	   // List<Product> featured = productList.subList(0, Math.min(productList.size(), 3));
	   // model.addAttribute("featured", featured);
	    return "home";
	}
}