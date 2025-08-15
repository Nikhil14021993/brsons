package com.brsons.controller;

import com.brsons.model.Category;
import com.brsons.model.Product;
import com.brsons.model.User;
import com.brsons.repository.CategoryRepository;
import com.brsons.repository.ProductRepository;
import com.brsons.repository.UserRepository;
import com.brsons.service.DayBookService;
import com.brsons.service.OrderService;

import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AdminController {
	
	@Autowired
    private DayBookService dayBookService;
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
    public String ordersPage(HttpSession session) {
        if (isAdmin(session)) return "admin-orders";
        return "redirect:/";
    }
    
    @GetMapping("/admin/add-product")
    public String addProductForm(HttpSession session, Model model) {
        if (isAdmin(session)) {
            model.addAttribute("product", new Product());
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin-add-product";
        }
        return "redirect:/";
    }

    @PostMapping("/admin/add-product")
    public String saveProduct(
            @RequestParam String productName,
            @RequestParam String colour,
            @RequestParam Double price,
            @RequestParam String size,
            @RequestParam String status,
            @RequestParam String mainPhoto, // "image1", "image2", etc.
            @RequestParam String selectedCategory,
            @RequestParam(required = false) String newCategoryName,
            @RequestParam("imageFile1") MultipartFile imageFile1,
            @RequestParam(value = "imageFile2", required = false) MultipartFile imageFile2,
            @RequestParam(value = "imageFile3", required = false) MultipartFile imageFile3,
            @RequestParam(value = "imageFile4", required = false) MultipartFile imageFile4,
            @RequestParam(value = "imageFile5", required = false) MultipartFile imageFile5
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
        product.setColour(colour);
        product.setPrice(price);
        product.setSize(size);
        product.setStatus(status);

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

        // 3. Set main photo by reading from the product object
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
        // 4. Link category and save
        product.setCategory(category);
        productRepository.save(product);

        return "redirect:/admin?success=Product+Added+Successfully";
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

}
